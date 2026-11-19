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

    private static final double MIN_POWER = 0.05;
    private static final double DEFAULT_POWER = 0.5;

    private final Table table;
    private final List<Ball> balls;
    private final Ball cueBall;
    private final Ball eightBall;
    private final ShotRecorder recorder = new ShotRecorder();
    private final PhysicsListenerGroup listeners = new PhysicsListenerGroup();
    private final PhysicsEngine engine;
    private final EightBallMatch match = new EightBallMatch();

    private GamePhase phase = GamePhase.AIMING;
    private double aimAngle;
    private double power = DEFAULT_POWER;
    private boolean placementValid = true;
    private boolean aimLocked;
    private TipContact tipContact = TipContact.CENTER;
    private Optional<TurnOutcome> lastOutcome = Optional.empty();

    public GameSession(Table table) {
        this.table = table;
        this.balls = RackFactory.standardRack(table);
        this.cueBall = balls.get(0);
        this.eightBall = balls.stream().filter(ball -> ball.kind() == BallKind.EIGHT).findFirst().orElseThrow();
        this.listeners.add(recorder);
        this.engine = new PhysicsEngine(table, listeners);
    }

    /** Also tells {@code listener} about cue strikes, collisions, cushion hits and pocketed balls. */
    public void addPhysicsListener(PhysicsListener listener) {
        listeners.add(listener);
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

    /** A copy of the balls and the match that can be played forward without touching this session. */
    public TableSnapshot snapshot() {
        return new TableSnapshot(table, balls, match, phase == GamePhase.PLACING_CUE);
    }

    public AimPrediction aimPrediction() {
        return AimPredictor.predict(table, balls, cueBall, aimAngle);
    }

    public boolean isAimLocked() {
        return aimLocked;
    }

    /** Fixes the current aim so power can be set without disturbing it; calling it again unlocks. */
    public void toggleAimLock() {
        if (phase == GamePhase.AIMING) {
            aimLocked = !aimLocked;
        }
    }

    public void aimToward(Vec2 target) {
        Vec2 offset = target.minus(cueBall.position());
        if (canAim() && offset.length() > 1e-6) {
            aimAngle = offset.angle();
        }
    }

    public void rotateAim(double radians) {
        if (canAim()) {
            aimAngle += radians;
        }
    }

    private boolean canAim() {
        return phase == GamePhase.AIMING && !aimLocked;
    }

    public void setPower(double value) {
        power = Math.max(MIN_POWER, Math.min(1.0, value));
    }

    public void adjustPower(double delta) {
        setPower(power + delta);
    }

    /** Where the cue will hit the cue ball. Back to the centre after every shot. */
    public TipContact tipContact() {
        return tipContact;
    }

    public void setTipContact(TipContact contact) {
        if (phase == GamePhase.AIMING) {
            tipContact = contact;
        }
    }

    public void shoot() {
        if (phase != GamePhase.AIMING) {
            return;
        }
        recorder.reset();
        aimLocked = false;
        lastOutcome = Optional.empty();
        new Shot(aimAngle, power, tipContact).strike(cueBall);
        tipContact = TipContact.CENTER;
        phase = GamePhase.BALLS_MOVING;
        listeners.onCueStruck(power);
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
