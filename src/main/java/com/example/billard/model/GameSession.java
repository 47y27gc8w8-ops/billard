package com.example.billard.model;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * One match from break to the final eight-ball. Owns the balls and drives the turn cycle:
 * aim, shoot, balls roll, rules decide, then aim again or place the cue ball.
 * Free of any UI dependency so the whole game can be tested headless.
 */
public final class GameSession {

    private static final double MAX_SHOT_SPEED = 2400.0;
    private static final double MIN_POWER = 0.05;
    private static final double DEFAULT_POWER = 0.5;

    private final Table table;
    private final List<Ball> balls;
    private final Ball cueBall;
    private final Ball eightBall;
    private final ShotRecorder recorder = new ShotRecorder();
    private final PhysicsEngine engine;
    private final EightBallMatch match = new EightBallMatch();

    private GamePhase phase = GamePhase.AIMING;
    private double aimAngle;
    private double power = DEFAULT_POWER;
    private boolean placementValid = true;
    private Optional<TurnOutcome> lastOutcome = Optional.empty();

    public GameSession(Table table) {
        this.table = table;
        this.balls = RackFactory.standardRack(table);
        this.cueBall = balls.get(0);
        this.eightBall = balls.stream().filter(ball -> ball.kind() == BallKind.EIGHT).findFirst().orElseThrow();
        this.engine = new PhysicsEngine(table, recorder);
    }

    public Table table() {
        return table;
    }

    public List<Ball> balls() {
        return Collections.unmodifiableList(balls);
    }

    public Ball cueBall() {
        return cueBall;
    }

    public GamePhase phase() {
        return phase;
    }

    public EightBallMatch match() {
        return match;
    }

    public Player currentPlayer() {
        return match.currentPlayer();
    }

    public double aimAngle() {
        return aimAngle;
    }

    public double power() {
        return power;
    }

    public boolean isPlacementValid() {
        return placementValid;
    }

    public Optional<TurnOutcome> lastOutcome() {
        return lastOutcome;
    }

    public Optional<Player> winner() {
        return lastOutcome.flatMap(TurnOutcome::winner);
    }

    public AimPrediction aimPrediction() {
        return AimPredictor.predict(table, balls, cueBall, aimAngle);
    }

    public void aimToward(Vec2 target) {
        Vec2 offset = target.minus(cueBall.position());
        if (phase == GamePhase.AIMING && offset.length() > 1e-6) {
            aimAngle = offset.angle();
        }
    }

    public void rotateAim(double radians) {
        if (phase == GamePhase.AIMING) {
            aimAngle += radians;
        }
    }

    public void setPower(double value) {
        power = Math.max(MIN_POWER, Math.min(1.0, value));
    }

    public void adjustPower(double delta) {
        setPower(power + delta);
    }

    public void shoot() {
        if (phase != GamePhase.AIMING) {
            return;
        }
        recorder.reset();
        lastOutcome = Optional.empty();
        cueBall.setVelocity(Vec2.fromAngle(aimAngle).times(power * MAX_SHOT_SPEED));
        phase = GamePhase.BALLS_MOVING;
    }

    /** Moves the simulation forward. Does nothing unless balls are rolling. */
    public void advance(double seconds) {
        if (phase != GamePhase.BALLS_MOVING) {
            return;
        }
        engine.advance(balls, seconds);
        if (balls.stream().noneMatch(Ball::isMoving)) {
            concludeShot();
        }
    }

    public void previewCueBall(Vec2 point) {
        if (phase != GamePhase.PLACING_CUE) {
            return;
        }
        Vec2 spot = table.clampCenter(point);
        cueBall.setPosition(spot);
        placementValid = BallPlacement.isFree(table, balls, cueBall, spot);
    }

    public void moveCueBallBy(Vec2 delta) {
        previewCueBall(cueBall.position().plus(delta));
    }

    public void confirmPlacement() {
        if (phase == GamePhase.PLACING_CUE && placementValid) {
            phase = GamePhase.AIMING;
        }
    }

    private void concludeShot() {
        TurnOutcome outcome = match.resolveShot(recorder.report());
        lastOutcome = Optional.of(outcome);
        if (outcome.eightRespotted()) {
            eightBall.putOnTable(BallPlacement.nearestFree(table, balls, eightBall, table.footSpot()));
        }
        if (outcome.winner().isPresent()) {
            phase = GamePhase.GAME_OVER;
        } else if (outcome.ballInHand()) {
            beginPlacement();
        } else {
            phase = GamePhase.AIMING;
        }
    }

    private void beginPlacement() {
        if (cueBall.isPocketed()) {
            cueBall.putOnTable(BallPlacement.nearestFree(table, balls, cueBall, table.headSpot()));
        }
        placementValid = true;
        phase = GamePhase.PLACING_CUE;
    }
}
