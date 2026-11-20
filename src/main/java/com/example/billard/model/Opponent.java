package com.example.billard.model;

/**
 * Whoever plays the other seat when that is not the person at this keyboard: the bot, or a player on another
 * computer. The match screen moves it on every frame and ignores the local input while it has the table.
 */
public interface Opponent {

    /** Moves the opponent's part of the match on by one frame, after the session itself has been advanced. */
    void update(double elapsedSeconds);

    /** True while the person here has to wait: it is the opponent's turn, or their shot is not yet settled. */
    boolean hasTheTable();

    /** Lets go of anything in progress; the match is being left. */
    void stop();
}
