package com.example.billard.ui;

import com.example.billard.model.Player;

/** Who sits on the other side of the table. Player 1 is always a person and always breaks. */
public enum MatchMode {
    /** Player 1 against the computer, which takes Player 2's turns. */
    VERSUS_BOT,
    /** Two people taking turns at the same mouse and keyboard. */
    TWO_PLAYERS;

    private static final String BOT_NAME = "Bot";

    public boolean isBot(Player player) {
        return this == VERSUS_BOT && player == Player.TWO;
    }

    public String nameOf(Player player) {
        return isBot(player) ? BOT_NAME : player.label();
    }
}
