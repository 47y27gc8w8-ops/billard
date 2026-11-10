package com.example.billard.ui;

import com.example.billard.model.AimPrediction;
import com.example.billard.model.Vec2;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Draws the cue stick and the aiming guide. All dimensions scale with the ball radius, so the same code
 * serves the game table and the menu artwork.
 */
final class CueRenderer {

    private static final double CUE_LENGTH_IN_RADII = 40;
    private static final double TIP_HALF_WIDTH_IN_RADII = 0.23;
    private static final double BUTT_HALF_WIDTH_IN_RADII = 0.46;
    private static final double TIP_LENGTH_IN_RADII = 1.0;
    private static final double BUTT_START_IN_RADII = 27;
    private static final double REST_GAP_IN_RADII = 0.45;
    private static final double MAX_PULL_BACK_IN_RADII = 8;

    private static final double GUIDE_LINE_WIDTH_IN_RADII = 0.2;
    private static final double GUIDE_DASH_IN_RADII = 0.8;
    private static final double TARGET_LINE_IN_RADII = 6;

    private CueRenderer() {
    }

    /** Draws the cue behind {@code ballCenter}, pointing along {@code angle}, pulled back by {@code power}. */
    static void drawCue(GraphicsContext gc, Vec2 ballCenter, double angle, double power, double radius) {
        Vec2 back = Vec2.fromAngle(angle + Math.PI);
        double gap = radius * (REST_GAP_IN_RADII + power * MAX_PULL_BACK_IN_RADII);
        Vec2 tip = ballCenter.plus(back.times(radius + gap));
        double length = radius * CUE_LENGTH_IN_RADII;
        double tipEnd = radius * TIP_LENGTH_IN_RADII;
        double buttStart = radius * BUTT_START_IN_RADII;

        fillSegment(gc, tip, back, 0, tipEnd, length, radius, Palette.ACCENT);
        fillSegment(gc, tip, back, tipEnd, buttStart, length, radius, Palette.CUE_SHAFT);
        fillSegment(gc, tip, back, buttStart, length, length, radius, Palette.CUE_BUTT);
    }

    /** Draws the dashed path to first contact, the ghost ball and the target's departure line. */
    static void drawGuide(GraphicsContext gc, Vec2 ballCenter, AimPrediction prediction, double radius) {
        Vec2 ghost = prediction.ghostCenter();
        gc.setLineWidth(radius * GUIDE_LINE_WIDTH_IN_RADII);
        gc.setStroke(Palette.GUIDE_LINE);
        gc.setLineDashes(radius * GUIDE_DASH_IN_RADII);
        gc.strokeLine(ballCenter.x(), ballCenter.y(), ghost.x(), ghost.y());
        gc.setLineDashes((double[]) null);

        gc.setStroke(Palette.GUIDE_GHOST);
        strokeCircle(gc, ghost, radius);

        prediction.target().ifPresent(target -> drawDeparture(gc, prediction, target.position(), radius));
    }

    private static void drawDeparture(GraphicsContext gc, AimPrediction prediction, Vec2 targetCenter, double radius) {
        Vec2 direction = prediction.targetDirection().orElseThrow();
        Vec2 end = targetCenter.plus(direction.times(radius * TARGET_LINE_IN_RADII));
        gc.setStroke(Palette.ACCENT);
        gc.setLineWidth(radius * GUIDE_LINE_WIDTH_IN_RADII * 1.6);
        gc.strokeLine(targetCenter.x(), targetCenter.y(), end.x(), end.y());
    }

    /** Ring around a ball that is being placed by hand: accent when the spot is legal, red when it is not. */
    static void drawPlacementRing(GraphicsContext gc, Vec2 center, boolean valid, double radius) {
        gc.setStroke(valid ? Palette.ACCENT : Palette.DANGER);
        gc.setLineWidth(radius * 0.22);
        strokeCircle(gc, center, radius * 1.45);
    }

    private static void strokeCircle(GraphicsContext gc, Vec2 center, double radius) {
        gc.strokeOval(center.x() - radius, center.y() - radius, radius * 2, radius * 2);
    }

    private static void fillSegment(GraphicsContext gc, Vec2 tip, Vec2 back,
                                    double from, double to, double length, double radius, Color color) {
        double fromHalfWidth = halfWidth(from, length, radius);
        double toHalfWidth = halfWidth(to, length, radius);
        Vec2 start = tip.plus(back.times(from));
        Vec2 end = tip.plus(back.times(to));
        Vec2 side = back.perpendicular();
        gc.setFill(color);
        gc.fillPolygon(
                new double[]{
                        start.x() + side.x() * fromHalfWidth, end.x() + side.x() * toHalfWidth,
                        end.x() - side.x() * toHalfWidth, start.x() - side.x() * fromHalfWidth},
                new double[]{
                        start.y() + side.y() * fromHalfWidth, end.y() + side.y() * toHalfWidth,
                        end.y() - side.y() * toHalfWidth, start.y() - side.y() * fromHalfWidth},
                4);
    }

    private static double halfWidth(double distanceFromTip, double length, double radius) {
        double t = distanceFromTip / length;
        double tip = TIP_HALF_WIDTH_IN_RADII * radius;
        double butt = BUTT_HALF_WIDTH_IN_RADII * radius;
        return tip + (butt - tip) * t;
    }
}
