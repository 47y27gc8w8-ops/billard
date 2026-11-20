package com.example.billard.model;

import java.util.List;

/**
 * A frozen copy of a match in progress: the balls, the rules state, and whether the shooter has ball in hand.
 * Nothing in it is shared with the live session, so it can be played forward on another thread.
 *
 * @param ballInHand true when the shooter places the cue ball before shooting
 */
public record TableSnapshot(Table table, List<Ball> balls, EightBallMatch match, boolean ballInHand) {

    public TableSnapshot {
        balls = copiesOf(balls);
        match = match.copy();
    }

    /** Fresh copies of the balls to play a shot on; the snapshot itself never changes. */
    public List<Ball> copyOfBalls() {
        return copiesOf(balls);
    }

    public Player shooter() {
        return match.currentPlayer();
    }

    public Ball cueBall() {
        return cueBallOf(balls);
    }

    static Ball cueBallOf(List<Ball> balls) {
        return balls.stream().filter(ball -> ball.kind() == BallKind.CUE).findFirst().orElseThrow();
    }

    private static List<Ball> copiesOf(List<Ball> balls) {
        return balls.stream().map(Ball::copy).toList();
    }
}
