package com.example.billard.ui;

import com.example.billard.model.BotLevel;
import com.example.billard.model.Player;
import java.util.Optional;

/**
 * Who sits at the table. Offline, Player 1 is always a person and breaks; Player 2 is either a second person or a
 * bot of the chosen level. Online, one seat is the person at this computer and the other a player elsewhere, each
 * shown by the name they chose.
 *
 * @param botLevel the bot's level when Player 2 is a bot
 * @param online   the seats and names in an online match
 */
public record MatchMode(Optional<BotLevel> botLevel, Optional<OnlineSeats> online) {

    /** In an online match: which seat is played here, and the names both players chose. */
    public record OnlineSeats(Player localSeat, String localName, String remoteName) {
    }

    public MatchMode {
        if (botLevel.isPresent() && online.isPresent()) {
            throw new IllegalArgumentException("A match is either against the bot or online, not both");
        }
    }

    public static MatchMode twoPlayers() {
        return new MatchMode(Optional.empty(), Optional.empty());
    }

    public static MatchMode versusBot(BotLevel level) {
        return new MatchMode(Optional.of(level), Optional.empty());
    }

    public static MatchMode online(Player localSeat, String localName, String remoteName) {
        return new MatchMode(Optional.empty(), Optional.of(new OnlineSeats(localSeat, localName, remoteName)));
    }

    public boolean isBot(Player player) {
        return botLevel.isPresent() && player == Player.TWO;
    }

    public boolean isRemote(Player player) {
        return online.filter(seats -> seats.localSeat() != player).isPresent();
    }

    /** True for a seat not played by someone at this keyboard: the bot's or the remote player's. */
    public boolean isAway(Player player) {
        return isBot(player) || isRemote(player);
    }

    /** "Player 1" and "Player 2", the bot's level and "bot", such as "Hard bot", or an online player's name. */
    public String nameOf(Player player) {
        if (isBot(player)) {
            return botLevel.get().label() + " bot";
        }
        return online.map(seats -> player == seats.localSeat() ? seats.localName() : seats.remoteName())
                .orElse(player.label());
    }
}
