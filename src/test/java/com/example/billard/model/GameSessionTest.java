package com.example.billard.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
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
    void lockedAimIgnoresMouseAndKeysButStillAllowsPower() {
        session.aimToward(session.cueBall().position().plus(new Vec2(100, 0)));
        session.toggleAimLock();

        session.aimToward(session.cueBall().position().plus(new Vec2(0, 100)));
        session.rotateAim(1.0);
        session.setPower(0.9);

        assertEquals(0.0, session.aimAngle(), 1e-9);
        assertEquals(0.9, session.power());
    }

    @Test
    void togglingAgainUnlocksTheAim() {
        session.toggleAimLock();
        session.toggleAimLock();

        session.aimToward(session.cueBall().position().plus(new Vec2(0, 100)));

        assertEquals(Math.PI / 2, session.aimAngle(), 1e-9);
    }

    @Test
    void shootingReleasesTheLock() {
        session.toggleAimLock();

        session.shoot();

        assertEquals(GamePhase.BALLS_MOVING, session.phase());
        assertTrue(!session.isAimLocked());
    }

    @Test
    void chosenTipContactBecomesSpinOnTheCueBallAndResetsAfterTheShot() {
        session.setTipContact(new TipContact(0.5, 1.0));

        session.shoot();

        assertTrue(session.cueBall().rollSpin().x() > 0);
        assertTrue(session.cueBall().sideSpin() != 0);
        assertEquals(TipContact.CENTER, session.tipContact());
    }

    @Test
    void addedListenersHearTheCueStrikeAndTheCollisionsThatFollow() {
        List<String> heard = new ArrayList<>();
        session.addPhysicsListener(new PhysicsListener() {
            @Override
            public void onCueStruck(double power) {
                heard.add("cue " + power);
            }

            @Override
            public void onBallCollision(Ball first, Ball second, double impactSpeed) {
                heard.add("collision");
            }
        });
        session.setPower(1.0);

        playShotToCompletion();

        assertEquals("cue 1.0", heard.get(0));
        assertTrue(heard.contains("collision"), "the break should hit the rack");
    }

    @Test
    void tipContactCannotBeChangedWhileBallsRoll() {
        session.shoot();

        session.setTipContact(new TipContact(0.5, 0.5));

        assertEquals(TipContact.CENTER, session.tipContact());
    }

    @Test
    void aimTowardPointsTheCueAtThePoint() {
        session.aimToward(session.cueBall().position().plus(new Vec2(0, 100)));

        assertEquals(Math.PI / 2, session.aimAngle(), 1e-9);
    }

    @Test
    void aCapturedTableRestoresExactlyOnAnotherSession() {
        session.setPower(1.0);
        playShotToCompletion();
        GameSession copy = new GameSession(table);

        copy.restore(session.capture());

        assertEquals(session.capture(), copy.capture());
        assertEquals(session.match().groupOf(Player.ONE), copy.match().groupOf(Player.ONE));
    }

    @Test
    void restoringStopsBallsThatAreStillRolling() {
        TableState start = session.capture();
        session.setPower(1.0);
        session.shoot();
        session.advance(FRAME);

        session.restore(start);

        assertEquals(start, session.capture());
        assertTrue(session.balls().stream().noneMatch(Ball::isMoving));
    }
}
