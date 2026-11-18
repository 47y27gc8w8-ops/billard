package com.example.billard.ui;

import com.example.billard.model.AimPrediction;
import com.example.billard.model.Vec2;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;

/**
 * Draws the cue stick and the aiming guide. All dimensions scale with the ball radius, so the same code
 * serves the game table and the menu artwork.
 */
final class CueRenderer {

    private static final double CUE_LENGTH_IN_RADII = 40;
    private static final double TIP_HALF_WIDTH_IN_RADII = 0.23;
    private static final double BUTT_HALF_WIDTH_IN_RADII = 0.48;
    private static final double REST_GAP_IN_RADII = 0.45;
    private static final double MAX_PULL_BACK_IN_RADII = 8;

    /** Where each part of the cue ends, in radii from the tip, and its colour. */
    private static final Part[] PARTS = {
            new Part(0.35, Palette.ACCENT),
            new Part(1.1, Palette.CUE_FERRULE),
            new Part(22, Palette.CUE_SHAFT),
            new Part(22.6, Palette.CUE_JOINT),
            new Part(30, Palette.CUE_FOREARM),
            new Part(37, Palette.CUE_WRAP),
            new Part(39.5, Palette.CUE_FOREARM),
            new Part(CUE_LENGTH_IN_RADII, Palette.CUE_WRAP),
    };

    /** The cue rises from the tip to the butt, so its shadow drifts further from it toward the butt. */
    private static final double SHADOW_LIFT_AT_TIP = 1.4;
    private static final double SHADOW_LIFT_AT_BUTT = 6;
    private static final double SHADOW_ALPHA = 0.42;
    private static final double SHADOW_BLUR_IN_RADII = 0.7;

    private static final double GUIDE_LINE_WIDTH_IN_RADII = 0.16;
    private static final double GUIDE_DASH_IN_RADII = 0.8;
    private static final double TARGET_LINE_IN_RADII = 6;

    private record Part(double endInRadii, Color color) {
    }

    private CueRenderer() {
    }

    /** Draws the cue behind {@code ballCenter}, pointing along {@code angle}, pulled back by {@code power}. */
    static void drawCue(GraphicsContext gc, Vec2 ballCenter, double angle, double power, double radius) {
        Vec2 back = Vec2.fromAngle(angle + Math.PI);
        double gap = radius * (REST_GAP_IN_RADII + power * MAX_PULL_BACK_IN_RADII);
        Vec2 tip = ballCenter.plus(back.times(radius + gap));

        drawShadow(gc, tip, back, radius);
        Vec2 lit = sideTowardLamp(back);
        double from = 0;
        for (Part part : PARTS) {
            double to = part.endInRadii() * radius;
            fillPart(gc, tip, back, lit, from, to, radius, part.color());
            from = to;
        }
    }

    /** Draws the dashed path to first contact, the ghost ball and the target's departure line. */
    static void drawGuide(GraphicsContext gc, Vec2 ballCenter, AimPrediction prediction, double radius) {
        Vec2 ghost = prediction.ghostCenter();
        gc.setLineWidth(radius * GUIDE_LINE_WIDTH_IN_RADII);
        gc.setStroke(Palette.GUIDE_LINE);
        gc.setLineDashes(radius * GUIDE_DASH_IN_RADII);
        gc.strokeLine(ballCenter.x(), ballCenter.y(), ghost.x(), ghost.y());
        gc.setLineDashes((double[]) null);

        gc.setFill(Lighting.white(0.1));
        gc.fillOval(ghost.x() - radius, ghost.y() - radius, radius * 2, radius * 2);
        gc.setStroke(Palette.GUIDE_GHOST);
        strokeCircle(gc, ghost, radius);

        prediction.target().ifPresent(target -> drawDeparture(gc, prediction, target.position(), radius));
    }

    private static void drawDeparture(GraphicsContext gc, AimPrediction prediction, Vec2 targetCenter, double radius) {
        Vec2 direction = prediction.targetDirection().orElseThrow();
        Vec2 end = targetCenter.plus(direction.times(radius * TARGET_LINE_IN_RADII));
        gc.setStroke(Palette.ACCENT);
        gc.setLineWidth(radius * GUIDE_LINE_WIDTH_IN_RADII * 1.8);
        gc.strokeLine(targetCenter.x(), targetCenter.y(), end.x(), end.y());
    }

    /** Ring around a ball that is being placed by hand: accent when the spot is legal, red when it is not. */
    static void drawPlacementRing(GraphicsContext gc, Vec2 center, boolean valid, double radius) {
        gc.setStroke(valid ? Palette.ACCENT : Palette.DANGER);
        gc.setLineWidth(radius * 0.22);
        strokeCircle(gc, center, radius * 1.45);
    }

    private static void drawShadow(GraphicsContext gc, Vec2 tip, Vec2 back, double radius) {
        double length = radius * CUE_LENGTH_IN_RADII;
        Vec2 shadowTip = tip.plus(lampShadow(radius * SHADOW_LIFT_AT_TIP));
        Vec2 shadowButt = tip.plus(back.times(length)).plus(lampShadow(radius * SHADOW_LIFT_AT_BUTT));
        Vec2 side = back.perpendicular();
        double tipHalf = radius * TIP_HALF_WIDTH_IN_RADII;
        double buttHalf = radius * BUTT_HALF_WIDTH_IN_RADII * 1.3;
        gc.save();
        gc.setEffect(new GaussianBlur(radius * SHADOW_BLUR_IN_RADII * 2));
        gc.setFill(Lighting.black(SHADOW_ALPHA));
        gc.fillPolygon(
                new double[]{shadowTip.x() + side.x() * tipHalf, shadowButt.x() + side.x() * buttHalf,
                        shadowButt.x() - side.x() * buttHalf, shadowTip.x() - side.x() * tipHalf},
                new double[]{shadowTip.y() + side.y() * tipHalf, shadowButt.y() + side.y() * buttHalf,
                        shadowButt.y() - side.y() * buttHalf, shadowTip.y() - side.y() * tipHalf},
                4);
        gc.restore();
    }

    private static Vec2 lampShadow(double height) {
        return new Vec2(Lighting.SHADOW_DX * height, Lighting.SHADOW_DY * height);
    }

    /** The side of the cue that faces the lamp (up and left on screen). */
    private static Vec2 sideTowardLamp(Vec2 back) {
        Vec2 side = back.perpendicular();
        boolean facesLamp = side.x() * -Lighting.SHADOW_DX + side.y() * -Lighting.SHADOW_DY > 0;
        return facesLamp ? side : side.times(-1);
    }

    private static void fillPart(GraphicsContext gc, Vec2 tip, Vec2 back, Vec2 lit,
                                 double from, double to, double radius, Color color) {
        double length = radius * CUE_LENGTH_IN_RADII;
        double fromHalfWidth = halfWidth(from, length, radius);
        double toHalfWidth = halfWidth(to, length, radius);
        Vec2 start = tip.plus(back.times(from));
        Vec2 end = tip.plus(back.times(to));
        Vec2 middle = tip.plus(back.times((from + to) / 2));
        double middleHalfWidth = (fromHalfWidth + toHalfWidth) / 2;
        Vec2 litEdge = middle.plus(lit.times(middleHalfWidth));
        Vec2 shadedEdge = middle.minus(lit.times(middleHalfWidth));
        gc.setFill(Lighting.cylinderShade(color, litEdge.x(), litEdge.y(), shadedEdge.x(), shadedEdge.y()));
        gc.fillPolygon(
                new double[]{
                        start.x() + lit.x() * fromHalfWidth, end.x() + lit.x() * toHalfWidth,
                        end.x() - lit.x() * toHalfWidth, start.x() - lit.x() * fromHalfWidth},
                new double[]{
                        start.y() + lit.y() * fromHalfWidth, end.y() + lit.y() * toHalfWidth,
                        end.y() - lit.y() * toHalfWidth, start.y() - lit.y() * fromHalfWidth},
                4);
    }

    private static void strokeCircle(GraphicsContext gc, Vec2 center, double radius) {
        gc.strokeOval(center.x() - radius, center.y() - radius, radius * 2, radius * 2);
    }

    private static double halfWidth(double distanceFromTip, double length, double radius) {
        double t = distanceFromTip / length;
        double tip = TIP_HALF_WIDTH_IN_RADII * radius;
        double butt = BUTT_HALF_WIDTH_IN_RADII * radius;
        return tip + (butt - tip) * t;
    }
}
