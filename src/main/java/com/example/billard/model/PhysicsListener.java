package com.example.billard.model;

/**
 * Receives what happens during a simulation. All callbacks are optional. Speeds are in table units per second and
 * are measured just before the impact, so they say how hard something was hit.
 */
public interface PhysicsListener {

    /** The cue hit the cue ball. {@code power} runs from 0 to 1. */
    default void onCueStruck(double power) {
    }

    /** {@code impactSpeed} is how fast the two balls closed in on each other along the line of centres. */
    default void onBallCollision(Ball first, Ball second, double impactSpeed) {
    }

    /** {@code impactSpeed} is how fast the ball travelled into the cushion. */
    default void onCushionHit(Ball ball, double impactSpeed) {
    }

    /** {@code speed} is how fast the ball was rolling when it dropped. */
    default void onPocketed(Ball ball, double speed) {
    }
}
