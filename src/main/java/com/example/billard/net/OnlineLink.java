package com.example.billard.net;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The connection to the other player for a whole online session, rematches included. Greets the other game and
 * checks it runs the same protocol, numbers every message that must arrive and keeps it until the other side
 * confirms it, pings to notice a dead connection, and when the connection drops reconnects and sends again
 * whatever was lost. If the connection cannot be restored within {@link #RECONNECT_WINDOW_SECONDS}, the link closes.
 *
 * <p>Every method must be called on the thread behind {@code events}, normally the JavaFX application thread;
 * the link also delivers everything it receives there, so its state needs no locking.
 *
 * <pre>
 * guest -&gt; host   HELLO version token name     token is - the first time, then the one the host issued
 * host  -&gt; guest  WELCOME version token name
 * host  -&gt; guest  REJECT reason
 * both            RESUME lastReceived           sent after greeting; the other side then repeats what was lost
 * both            R sequence message            a game message that must arrive, numbered from 1
 * both            U message                     a live game message that may be lost
 * both            PING lastReceived             keep-alive, also confirming what has arrived
 * both            BYE                           the player left the match
 * </pre>
 */
public final class OnlineLink implements MatchChannel, AutoCloseable {

    public static final int PROTOCOL_VERSION = 1;
    public static final long RECONNECT_WINDOW_SECONDS = 60;

    private static final Logger LOG = Logger.getLogger(OnlineLink.class.getName());
    private static final long PING_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(1500);
    private static final long SILENCE_LIMIT_NANOS = TimeUnit.SECONDS.toNanos(6);
    private static final long RETRY_DELAY_MILLIS = 1000;
    private static final long TICK_MILLIS = 250;
    private static final int MAX_WAITING_MESSAGES = 1000;
    private static final int TOKEN_BYTES = 16;
    private static final String NO_TOKEN = "-";

    public enum Role { HOST, GUEST }

    public enum State { CONNECTING, CONNECTED, RECONNECTING, CLOSED }

    /** Told whenever the link's state changes; {@code reason} explains a {@link State#CLOSED}, else it is empty. */
    @FunctionalInterface
    public interface StatusListener {
        void onStatus(State state, String reason);
    }

    private record Unconfirmed(long sequence, String body) {
    }

    private final Role role;
    private final Connector connector;
    private final Executor events;
    private final String localName;
    private final ScheduledExecutorService timer;
    private final Deque<Unconfirmed> unconfirmed = new ArrayDeque<>();
    private final Deque<GameMessage> incoming = new ArrayDeque<>();
    private StatusListener listener = (state, reason) -> { };
    private State state = State.CONNECTING;
    private String closeReason = "";
    private String remoteName = "";
    private String sessionToken;
    private LineChannel channel;
    private boolean greeted;
    private boolean everConnected;
    private long nextSequence = 1;
    private long lastReceived;
    private long lastHeardNanos;
    private long lastPingNanos;
    private long reconnectDeadlineNanos;

    private OnlineLink(Role role, Connector connector, String localName, Executor events) {
        if (!PlayerName.isValid(localName)) {
            throw new IllegalArgumentException("Not an allowed player name: " + localName);
        }
        this.role = role;
        this.connector = connector;
        this.localName = localName;
        this.events = events;
        this.timer = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().name("billard-net-timer").factory());
    }

    /** Waits for a guest through {@code connector}. The host plays first in the first match. */
    public static OnlineLink host(Connector connector, String localName, Executor events) {
        return new OnlineLink(Role.HOST, connector, localName, events).start();
    }

    public static OnlineLink join(Connector connector, String localName, Executor events) {
        return new OnlineLink(Role.GUEST, connector, localName, events).start();
    }

    private OnlineLink start() {
        timer.scheduleAtFixedRate(() -> events.execute(this::tick), TICK_MILLIS, TICK_MILLIS, TimeUnit.MILLISECONDS);
        connectInBackground();
        return this;
    }

    /** Replaces the listener; it is told the current state straight away. */
    public void setStatusListener(StatusListener listener) {
        this.listener = listener;
        listener.onStatus(state, closeReason);
    }

    public Role role() {
        return role;
    }

    public State state() {
        return state;
    }

    public String localName() {
        return localName;
    }

    /** The other player's name, once they have been greeted; empty before. */
    public String remoteName() {
        return remoteName;
    }

    @Override
    public boolean isConnected() {
        return state == State.CONNECTED;
    }

    @Override
    public void send(GameMessage message) {
        if (state == State.CLOSED) {
            return;
        }
        String body = GameMessageCodec.encode(message);
        if (message.isLive()) {
            if (isConnected()) {
                channel.send("U " + body);
            }
            return;
        }
        Unconfirmed numbered = new Unconfirmed(nextSequence++, body);
        unconfirmed.addLast(numbered);
        if (isConnected()) {
            sendNumbered(numbered);
        }
    }

    @Override
    public Optional<GameMessage> nextIncoming() {
        return Optional.ofNullable(incoming.peekFirst());
    }

    @Override
    public void consumeIncoming() {
        incoming.pollFirst();
    }

    /** Leaves the session: tells the other player, if they can be reached, and closes everything. Idempotent. */
    @Override
    public void close() {
        if (isConnected()) {
            channel.send("BYE");
        }
        fail("You left the match.");
    }

    /** Whole seconds left to get the other player back while {@link State#RECONNECTING}; otherwise 0. */
    public long reconnectSecondsLeft() {
        if (state != State.RECONNECTING) {
            return 0;
        }
        long left = reconnectDeadlineNanos - System.nanoTime();
        return Math.max(0, TimeUnit.NANOSECONDS.toSeconds(left + TimeUnit.SECONDS.toNanos(1) - 1));
    }

    private void connectInBackground() {
        Thread.ofVirtual().name("billard-net-connect").start(() -> {
            try {
                LineChannel opened = connector.connect();
                events.execute(() -> attach(opened));
            } catch (ConnectionRefusedException refused) {
                events.execute(() -> fail(refused.getMessage()));
            } catch (IOException | ProtocolException failed) {
                events.execute(() -> connectFailed(failed));
            }
        });
    }

    private void connectFailed(Exception failure) {
        if (state == State.CLOSED) {
            return;
        }
        if (!everConnected) {
            fail("Could not connect: " + describe(failure));
            return;
        }
        timer.schedule(() -> events.execute(this::retryConnect), RETRY_DELAY_MILLIS, TimeUnit.MILLISECONDS);
    }

    private void retryConnect() {
        if (state == State.RECONNECTING && channel == null) {
            connectInBackground();
        }
    }

    private void attach(LineChannel opened) {
        if (state == State.CLOSED) {
            opened.close();
            return;
        }
        channel = opened;
        greeted = false;
        lastHeardNanos = System.nanoTime();
        opened.start(line -> events.execute(() -> receive(opened, line)),
                () -> events.execute(() -> drop(opened)));
        if (role == Role.GUEST) {
            opened.send("HELLO " + PROTOCOL_VERSION + " " + (sessionToken == null ? NO_TOKEN : sessionToken)
                    + " " + localName);
        }
    }

    /** Lets go of {@code dropped} and, unless the link is closed, tries to get the other player back. */
    private void drop(LineChannel dropped) {
        if (dropped != channel) {
            return;
        }
        channel.close();
        channel = null;
        greeted = false;
        if (state == State.CLOSED) {
            return;
        }
        if (!everConnected) {
            waitForFirstConnectionAgain();
            return;
        }
        if (state != State.RECONNECTING) {
            reconnectDeadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(RECONNECT_WINDOW_SECONDS);
            changeState(State.RECONNECTING, "");
        }
        connectInBackground();
    }

    /** A host keeps waiting when a caller turns out not to be a proper guest; a guest that got nowhere gives up. */
    private void waitForFirstConnectionAgain() {
        if (role == Role.HOST) {
            connectInBackground();
        } else {
            fail("The host did not answer.");
        }
    }

    private void receive(LineChannel from, String line) {
        if (from != channel) {
            return;
        }
        lastHeardNanos = System.nanoTime();
        try {
            handle(line);
        } catch (ProtocolException e) {
            LOG.log(Level.WARNING, "Dropping the connection after an invalid message: {0}", e.getMessage());
            drop(from);
        }
    }

    private void handle(String line) {
        Fields fields = Fields.of(line);
        String type = fields.nextText();
        if (!greeted) {
            greet(type, fields);
            return;
        }
        switch (type) {
            case "RESUME" -> resume(fields);
            case "PING" -> confirm(readSequence(fields));
            case "R" -> receiveNumbered(fields);
            case "U" -> receiveLive(fields);
            case "BYE" -> fail(remoteName + " left the match.");
            default -> throw new ProtocolException("Unknown message type: " + Fields.abbreviate(type));
        }
    }

    private void greet(String type, Fields fields) {
        switch (role) {
            case HOST -> greetGuest(type, fields);
            case GUEST -> greetHost(type, fields);
        }
    }

    private void greetGuest(String type, Fields fields) {
        if (!type.equals("HELLO")) {
            throw new ProtocolException("Expected HELLO but got " + Fields.abbreviate(type));
        }
        long version = fields.nextLong();
        String token = fields.nextText();
        String name = PlayerName.requireValid(fields.restOfLine());
        if (version != PROTOCOL_VERSION) {
            refuse("This match runs Billard protocol " + PROTOCOL_VERSION + "; please use the same game version.");
            return;
        }
        if (sessionToken != null && !sessionToken.equals(token)) {
            refuse("This match is already being played.");
            return;
        }
        if (sessionToken == null) {
            sessionToken = HexFormat.of().formatHex(randomBytes());
            remoteName = name;
        }
        channel.send("WELCOME " + PROTOCOL_VERSION + " " + sessionToken + " " + localName);
        beginResume();
    }

    private void refuse(String reason) {
        channel.send("REJECT " + reason);
        drop(channel);
    }

    private void greetHost(String type, Fields fields) {
        if (type.equals("REJECT")) {
            fail("The host refused: " + Fields.abbreviate(fields.restOfLine()));
            return;
        }
        if (!type.equals("WELCOME")) {
            throw new ProtocolException("Expected WELCOME but got " + Fields.abbreviate(type));
        }
        long version = fields.nextLong();
        String token = fields.nextText();
        String name = PlayerName.requireValid(fields.restOfLine());
        if (version != PROTOCOL_VERSION) {
            fail("The host runs a different version of Billard.");
            return;
        }
        if (sessionToken != null && !sessionToken.equals(token)) {
            throw new ProtocolException("The host answered for a different match");
        }
        if (sessionToken == null) {
            sessionToken = token;
            remoteName = name;
        }
        beginResume();
    }

    private void beginResume() {
        greeted = true;
        channel.send("RESUME " + lastReceived);
    }

    /** The other side says what it has; repeat the rest, oldest first, and the link is up again. */
    private void resume(Fields fields) {
        confirm(readSequence(fields));
        unconfirmed.forEach(this::sendNumbered);
        everConnected = true;
        changeState(State.CONNECTED, "");
    }

    private long readSequence(Fields fields) {
        long sequence = fields.nextLong();
        fields.requireEnd();
        return sequence;
    }

    private void confirm(long received) {
        if (received < 0 || received >= nextSequence) {
            throw new ProtocolException("Confirmed message " + received + " that was never sent");
        }
        while (!unconfirmed.isEmpty() && unconfirmed.peekFirst().sequence() <= received) {
            unconfirmed.pollFirst();
        }
    }

    private void receiveNumbered(Fields fields) {
        long sequence = fields.nextLong();
        GameMessage message = GameMessageCodec.decode(fields.restOfLine());
        if (sequence <= lastReceived) {
            return;
        }
        if (sequence != lastReceived + 1) {
            throw new ProtocolException("Message " + (lastReceived + 1) + " is missing");
        }
        lastReceived = sequence;
        enqueue(message);
    }

    private void receiveLive(Fields fields) {
        GameMessage message = GameMessageCodec.decode(fields.restOfLine());
        if (!message.isLive()) {
            throw new ProtocolException("A message that must arrive was sent without a number");
        }
        enqueue(message);
    }

    private void enqueue(GameMessage message) {
        if (incoming.size() >= MAX_WAITING_MESSAGES) {
            throw new ProtocolException("Too many messages waiting to be handled");
        }
        incoming.addLast(message);
    }

    private void sendNumbered(Unconfirmed numbered) {
        channel.send("R " + numbered.sequence() + " " + numbered.body());
    }

    private void tick() {
        if (state == State.CLOSED) {
            return;
        }
        long now = System.nanoTime();
        if (channel != null && greeted && now - lastPingNanos >= PING_INTERVAL_NANOS) {
            channel.send("PING " + lastReceived);
            lastPingNanos = now;
        }
        if (channel != null && now - lastHeardNanos > SILENCE_LIMIT_NANOS) {
            drop(channel);
        }
        if (state == State.RECONNECTING && now > reconnectDeadlineNanos) {
            fail("The connection to " + remoteName + " was lost.");
        }
    }

    private void fail(String reason) {
        if (state != State.CLOSED) {
            shutDown(reason);
        }
    }

    private void shutDown(String reason) {
        state = State.CLOSED;
        closeReason = reason;
        connector.close();
        if (channel != null) {
            channel.close();
            channel = null;
        }
        timer.shutdownNow();
        listener.onStatus(State.CLOSED, reason);
    }

    private void changeState(State next, String reason) {
        state = next;
        listener.onStatus(next, reason);
    }

    private static byte[] randomBytes() {
        byte[] bytes = new byte[TOKEN_BYTES];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    private static String describe(Exception failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank() ? failure.getClass().getSimpleName() : message;
    }
}
