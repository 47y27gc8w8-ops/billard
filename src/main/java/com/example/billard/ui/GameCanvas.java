package com.example.billard.ui;

import com.example.billard.model.GamePhase;
import com.example.billard.model.GameSession;
import com.example.billard.model.Vec2;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;

/**
 * The table view. Draws the session each frame and translates mouse and keyboard input into session calls.
 *
 * <p>Controls: mouse or left/right arrows aim (Shift for fine steps), right click locks or unlocks the aim,
 * wheel or up/down arrows set power, click or Space shoots. While placing the cue ball the mouse or arrows move it and click or Enter confirms.
 */
final class GameCanvas extends Pane {

    private static final double AIM_STEP = Math.toRadians(1.2);
    private static final double FINE_AIM_STEP = Math.toRadians(0.15);
    private static final double POWER_STEP = 0.05;
    private static final double WHEEL_POWER_STEP = 0.03;
    private static final double PLACEMENT_STEP = 8;

    private final GameSession session;
    private final Canvas canvas = new Canvas();
    private final TableRenderer tableRenderer;

    GameCanvas(GameSession session) {
        this.session = session;
        this.tableRenderer = new TableRenderer(session.table());
        getChildren().add(canvas);
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        setMinSize(0, 0);
        setPrefSize(0, 0);
        setFocusTraversable(true);
        setAccessibleText("Billiard table. Arrow keys aim and set power, Space shoots.");

        setOnMouseMoved(this::steerWithMouse);
        setOnMouseDragged(this::steerWithMouse);
        setOnMousePressed(this::pressMouse);
        setOnScroll(this::scrollPower);
        setOnKeyPressed(this::pressKey);
    }

    void draw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setTransform(1, 0, 0, 1, 0, 0);
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.save();
        viewport().apply(gc);

        tableRenderer.drawSurface(gc);
        drawGuideIfAiming(gc);
        tableRenderer.drawBalls(gc, session.balls());
        drawCueOrPlacementRing(gc);
        gc.restore();
    }

    private void drawGuideIfAiming(GraphicsContext gc) {
        if (session.phase() == GamePhase.AIMING) {
            CueRenderer.drawGuide(gc, session.cueBall().position(), session.aimPrediction(), session.table().ballRadius());
        }
    }

    private void drawCueOrPlacementRing(GraphicsContext gc) {
        double radius = session.table().ballRadius();
        Vec2 cue = session.cueBall().position();
        switch (session.phase()) {
            case AIMING -> CueRenderer.drawCue(gc, cue, session.aimAngle(), session.power(), radius);
            case PLACING_CUE -> CueRenderer.drawPlacementRing(gc, cue, session.isPlacementValid(), radius);
            default -> { }
        }
    }

    private Viewport viewport() {
        return new Viewport(getWidth(), getHeight(), session.table());
    }

    private void steerWithMouse(MouseEvent event) {
        Vec2 point = viewport().toTable(event.getX(), event.getY());
        switch (session.phase()) {
            case AIMING -> session.aimToward(point);
            case PLACING_CUE -> session.previewCueBall(point);
            default -> { }
        }
    }

    private void pressMouse(MouseEvent event) {
        requestFocus();
        if (event.getButton() == MouseButton.SECONDARY) {
            session.toggleAimLock();
        } else if (event.getButton() == MouseButton.PRIMARY) {
            steerWithMouse(event);
            commitAction();
        }
    }

    private void scrollPower(ScrollEvent event) {
        if (event.getDeltaY() != 0) {
            session.adjustPower(Math.signum(event.getDeltaY()) * WHEEL_POWER_STEP);
        }
    }

    private void pressKey(KeyEvent event) {
        boolean handled = true;
        switch (event.getCode()) {
            case LEFT -> nudge(-1, event.isShiftDown());
            case RIGHT -> nudge(1, event.isShiftDown());
            case UP -> nudgeVertically(1);
            case DOWN -> nudgeVertically(-1);
            case SPACE, ENTER -> commitAction();
            default -> handled = false;
        }
        if (handled) {
            event.consume();
        }
    }

    /** Up and down set power while aiming and move the cue ball while placing it. */
    private void nudgeVertically(int direction) {
        if (session.phase() == GamePhase.PLACING_CUE) {
            session.moveCueBallBy(new Vec2(0, -direction * PLACEMENT_STEP));
        } else {
            session.adjustPower(direction * POWER_STEP);
        }
    }

    private void nudge(int direction, boolean fine) {
        if (session.phase() == GamePhase.PLACING_CUE) {
            session.moveCueBallBy(new Vec2(direction * PLACEMENT_STEP, 0));
        } else {
            session.rotateAim(direction * (fine ? FINE_AIM_STEP : AIM_STEP));
        }
    }

    private void commitAction() {
        switch (session.phase()) {
            case AIMING -> session.shoot();
            case PLACING_CUE -> session.confirmPlacement();
            default -> { }
        }
    }
}
