package com.example.billard.model;

import java.util.Optional;

/**
 * Where the cue ball would stop if shot along a line: touching {@code target}, or against a cushion.
 *
 * @param ghostCenter the cue ball's centre at the moment of first contact
 * @param target      the ball it would hit, if any
 */
public record AimPrediction(Vec2 ghostCenter, Optional<Ball> target) {

    /** The direction the target ball would start rolling in. */
    public Optional<Vec2> targetDirection() {
        return target.map(ball -> ball.position().minus(ghostCenter).normalized());
    }
}
