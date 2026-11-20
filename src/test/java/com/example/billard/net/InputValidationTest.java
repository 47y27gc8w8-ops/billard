package com.example.billard.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/** What players type into the lobby, and names received from the other game, are checked before use. */
class InputValidationTest {

    @Test
    void aNameIsTrimmedAndItsSpacesClosedUp() {
        assertEquals(Optional.of("Ann Lee"), PlayerName.clean("  Ann    Lee "));
    }

    @Test
    void namesThatAreEmptyTooLongOrCarryControlCharactersAreRefused() {
        assertTrue(PlayerName.clean("   ").isEmpty());
        assertTrue(PlayerName.clean("x".repeat(PlayerName.MAX_LENGTH + 1)).isEmpty());
        assertTrue(PlayerName.clean("Ann\tLee").isEmpty());
        assertTrue(PlayerName.clean("<script>").isEmpty());
    }

    @Test
    void aReceivedNameThatIsNotAllowedBreaksTheProtocol() {
        assertThrows(ProtocolException.class, () -> PlayerName.requireValid("bad\u0000name"));
    }

    @Test
    void addressesAreReadWithOrWithoutAPort() {
        assertEquals(Optional.of(new ServerAddress("192.168.1.20", 47474)), ServerAddress.parse("192.168.1.20", 47474));
        assertEquals(Optional.of(new ServerAddress("example.org", 9000)), ServerAddress.parse(" example.org:9000 ", 1));
        assertEquals(Optional.of(new ServerAddress("::1", 5000)), ServerAddress.parse("[::1]:5000", 1));
    }

    @Test
    void addressesWithABadPortOrStrayCharactersAreRefused() {
        assertTrue(ServerAddress.parse("host:0", 1).isEmpty());
        assertTrue(ServerAddress.parse("host:70000", 1).isEmpty());
        assertTrue(ServerAddress.parse("http://host", 1).isEmpty());
        assertTrue(ServerAddress.parse("", 1).isEmpty());
    }

    @Test
    void roomCodesAreUpperCasedAndOnlyUseTheUnambiguousAlphabet() {
        assertEquals(Optional.of("K7QM"), RelayProtocol.roomCode(" k7qm "));
        assertTrue(RelayProtocol.roomCode("K0QM").isEmpty());
        assertTrue(RelayProtocol.roomCode("K7Q").isEmpty());
    }
}
