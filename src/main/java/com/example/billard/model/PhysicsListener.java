package com.example.billard.model;

/** Receives what happens during a simulation. All callbacks are optional. */
public interface PhysicsListener {

    default void onBallCollision(Ball first, Ball second) {
    }

    default void onCushionHit(Ball ball) {
    }

    default void onPocketed(Ball ball) {
    }
}
