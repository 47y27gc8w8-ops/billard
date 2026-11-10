package com.example.billard.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GameSessionTest {

    private static final double FRAME = 1.0 / 60.0;
    private static final int MAX_FRAMES = 60 * 90;

    private final Table table = Table.standard();
    private final GameSession session = new GameSession(table);

    private void playShotToCompletion() {
        session.shoot();
        for (int frame = 0; frame < MAX_FRAMES && session.phase() == GamePhase.BALLS_MOVING; frame++) {
            session.advance(FRAME);
        }
    }

    @Test
    void startsWithAllSixteenBallsReadyToBreak() {
        assertEquals(GamePhase.AIMING, session.phase());
        assertEquals(16, session.balls().size());
        assertEquals(Player.ONE, session.currentPlayer());
    }

    @Test
    void shootingStartsTheBallsRolling() {
        session.shoot();

        assertEquals(GamePhase.BALLS_MOVING, session.phase());
        assertTrue(session.cueBall().isMoving());
    }

    @Test
    void aShotAlwaysComesToRestAndHandsControlBack() {
        session.setPower(1.0);

        playShotToCompletion();

        assertNotEquals(GamePhase.BALLS_MOVING, session.phase());
        assertTrue(session.lastOutcome().isPresent());
    }

    @Test
    void ballsNeverLeaveTheTableDuringABreak() {
        session.setPower(1.0);

        playShotToCompletion();

        session.balls().stream().filter(ball -> !ball.isPocketed()).forEach(ball ->
                assertTrue(table.containsCenter(ball.position()), "ball " + ball.number() + " left the table"));
    }

    @Test
    void aimingAndShootingAreIgnoredWhileBallsRoll() {
        session.shoot();
        double angleBefore = session.aimAngle();

        session.rotateAim(1.0);
        session.shoot();

        assertEquals(angleBefore, session.aimAngle());
    }

    @Test
    void powerStaysWithinItsLimits() {
        session.setPower(5);
        assertEquals(1.0, session.power());

        session.setPower(-5);
        assertTrue(session.power() > 0);
    }

    @Test
    void aimTowardPointsTheCueAtThePoint() {
        session.aimToward(session.cueBall().position().plus(new Vec2(0, 100)));

        assertEquals(Math.PI / 2, session.aimAngle(), 1e-9);
    }
}
