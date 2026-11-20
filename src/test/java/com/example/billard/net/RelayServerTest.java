package com.example.billard.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.billard.net.GameMessage.Chat;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 30, unit = TimeUnit.SECONDS)
class RelayServerTest {

    private static final long WAIT_SECONDS = 15;

    private final RelayServer relay;
    private final ServerAddress address;
    private final CompletableFuture<String> roomCode = new CompletableFuture<>();

    RelayServerTest() throws IOException {
        relay = new RelayServer(0);
        relay.start();
        address = new ServerAddress("127.0.0.1", relay.port());
    }

    @AfterEach
    void stopRelay() {
        relay.close();
    }

    @Test
    void twoPlayersMeetByRoomCodeAndTheirLinesArePassedOn() throws Exception {
        Seats seats = pairUp();

        seats.host.channel.send("hello guest");
        seats.guest.channel.send("hello host");

        assertEquals("hello guest", seats.guest.channel.readLine());
        assertEquals("hello host", seats.host.channel.readLine());
    }

    @Test
    void anUnknownRoomCodeIsRefused() {
        RelayConnector stranger = RelayConnector.joining(address, "ZZZZ");

        assertThrows(ConnectionRefusedException.class, stranger::connect);
    }

    @Test
    void aThirdPlayerCannotTakeAFullRoom() throws Exception {
        pairUp();
        RelayConnector third = RelayConnector.joining(address, roomCode.get());

        assertThrows(ConnectionRefusedException.class, third::connect);
    }

    @Test
    void whenOnePlayerDropsBothRejoinTheirSeatsWithTheirSecrets() throws Exception {
        Seats seats = pairUp();

        seats.guest.channel.close();
        assertThrows(IOException.class, () -> {
            while (true) {
                seats.host.channel.readLine();
            }
        });

        CompletableFuture<LineChannel> hostBack = CompletableFuture.supplyAsync(() -> connect(seats.host.connector));
        LineChannel guestBack = seats.guest.connector.connect();
        LineChannel hostChannel = hostBack.get(WAIT_SECONDS, TimeUnit.SECONDS);

        guestBack.send("back again");
        assertEquals("back again", hostChannel.readLine());
        assertEquals(1, relay.roomCount());
    }

    @Test
    void twoGamesPlayThroughTheRelay() throws Exception {
        ExecutorService events = Executors.newSingleThreadExecutor();
        try {
            OnlineLink host = events.submit(() -> OnlineLink.host(RelayConnector.hosting(address, roomCode::complete),
                    "Host", events)).get();
            String code = roomCode.get(WAIT_SECONDS, TimeUnit.SECONDS);
            OnlineLink guest = events.submit(() -> OnlineLink.join(RelayConnector.joining(address, code), "Guest",
                    events)).get();

            awaitOn(events, () -> host.isConnected() && guest.isConnected());
            events.submit(() -> guest.send(new Chat(QuickChat.WELL_PLAYED))).get();
            awaitOn(events, () -> host.nextIncoming().isPresent());

            assertEquals(Optional.of(new Chat(QuickChat.WELL_PLAYED)), events.submit(host::nextIncoming).get());
            events.submit(() -> {
                host.close();
                guest.close();
            }).get();
        } finally {
            events.shutdownNow();
        }
    }

    private record Seat(RelayConnector connector, LineChannel channel) {
    }

    private record Seats(Seat host, Seat guest) {
    }

    /** Opens a room, joins it, and returns both ends once the relay has paired them. */
    private Seats pairUp() throws Exception {
        RelayConnector hostConnector = RelayConnector.hosting(address, roomCode::complete);
        CompletableFuture<LineChannel> hostChannel = CompletableFuture.supplyAsync(() -> connect(hostConnector));
        RelayConnector guestConnector = RelayConnector.joining(address, roomCode.get(WAIT_SECONDS, TimeUnit.SECONDS));
        LineChannel guestChannel = guestConnector.connect();
        return new Seats(new Seat(hostConnector, hostChannel.get(WAIT_SECONDS, TimeUnit.SECONDS)),
                new Seat(guestConnector, guestChannel));
    }

    private static LineChannel connect(Connector connector) {
        try {
            return connector.connect();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private interface Condition {
        boolean holds();
    }

    private static void awaitOn(ExecutorService events, Condition condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
        while (!events.submit(condition::holds).get()) {
            assertTrue(System.nanoTime() < deadline, "timed out waiting");
            Thread.sleep(10);
        }
    }
}
