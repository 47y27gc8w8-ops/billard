package com.example.billard.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.billard.model.Player;
import org.junit.jupiter.api.Test;

class MatchModeTest {

    @Test
    void againstTheBotOnlyPlayerTwoIsTheBot() {
        assertFalse(MatchMode.VERSUS_BOT.isBot(Player.ONE));
        assertTrue(MatchMode.VERSUS_BOT.isBot(Player.TWO));
        assertEquals("Player 1", MatchMode.VERSUS_BOT.nameOf(Player.ONE));
        assertEquals("Bot", MatchMode.VERSUS_BOT.nameOf(Player.TWO));
    }

    @Test
    void twoPlayersHasNoBot() {
        assertFalse(MatchMode.TWO_PLAYERS.isBot(Player.ONE));
        assertFalse(MatchMode.TWO_PLAYERS.isBot(Player.TWO));
        assertEquals("Player 2", MatchMode.TWO_PLAYERS.nameOf(Player.TWO));
    }
}
