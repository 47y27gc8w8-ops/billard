package com.example.billard.model;

/**
 * Hears the two decisions a shooter commits to during a turn, so they can be passed on, for example to the other
 * computer in an online match. Both callbacks are optional.
 */
public interface TurnListener {

    /** The shooter placed the cue ball on {@code spot} after a foul and is now aiming. */
    default void onCueBallPlaced(Vec2 spot) {
    }

    /** The shooter struck the cue ball with {@code shot}; the balls are now rolling. */
    default void onShot(Shot shot) {
    }
}
