package com.example.billard.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class ShotPlannerTest {

    private static final int MAX_STEPS = 20_000;

    private final Table table = Table.standard();
    private final ShotPlanner planner = new ShotPlanner(BotLevel.BOSS, RandomGenerator.of("L64X128MixRandom"));

    /** Plays {@code move} on a copy of the snapshot's balls until they stop, and returns them. */
    private List<Ball> play(TableSnapshot snapshot, BotMove move) {
        List<Ball> balls = snapshot.copyOfBalls();
        Ball cueBall = TableSnapshot.cueBallOf(balls);
        move.cueSpot().ifPresent(cueBall::putOnTable);
        PhysicsEngine engine = new PhysicsEngine(table, new PhysicsListener() { });
        move.shot().strike(cueBall);
        for (int step = 0; step < MAX_STEPS && balls.stream().anyMatch(Ball::isMoving); step++) {
            engine.step(balls);
        }
        return balls;
    }

    private static Ball numbered(List<Ball> balls, int number) {
        return balls.stream().filter(ball -> ball.number() == number).findFirst().orElseThrow();
    }

    private TableSnapshot snapshotOf(boolean ballInHand, Ball... balls) {
        return new TableSnapshot(table, List.of(balls), new EightBallMatch(), ballInHand);
    }

    @Test
    void potsAStraightShotIntoTheCornerWithoutScratching() {
        TableSnapshot snapshot = snapshotOf(false,
                new Ball(0, new Vec2(700, 350)),
                new Ball(3, new Vec2(900, 450)),
                new Ball(8, new Vec2(200, 100)));

        List<Ball> after = play(snapshot, planner.plan(snapshot));

        assertTrue(numbered(after, 3).isPocketed(), "the 3-ball should drop");
        assertFalse(numbered(after, 0).isPocketed(), "the cue ball should stay up");
    }

    @Test
    void leavesAnEasyEightAloneWhileItsGroupIsStillOnTheTable() {
        TableSnapshot snapshot = snapshotOf(false,
                new Ball(0, new Vec2(700, 350)),
                new Ball(8, new Vec2(900, 450)),
                new Ball(3, new Vec2(300, 250)));

        List<Ball> after = play(snapshot, planner.plan(snapshot));

        assertFalse(numbered(after, 8).isPocketed(), "potting the 8-ball now would lose the match");
    }

    @Test
    void withBallInHandPlacesTheCueBallOnAFreeSpotAndPots() {
        TableSnapshot snapshot = snapshotOf(true,
                new Ball(0, new Vec2(40, 460)),
                new Ball(3, new Vec2(600, 130)),
                new Ball(8, new Vec2(850, 400)));

        BotMove move = planner.plan(snapshot);
        List<Ball> after = play(snapshot, move);

        move.cueSpot().ifPresent(spot ->
                assertTrue(BallPlacement.isFree(table, snapshot.balls(), snapshot.cueBall(), spot)));
        assertTrue(numbered(after, 3).isPocketed(), "the 3-ball should drop");
    }

    @Test
    void aSnapshotIsNotChangedByPlanningOnIt() {
        GameSession session = new GameSession(table);
        Vec2 cueBefore = session.cueBall().position();

        planner.plan(session.snapshot());

        assertTrue(session.cueBall().position().equals(cueBefore));
        assertTrue(session.balls().stream().noneMatch(Ball::isMoving));
    }

    @Test
    void aBossShotPlayedThroughTheSessionEndsExactlyAsSimulated() {
        GameSession session = new GameSession(table);
        session.setPower(1.0);
        playThrough(session);
        TableSnapshot snapshot = session.snapshot();
        BotMove move = planner.plan(snapshot);

        List<Ball> simulated = play(snapshot, move);
        move.cueSpot().ifPresent(session::previewCueBall);
        session.confirmPlacement();
        session.setAimAngle(move.shot().angle());
        session.setPower(move.shot().power());
        session.setTipContact(move.shot().tip());
        playThrough(session);

        for (Ball ball : session.balls()) {
            Ball twin = numbered(simulated, ball.number());
            assertEquals(twin.isPocketed(), ball.isPocketed(), "ball " + ball.number() + " pocketed");
            if (!ball.isPocketed()) {
                assertEquals(twin.position(), ball.position(), "ball " + ball.number() + " position");
            }
        }
    }

    private static void playThrough(GameSession session) {
        session.shoot();
        for (int frame = 0; frame < MAX_STEPS && session.phase() == GamePhase.BALLS_MOVING; frame++) {
            session.advance(1.0 / 60.0);
        }
    }

    @Test
    void theBossPlaysItsShotWithoutAnyError() {
        Shot shot = new Shot(0.7, 0.45, TipContact.CENTER);

        assertEquals(shot, BotLevel.BOSS.blur(shot, RandomGenerator.of("L64X128MixRandom")));
    }

    @Test
    void eachLevelAimsMoreSteadilyThanTheOneBelow() {
        BotLevel[] levels = BotLevel.values();
        for (int i = 1; i < levels.length; i++) {
            assertTrue(levels[i].aimError() < levels[i - 1].aimError(), levels[i] + " should aim better");
        }
        assertEquals(0, BotLevel.BOSS.aimError());
    }
}
