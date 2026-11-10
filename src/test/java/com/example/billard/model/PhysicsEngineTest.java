package com.example.billard.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PhysicsEngineTest {

    private final Table table = Table.standard();
    private final List<Ball> pocketedBalls = new ArrayList<>();
    private final PhysicsEngine engine = new PhysicsEngine(table, new PhysicsListener() {
        @Override
        public void onPocketed(Ball ball) {
            pocketedBalls.add(ball);
        }
    });

    @Test
    void rollingBallComesToRestOnTheTable() {
        Ball ball = new Ball(0, new Vec2(300, 250));
        ball.setVelocity(new Vec2(300, 0));

        engine.advance(List.of(ball), 0.05);
        for (int i = 0; i < 200; i++) {
            engine.advance(List.of(ball), 0.05);
        }

        assertFalse(ball.isMoving());
        assertTrue(ball.position().x() > 300);
    }

    @Test
    void headOnHitPassesMomentumToTheStationaryBall() {
        Ball cue = new Ball(0, new Vec2(300, 250));
        Ball target = new Ball(1, new Vec2(500, 250));
        cue.setVelocity(new Vec2(1000, 0));

        engine.advance(List.of(cue, target), 0.25);

        assertTrue(target.velocity().x() > 600, "target should be rolling away fast");
        assertTrue(cue.velocity().length() < 50, "cue ball should be nearly stopped");
    }

    @Test
    void ballBouncesOffTheEndCushionAndLosesSpeed() {
        Ball ball = new Ball(1, new Vec2(900, 250));
        ball.setVelocity(new Vec2(600, 0));

        engine.advance(List.of(ball), 0.3);

        assertTrue(ball.position().x() <= table.width() - table.ballRadius());
        assertTrue(ball.velocity().x() < 0, "ball should travel back");
        assertTrue(Math.abs(ball.velocity().x()) < 600);
    }

    @Test
    void ballRollingIntoACornerIsPocketed() {
        Ball ball = new Ball(3, new Vec2(80, 80));
        ball.setVelocity(new Vec2(-300, -300));

        engine.advance(List.of(ball), 0.3);

        assertTrue(ball.isPocketed());
        assertEquals(List.of(ball), pocketedBalls);
        assertFalse(ball.isMoving());
    }

    @Test
    void pocketedBallIsIgnoredByTheSimulation() {
        Ball gone = new Ball(3, new Vec2(80, 80));
        gone.pocket();
        Ball cue = new Ball(0, new Vec2(80, 80));
        cue.setVelocity(new Vec2(100, 0));

        engine.advance(List.of(gone, cue), 0.1);

        assertTrue(cue.position().x() > 80);
    }
}
