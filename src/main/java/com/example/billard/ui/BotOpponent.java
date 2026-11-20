package com.example.billard.ui;

import com.example.billard.model.BotMove;
import com.example.billard.model.GamePhase;
import com.example.billard.model.GameSession;
import com.example.billard.model.Player;
import com.example.billard.model.Shot;
import com.example.billard.model.ShotPlanner;
import com.example.billard.model.TableSnapshot;
import com.example.billard.model.Vec2;
import java.util.concurrent.CompletableFuture;

/**
 * Takes one player's turns through the same session calls a person's input makes, paced like a person: a moment
 * to think, the cue ball placed if it has ball in hand, the cue swung round onto the line, a short settle, then
 * the shot. Planning runs on a background thread on a snapshot of the table, so the frame loop never waits for it.
 */
final class BotOpponent {

    private static final double MIN_THINK_SECONDS = 0.7;
    private static final double AIM_SECONDS = 0.9;
    private static final double SETTLE_SECONDS = 0.4;

    private enum Stage { IDLE, THINKING, AIMING, SETTLING }

    private final GameSession session;
    private final Player seat;
    private final ShotPlanner planner;
    private Stage stage = Stage.IDLE;
    private double stageSeconds;
    private CompletableFuture<BotMove> pendingPlan;
    private Shot shot;
    private double startAngle;

    BotOpponent(GameSession session, Player seat, ShotPlanner planner) {
        this.session = session;
        this.seat = seat;
        this.planner = planner;
    }

    /** Moves the bot's turn on by one frame. Does nothing while it is not the bot's turn to act. */
    void update(double elapsedSeconds) {
        if (!isToAct()) {
            stop();
            return;
        }
        stageSeconds += elapsedSeconds;
        switch (stage) {
            case IDLE -> startThinking();
            case THINKING -> playPlanWhenReady();
            case AIMING -> swingCue();
            case SETTLING -> shootWhenSettled();
        }
    }

    /** Drops any plan in progress; the bot starts afresh on its next turn. */
    void stop() {
        if (pendingPlan != null) {
            pendingPlan.cancel(false);
            pendingPlan = null;
        }
        stage = Stage.IDLE;
    }

    private boolean isToAct() {
        GamePhase phase = session.phase();
        return session.currentPlayer() == seat && (phase == GamePhase.AIMING || phase == GamePhase.PLACING_CUE);
    }

    private void startThinking() {
        TableSnapshot snapshot = session.snapshot();
        pendingPlan = CompletableFuture.supplyAsync(() -> planner.plan(snapshot));
        enter(Stage.THINKING);
    }

    private void playPlanWhenReady() {
        if (!pendingPlan.isDone() || stageSeconds < MIN_THINK_SECONDS) {
            return;
        }
        BotMove move = pendingPlan.join();
        pendingPlan = null;
        move.cueSpot().ifPresent(this::placeCueBall);
        session.confirmPlacement();
        shot = move.shot();
        session.setPower(shot.power());
        session.setTipContact(shot.tip());
        startAngle = session.aimAngle();
        enter(Stage.AIMING);
    }

    /** Puts the cue ball on {@code spot}, or leaves it where it was if the spot turns out not to be free. */
    private void placeCueBall(Vec2 spot) {
        Vec2 original = session.cueBall().position();
        session.previewCueBall(spot);
        if (!session.isPlacementValid()) {
            session.previewCueBall(original);
        }
    }

    /**
     * Turns the cue from where it pointed onto the shot line, easing in and out, the short way round. The last frame
     * sets the planned angle exactly, so the shot played is the shot the planner simulated.
     */
    private void swingCue() {
        double progress = Math.min(1, stageSeconds / AIM_SECONDS);
        if (progress >= 1) {
            session.setAimAngle(shot.angle());
            enter(Stage.SETTLING);
            return;
        }
        double eased = progress * progress * (3 - 2 * progress);
        double turn = Math.IEEEremainder(shot.angle() - startAngle, 2 * Math.PI);
        session.setAimAngle(startAngle + turn * eased);
    }

    private void shootWhenSettled() {
        if (stageSeconds >= SETTLE_SECONDS) {
            session.shoot();
            stage = Stage.IDLE;
        }
    }

    private void enter(Stage next) {
        stage = next;
        stageSeconds = 0;
    }
}
