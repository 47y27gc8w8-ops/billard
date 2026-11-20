package com.example.billard.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.billard.net.GameMessage.Chat;
import com.example.billard.net.OnlineLink.State;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Runs two links against each other over real sockets on this computer. */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class OnlineLinkTest {

    private static final long WAIT_MILLIS = 15_000;

    private final ExecutorService events = Executors.newSingleThreadExecutor();
    private final List<AutoCloseable> toClose = new ArrayList<>();

    @AfterEach
    void closeEverything() throws Exception {
        onEvents(() -> {
            for (AutoCloseable closeable : toClose) {
                closeable.close();
            }
            return null;
        });
        events.shutdownNow();
    }

    @Test
    void bothPlayersLearnEachOthersNames() throws Exception {
        Pair pair = connectedPair();

        assertEquals("Guest", onEvents(pair.host::remoteName));
        assertEquals("Host Player", onEvents(pair.guest::remoteName));
    }

    @Test
    void messagesArriveInTheOrderTheyWereSent() throws Exception {
        Pair pair = connectedPair();

        onEvents(() -> {
            pair.guest.send(new Chat(QuickChat.GOOD_LUCK));
            pair.guest.send(new Chat(QuickChat.OOPS));
            pair.guest.send(new Chat(QuickChat.GOOD_GAME));
            return null;
        });

        assertEquals(List.of(QuickChat.GOOD_LUCK, QuickChat.OOPS, QuickChat.GOOD_GAME), receiveChat(pair.host, 3));
    }

    @Test
    void afterADroppedConnectionTheLinkReconnectsAndDeliversWhatWasSentMeanwhile() throws Exception {
        DirectHost hostConnector = new DirectHost(0);
        RecordingConnector guestConnector = new RecordingConnector(new DirectJoin(local(hostConnector.port())));
        Pair pair = connect(hostConnector, guestConnector);

        guestConnector.lastChannel.close();
        awaitTrue(() -> onEvents(() -> pair.host.state() != State.CONNECTED));
        onEvents(() -> {
            pair.host.send(new Chat(QuickChat.THINKING));
            return null;
        });

        awaitTrue(() -> onEvents(() -> pair.host.isConnected() && pair.guest.isConnected()));
        assertEquals(List.of(QuickChat.THINKING), receiveChat(pair.guest, 1));
        assertEquals(2, guestConnector.connections);
    }

    @Test
    void theHostTurnsAwayACallerOnAnotherVersionAndKeepsWaiting() throws Exception {
        DirectHost hostConnector = new DirectHost(0);
        OnlineLink host = onEvents(() -> OnlineLink.host(hostConnector, "Host Player", events));
        toClose.add(host);

        try (Socket stranger = new Socket("127.0.0.1", hostConnector.port())) {
            LineChannel channel = new LineChannel(stranger);
            channel.send("HELLO 99 - Stranger");
            assertTrue(channel.readLine().startsWith("REJECT "));
        }

        OnlineLink guest = onEvents(() -> OnlineLink.join(new DirectJoin(local(hostConnector.port())), "Guest",
                events));
        toClose.add(guest);
        awaitTrue(() -> onEvents(() -> host.isConnected() && guest.isConnected()));
    }

    @Test
    void whenOnePlayerLeavesTheOtherIsToldWhy() throws Exception {
        Pair pair = connectedPair();
        List<String> reasons = new ArrayList<>();
        onEvents(() -> {
            pair.host.setStatusListener((state, reason) -> reasons.add(state + ": " + reason));
            pair.guest.close();
            return null;
        });

        awaitTrue(() -> onEvents(() -> pair.host.state() == State.CLOSED));
        assertEquals("CLOSED: Guest left the match.", onEvents(() -> reasons.getLast()));
    }

    @Test
    void aGuestThatFindsNoHostGivesUp() throws Exception {
        int freePort;
        try (DirectHost probe = new DirectHost(0)) {
            freePort = probe.port();
        }
        OnlineLink guest = onEvents(() -> OnlineLink.join(new DirectJoin(local(freePort)), "Guest", events));
        toClose.add(guest);

        awaitTrue(() -> onEvents(() -> guest.state() == State.CLOSED));
    }

    private record Pair(OnlineLink host, OnlineLink guest) {
    }

    private Pair connectedPair() throws Exception {
        DirectHost hostConnector = new DirectHost(0);
        return connect(hostConnector, new DirectJoin(local(hostConnector.port())));
    }

    private Pair connect(Connector hostConnector, Connector guestConnector) throws Exception {
        OnlineLink host = onEvents(() -> OnlineLink.host(hostConnector, "Host Player", events));
        OnlineLink guest = onEvents(() -> OnlineLink.join(guestConnector, "Guest", events));
        toClose.add(host);
        toClose.add(guest);
        awaitTrue(() -> onEvents(() -> host.isConnected() && guest.isConnected()));
        return new Pair(host, guest);
    }

    private List<QuickChat> receiveChat(OnlineLink link, int count) throws Exception {
        List<QuickChat> received = new ArrayList<>();
        awaitTrue(() -> onEvents(() -> {
            link.nextIncoming().ifPresent(message -> {
                received.add(((Chat) message).phrase());
                link.consumeIncoming();
            });
            return received.size() >= count;
        }));
        return received;
    }

    private static ServerAddress local(int port) {
        return new ServerAddress("127.0.0.1", port);
    }

    /** Runs {@code task} on the link's event thread, where every link call has to be made. */
    private <T> T onEvents(Callable<T> task) throws Exception {
        return events.submit(task).get(WAIT_MILLIS, TimeUnit.MILLISECONDS);
    }

    private interface ThrowingCondition {
        boolean holds() throws Exception;
    }

    private static void awaitTrue(ThrowingCondition condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(WAIT_MILLIS);
        while (!condition.holds()) {
            assertTrue(System.nanoTime() < deadline, "timed out waiting");
            Thread.sleep(10);
        }
    }

    /** Passes connections through and keeps the latest, so a test can cut it. */
    private static final class RecordingConnector implements Connector {
        private final Connector inner;
        private volatile LineChannel lastChannel;
        private volatile int connections;

        RecordingConnector(Connector inner) {
            this.inner = inner;
        }

        @Override
        public LineChannel connect() throws IOException {
            LineChannel channel = inner.connect();
            lastChannel = channel;
            connections++;
            return channel;
        }

        @Override
        public void close() {
            inner.close();
        }
    }
}
