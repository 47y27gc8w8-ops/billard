package com.example.billard.ui;

import com.example.billard.model.GamePhase;
import com.example.billard.model.GameSession;
import com.example.billard.model.Vec2;
import java.util.function.Consumer;
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
 * wheel or up/down arrows set power, click or Space shoots. While placing the cue ball the mouse or arrows move it
 * and click or Enter confirms.
 */
final class GameCanvas extends Pane {

    private static final double AIM_STEP = Math.toRadians(1.2);
    private static final double FINE_AIM_STEP = Math.toRadians(0.15);
    private static final double POWER_STEP = 0.05;
    private static final double WHEEL_POWER_STEP = 0.03;
    private static final double PLACEMENT_STEP = 8;

    private final GameSession session;
    /** The table itself, which never changes during play: painted once, then again only after a resize. */
    private final Canvas surface = new Canvas();
    /** Balls, guide and cue, painted every frame over the surface. */
    private final Canvas canvas = new Canvas();
    private final TableRenderer tableRenderer;
    private final PocketDrops pocketDrops;
    private double surfaceWidth = -1;
    private double surfaceHeight = -1;
    private double surfacePixelsPerUnit = -1;

    GameCanvas(GameSession session) {
        this.session = session;
        this.tableRenderer = new TableRenderer(session.table());
        this.pocketDrops = new PocketDrops(session.table());
        session.addPhysicsListener(pocketDrops);
        getChildren().addAll(surface, canvas);
        for (Canvas layer : new Canvas[]{surface, canvas}) {
            layer.widthProperty().bind(widthProperty());
            layer.heightProperty().bind(heightProperty());
        }
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

    /** @param elapsedSeconds time since the last frame, which moves balls that are dropping into pockets */
    void draw(double elapsedSeconds) {
        Viewport viewport = viewport();
        double pixelsPerUnit = viewport.scale() * Viewport.outputScaleOf(this);
        redrawSurfaceIfResized(viewport, pixelsPerUnit);
        pocketDrops.advance(elapsedSeconds);
        paintLayer(canvas, viewport, gc -> {
            pocketDrops.draw(gc, pixelsPerUnit);
            drawGuideIfAiming(gc);
            tableRenderer.drawBalls(gc, session.balls(), pixelsPerUnit);
            drawCueOrPlacementRing(gc);
        });
    }

    /**
     * Repaints the table layer only when the pane's size or the display scale changed. The table's clipped rails,
     * textures and soft shadows are by far the most expensive part of a frame.
     */
    private void redrawSurfaceIfResized(Viewport viewport, double pixelsPerUnit) {
        double width = getWidth();
        double height = getHeight();
        if (width == surfaceWidth && height == surfaceHeight && pixelsPerUnit == surfacePixelsPerUnit) {
            return;
        }
        surfaceWidth = width;
        surfaceHeight = height;
        surfacePixelsPerUnit = pixelsPerUnit;
        paintLayer(surface, viewport, gc -> tableRenderer.drawSurface(gc, pixelsPerUnit));
    }

    /** Wipes {@code layer} and lets {@code painter} draw on it in table coordinates. */
    private static void paintLayer(Canvas layer, Viewport viewport, Consumer<GraphicsContext> painter) {
        GraphicsContext gc = layer.getGraphicsContext2D();
        gc.setTransform(1, 0, 0, 1, 0, 0);
        gc.clearRect(0, 0, layer.getWidth(), layer.getHeight());
        gc.save();
        viewport.apply(gc);
        painter.accept(gc);
        gc.restore();
    }

    private void drawGuideIfAiming(GraphicsContext gc) {
        if (session.phase() == GamePhase.AIMING) {
            double radius = session.table().ballRadius();
            CueRenderer.drawGuide(gc, session.cueBall().position(), session.aimPrediction(), radius);
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
            case LEFT -> nudgeHorizontally(-1, aimStepFor(event));
            case RIGHT -> nudgeHorizontally(1, aimStepFor(event));
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

    /** Left and right turn the aim by {@code aimStep} while aiming and move the cue ball while placing it. */
    private void nudgeHorizontally(int direction, double aimStep) {
        if (session.phase() == GamePhase.PLACING_CUE) {
            session.moveCueBallBy(new Vec2(direction * PLACEMENT_STEP, 0));
        } else {
            session.rotateAim(direction * aimStep);
        }
    }

    /** Shift turns the aim in fine steps. */
    private static double aimStepFor(KeyEvent event) {
        return event.isShiftDown() ? FINE_AIM_STEP : AIM_STEP;
    }

    private void commitAction() {
        switch (session.phase()) {
            case AIMING -> session.shoot();
            case PLACING_CUE -> session.confirmPlacement();
            default -> { }
        }
    }
}
