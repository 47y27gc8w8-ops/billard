package com.example.billard.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Meets the other player through a relay server, for players whose routers do not let a direct connection in. The
 * host opens a room and shares its code; the guest joins with it. After a drop, both come back with the secret the
 * relay gave them, so nobody else can take their seat.
 */
public final class RelayConnector implements Connector {

    private static final int CONNECT_TIMEOUT_MILLIS = 5000;

    private final ServerAddress relay;
    private final Consumer<String> onRoomOpened;
    private String roomCode;
    private String secret;
    private volatile Socket attempt;
    private volatile boolean closed;

    private RelayConnector(ServerAddress relay, String roomCode, Consumer<String> onRoomOpened) {
        this.relay = relay;
        this.roomCode = roomCode;
        this.onRoomOpened = onRoomOpened;
    }

    /** Opens a new room; {@code onRoomOpened} receives its code, from the connecting thread, once it exists. */
    public static RelayConnector hosting(ServerAddress relay, Consumer<String> onRoomOpened) {
        return new RelayConnector(relay, null, onRoomOpened);
    }

    public static RelayConnector joining(ServerAddress relay, String roomCode) {
        if (!RelayProtocol.isRoomCode(roomCode)) {
            throw new IllegalArgumentException("Not a room code: " + roomCode);
        }
        return new RelayConnector(relay, roomCode, code -> { });
    }

    @Override
    public LineChannel connect() throws IOException {
        LineChannel channel = open();
        try {
            if (secret != null) {
                rejoin(channel);
            } else if (roomCode == null) {
                openRoom(channel);
            } else {
                joinRoom(channel);
            }
            expect(channel.readLine(), RelayProtocol.PAIRED);
            return channel;
        } catch (IOException | RuntimeException e) {
            channel.close();
            throw e;
        }
    }

    private LineChannel open() throws IOException {
        Socket socket = new Socket();
        attempt = socket;
        if (closed) {
            socket.close();
            throw new IOException("Connecting was cancelled");
        }
        socket.connect(new InetSocketAddress(relay.host(), relay.port()), CONNECT_TIMEOUT_MILLIS);
        socket.setTcpNoDelay(true);
        return new LineChannel(socket);
    }

    private void openRoom(LineChannel channel) throws IOException {
        channel.send(RelayProtocol.HOST);
        Fields reply = expect(channel.readLine(), RelayProtocol.ROOM);
        String code = reply.nextText();
        String hostSecret = reply.nextText();
        reply.requireEnd();
        if (!RelayProtocol.isRoomCode(code) || !RelayProtocol.isSecret(hostSecret)) {
            throw new ProtocolException("The relay sent an invalid room");
        }
        roomCode = code;
        secret = hostSecret;
        onRoomOpened.accept(code);
    }

    private void joinRoom(LineChannel channel) throws IOException {
        channel.send(RelayProtocol.JOIN + " " + roomCode);
        Fields reply = expect(channel.readLine(), RelayProtocol.JOINED);
        String guestSecret = reply.nextText();
        reply.requireEnd();
        if (!RelayProtocol.isSecret(guestSecret)) {
            throw new ProtocolException("The relay sent an invalid secret");
        }
        secret = guestSecret;
    }

    private void rejoin(LineChannel channel) throws IOException {
        channel.send(RelayProtocol.REJOIN + " " + roomCode + " " + secret);
        expect(channel.readLine(), RelayProtocol.REJOINED).requireEnd();
    }

    /** The fields after {@code keyword}, or a refusal if the relay answered with an error instead. */
    private static Fields expect(String line, String keyword) throws ConnectionRefusedException {
        Fields fields = Fields.of(line);
        String first = fields.nextText();
        if (first.equals(RelayProtocol.ERROR)) {
            throw new ConnectionRefusedException("Relay: " + Fields.abbreviate(fields.restOfLine()));
        }
        if (!first.equals(keyword)) {
            throw new ProtocolException("Expected " + keyword + " from the relay");
        }
        return fields;
    }

    @Override
    public void close() {
        closed = true;
        Socket socket = attempt;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // The attempt is abandoned either way.
            }
        }
    }
}
