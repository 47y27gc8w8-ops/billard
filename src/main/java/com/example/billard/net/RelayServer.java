package com.example.billard.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A small server that lets two players meet by room code when neither can accept a direct connection. It speaks
 * {@link RelayProtocol}: once both seats of a room are taken it passes every line from one player to the other,
 * without reading it. The games check each other themselves, so the relay never sees a player name or a shot.
 *
 * <p>When either player's connection ends, the relay closes the other one too and both come back with their
 * secrets; a seat is never handed to anyone else. Rooms that stay empty for {@link #EMPTY_ROOM_SECONDS} are removed.
 *
 * <p>Run it without JavaFX: {@code java -cp target/classes com.example.billard.net.RelayServer [port]}.
 */
public final class RelayServer implements AutoCloseable {

    public static final int MAX_ROOMS = 500;
    public static final long EMPTY_ROOM_SECONDS = 120;

    private static final Logger LOG = Logger.getLogger(RelayServer.class.getName());
    private static final int HANDSHAKE_TIMEOUT_MILLIS = 10_000;
    private static final int SECRET_BYTES = 16;
    private static final long SWEEP_SECONDS = 15;

    private enum Seat { HOST, GUEST }

    /** One match's meeting point. Guarded by the server's lock, like everything else that changes. */
    private static final class Room {
        final String code;
        final String hostSecret;
        String guestSecret;
        LineChannel host;
        LineChannel guest;
        boolean paired;
        long emptySinceNanos;

        Room(String code, String hostSecret) {
            this.code = code;
            this.hostSecret = hostSecret;
        }

        LineChannel at(Seat seat) {
            return seat == Seat.HOST ? host : guest;
        }

        void seat(Seat seat, LineChannel channel) {
            if (seat == Seat.HOST) {
                host = channel;
            } else {
                guest = channel;
            }
        }

        boolean isEmpty() {
            return host == null && guest == null;
        }
    }

    private final ServerSocket server;
    private final Map<String, Room> rooms = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final ScheduledExecutorService sweeper =
            Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().name("billard-relay-sweeper").factory());

    /** @param port the port to listen on, or 0 for any free one */
    public RelayServer(int port) throws IOException {
        this.server = new ServerSocket(port);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : RelayProtocol.DEFAULT_PORT;
        RelayServer relay = new RelayServer(port);
        relay.start();
        LOG.log(Level.INFO, "Billard relay listening on port {0}", relay.port());
        Thread.currentThread().join();
    }

    public int port() {
        return server.getLocalPort();
    }

    /** Starts taking connections on a thread of its own. */
    public void start() {
        sweeper.scheduleAtFixedRate(this::removeAbandonedRooms, SWEEP_SECONDS, SWEEP_SECONDS, TimeUnit.SECONDS);
        Thread.ofVirtual().name("billard-relay-accept").start(this::acceptForever);
    }

    @Override
    public void close() {
        sweeper.shutdownNow();
        try {
            server.close();
        } catch (IOException ignored) {
            // Closing is best effort; the port is released either way.
        }
        synchronized (this) {
            rooms.values().forEach(RelayServer::closeSeats);
            rooms.clear();
        }
    }

    synchronized int roomCount() {
        return rooms.size();
    }

    private void acceptForever() {
        while (!server.isClosed()) {
            try {
                Socket socket = server.accept();
                Thread.ofVirtual().name("billard-relay-greet").start(() -> greet(socket));
            } catch (IOException e) {
                if (!server.isClosed()) {
                    LOG.log(Level.WARNING, "Could not accept a connection: {0}", e.getMessage());
                }
            }
        }
    }

    /** Reads the newcomer's request, with a time limit so a silent connection cannot hold a thread for ever. */
    private void greet(Socket socket) {
        LineChannel channel;
        try {
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(HANDSHAKE_TIMEOUT_MILLIS);
            channel = new LineChannel(socket);
        } catch (IOException e) {
            closeQuietly(socket);
            return;
        }
        try {
            String request = channel.readLine();
            channel.setReadTimeout(0);
            handle(channel, Fields.of(request));
        } catch (IOException | ProtocolException e) {
            channel.close();
        }
    }

    private void handle(LineChannel channel, Fields request) {
        String type = request.nextText();
        switch (type) {
            case RelayProtocol.HOST -> {
                request.requireEnd();
                openRoom(channel);
            }
            case RelayProtocol.JOIN -> {
                String code = request.nextText();
                request.requireEnd();
                joinRoom(channel, code);
            }
            case RelayProtocol.REJOIN -> {
                String code = request.nextText();
                String secret = request.nextText();
                request.requireEnd();
                rejoinRoom(channel, code, secret);
            }
            default -> refuse(channel, "Unknown request");
        }
    }

    private synchronized void openRoom(LineChannel channel) {
        if (rooms.size() >= MAX_ROOMS) {
            refuse(channel, "The relay is full, please try again later");
            return;
        }
        String code = unusedCode();
        Room room = new Room(code, newSecret());
        rooms.put(code, room);
        channel.send(RelayProtocol.ROOM + " " + code + " " + room.hostSecret);
        takeSeat(room, Seat.HOST, channel);
    }

    private synchronized void joinRoom(LineChannel channel, String code) {
        Room room = rooms.get(code);
        if (room == null || room.guestSecret != null) {
            refuse(channel, room == null ? "No room with that code" : "That room is already full");
            return;
        }
        room.guestSecret = newSecret();
        channel.send(RelayProtocol.JOINED + " " + room.guestSecret);
        takeSeat(room, Seat.GUEST, channel);
    }

    private synchronized void rejoinRoom(LineChannel channel, String code, String secret) {
        Room room = rooms.get(code);
        Seat seat = room == null ? null : seatWithSecret(room, secret);
        if (seat == null) {
            refuse(channel, "That seat is not yours");
            return;
        }
        if (room.paired) {
            unpair(room);
        }
        LineChannel stale = room.at(seat);
        if (stale != null) {
            stale.close();
        }
        channel.send(RelayProtocol.REJOINED);
        takeSeat(room, seat, channel);
    }

    /** The seat {@code secret} belongs to, compared in constant time so it cannot be guessed piece by piece. */
    private static Seat seatWithSecret(Room room, String secret) {
        if (matches(room.hostSecret, secret)) {
            return Seat.HOST;
        }
        if (room.guestSecret != null && matches(room.guestSecret, secret)) {
            return Seat.GUEST;
        }
        return null;
    }

    private static boolean matches(String expected, String given) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                given.getBytes(StandardCharsets.US_ASCII));
    }

    /** Seats {@code channel} and starts listening to it; pairs the room once both players are there. */
    private void takeSeat(Room room, Seat seat, LineChannel channel) {
        room.seat(seat, channel);
        channel.start(line -> forward(room, channel, line), () -> leave(room, channel));
        if (room.host != null && room.guest != null) {
            room.paired = true;
            room.host.send(RelayProtocol.PAIRED);
            room.guest.send(RelayProtocol.PAIRED);
        }
    }

    /**
     * Passes a line on to the other seat. Talking before both players are there breaks the protocol. The line is
     * written outside the lock, so one slow player cannot hold up the other rooms.
     */
    private void forward(Room room, LineChannel from, String line) {
        LineChannel to = partnerOf(room, from);
        if (to == null) {
            from.close();
        } else {
            to.send(line);
        }
    }

    private synchronized LineChannel partnerOf(Room room, LineChannel channel) {
        if (!room.paired || (channel != room.host && channel != room.guest)) {
            return null;
        }
        return channel == room.host ? room.guest : room.host;
    }

    private synchronized void leave(Room room, LineChannel channel) {
        if (room.paired && (channel == room.host || channel == room.guest)) {
            unpair(room);
        } else if (channel == room.host) {
            room.host = null;
        } else if (channel == room.guest) {
            room.guest = null;
        }
        if (room.isEmpty()) {
            room.emptySinceNanos = System.nanoTime();
        }
    }

    /** Ends the pairing: both players drop and rejoin, so neither keeps talking into a half-open connection. */
    private static void unpair(Room room) {
        room.paired = false;
        closeSeats(room);
        room.emptySinceNanos = System.nanoTime();
    }

    private static void closeSeats(Room room) {
        for (Seat seat : Seat.values()) {
            LineChannel channel = room.at(seat);
            room.seat(seat, null);
            if (channel != null) {
                channel.close();
            }
        }
    }

    private synchronized void removeAbandonedRooms() {
        long now = System.nanoTime();
        long limit = TimeUnit.SECONDS.toNanos(EMPTY_ROOM_SECONDS);
        rooms.values().removeIf(room -> room.isEmpty() && now - room.emptySinceNanos > limit);
    }

    private static void refuse(LineChannel channel, String reason) {
        channel.send(RelayProtocol.ERROR + " " + reason);
        channel.close();
    }

    private String unusedCode() {
        String code;
        do {
            StringBuilder text = new StringBuilder(RelayProtocol.CODE_LENGTH);
            for (int i = 0; i < RelayProtocol.CODE_LENGTH; i++) {
                text.append(RelayProtocol.CODE_ALPHABET.charAt(random.nextInt(RelayProtocol.CODE_ALPHABET.length())));
            }
            code = text.toString();
        } while (rooms.containsKey(code));
        return code;
    }

    private String newSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
            // The connection is abandoned either way.
        }
    }
}
