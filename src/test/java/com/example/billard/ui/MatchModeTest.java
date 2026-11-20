package com.example.billard.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.billard.model.BotLevel;
import com.example.billard.model.Player;
import org.junit.jupiter.api.Test;

class MatchModeTest {

    @Test
    void againstABotOnlyPlayerTwoIsTheBotAndItIsNamedByLevel() {
        MatchMode mode = MatchMode.versusBot(BotLevel.HARD);

        assertFalse(mode.isBot(Player.ONE));
        assertTrue(mode.isBot(Player.TWO));
        assertEquals("Player 1", mode.nameOf(Player.ONE));
        assertEquals("Hard bot", mode.nameOf(Player.TWO));
    }

    @Test
    void twoPlayersHasNoBot() {
        MatchMode mode = MatchMode.twoPlayers();

        assertFalse(mode.isBot(Player.ONE));
        assertFalse(mode.isBot(Player.TWO));
        assertEquals("Player 2", mode.nameOf(Player.TWO));
    }
}
