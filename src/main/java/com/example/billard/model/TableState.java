package com.example.billard.model;

import java.util.List;
import java.util.Optional;

/**
 * Everything that decides how a match goes on once the balls have stopped: where each ball lies, whose turn it is,
 * the groups and the last shot's outcome. Two copies of a game that share this state play on identically.
 *
 * @param playerOneGroup Player 1's group once the table is closed; Player 2 has the other one
 * @param balls          all sixteen balls, in number order from the cue ball to the 15
 */
public record TableState(
        GamePhase phase,
        Player currentPlayer,
        boolean breakShot,
        Optional<BallKind> playerOneGroup,
        Optional<TurnOutcome> lastOutcome,
        List<BallState> balls) {

    public static final int BALL_COUNT = 16;

    /** One ball at rest. A pocketed ball's position is meaningless. */
    public record BallState(int number, Vec2 position, boolean pocketed) {
    }

    public TableState {
        if (phase == GamePhase.BALLS_MOVING) {
            throw new IllegalArgumentException("A table state is only taken while the balls are at rest");
        }
        if (playerOneGroup.isPresent() && !playerOneGroup.get().isGroup()) {
            throw new IllegalArgumentException("Player 1's group must be solids or stripes, not " + playerOneGroup);
        }
        balls = List.copyOf(balls);
        requireEveryBallOnce(balls);
    }

    private static void requireEveryBallOnce(List<BallState> balls) {
        if (balls.size() != BALL_COUNT) {
            throw new IllegalArgumentException("Expected " + BALL_COUNT + " balls but got " + balls.size());
        }
        for (int number = 0; number < BALL_COUNT; number++) {
            if (balls.get(number).number() != number) {
                throw new IllegalArgumentException("Ball " + number + " is missing or out of order");
            }
        }
    }
}
