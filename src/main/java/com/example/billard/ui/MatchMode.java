package com.example.billard.ui;

import com.example.billard.model.BotLevel;
import com.example.billard.model.Player;
import java.util.Optional;

/**
 * Who sits on the other side of the table. Player 1 is always a person and always breaks; Player 2 is either a
 * second person or a bot of the chosen level.
 *
 * @param botLevel the bot's level when Player 2 is a bot, empty for two people
 */
public record MatchMode(Optional<BotLevel> botLevel) {

    public static MatchMode twoPlayers() {
        return new MatchMode(Optional.empty());
    }

    public static MatchMode versusBot(BotLevel level) {
        return new MatchMode(Optional.of(level));
    }

    public boolean isBot(Player player) {
        return botLevel.isPresent() && player == Player.TWO;
    }

    /** "Player 1" and "Player 2", or the bot's level and "bot", such as "Hard bot". */
    public String nameOf(Player player) {
        return isBot(player) ? botLevel.get().label() + " bot" : player.label();
    }
}
