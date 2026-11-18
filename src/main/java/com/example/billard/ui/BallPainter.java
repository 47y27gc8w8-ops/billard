package com.example.billard.ui;

import com.example.billard.model.BallKind;
import javafx.scene.canvas.GraphicsContext;

/**
 * Draws one ball as a lit sphere: its cast shadow on the cloth, the painted body (solid, stripe band, number disc),
 * then shading and the lamp's reflection over the top.
 */
final class BallPainter {

    private static final double SHADOW_REACH = 1.28;
    private static final double SHADOW_ALPHA = 0.78;
    private static final double CONTACT_ALPHA = 0.55;
    private static final double STRIPE_HALF_HEIGHT = 0.56;
    private static final double NUMBER_DISC = 0.54;
    private static final double NUMBER_SIZE = 0.72;
    private static final double SPECULAR_OFFSET_X = 0.36;
    private static final double SPECULAR_OFFSET_Y = 0.42;
    private static final double SPECULAR_SIZE = 0.22;
    private static final double MIN_RADIUS_FOR_NUMBER = 8;

    private BallPainter() {
    }

    /** A lone ball with its shadow. Where balls touch, paint every shadow first and then {@link #paintSphere}. */
    static void paint(GraphicsContext gc, int number, double cx, double cy, double radius) {
        paintShadow(gc, cx, cy, radius);
        paintSphere(gc, number, cx, cy, radius);
    }

    /** The ball itself, without the shadow it casts. */
    static void paintSphere(GraphicsContext gc, int number, double cx, double cy, double radius) {
        paintBody(gc, number, cx, cy, radius);
        if (number != 0 && radius >= MIN_RADIUS_FOR_NUMBER) {
            paintNumberDisc(gc, number, cx, cy, radius);
        }
        paintShading(gc, cx, cy, radius);
    }

    /** A wide soft shadow thrown away from the lamp, and a tight dark one where the ball sits on the cloth. */
    static void paintShadow(GraphicsContext gc, double cx, double cy, double radius) {
        Lighting.fillSoftShadow(gc,
                cx + radius * Lighting.SHADOW_DX, cy + radius * Lighting.SHADOW_DY,
                radius * SHADOW_REACH, radius * SHADOW_REACH * 0.92, SHADOW_ALPHA);
        Lighting.fillSoftShadow(gc, cx + radius * 0.08, cy + radius * 0.12, radius * 1.04, radius * 1.04, CONTACT_ALPHA);
    }

    /** Shading and reflection only, for a sphere whose body is already painted. */
    static void paintShading(GraphicsContext gc, double cx, double cy, double radius) {
        gc.setFill(Lighting.sphereShade(cx, cy, radius));
        fillCircle(gc, cx, cy, radius);
        gc.setFill(Lighting.specular());
        fillCircle(gc, cx - radius * SPECULAR_OFFSET_X, cy - radius * SPECULAR_OFFSET_Y, radius * SPECULAR_SIZE);
    }

    private static void paintBody(GraphicsContext gc, int number, double cx, double cy, double radius) {
        if (BallKind.fromNumber(number) == BallKind.STRIPE) {
            paintStripedBody(gc, number, cx, cy, radius);
            return;
        }
        gc.setFill(Palette.ballColor(number));
        fillCircle(gc, cx, cy, radius);
    }

    private static void paintStripedBody(GraphicsContext gc, int number, double cx, double cy, double radius) {
        gc.setFill(Palette.CUE_BALL);
        fillCircle(gc, cx, cy, radius);
        gc.save();
        gc.beginPath();
        gc.arc(cx, cy, radius, radius, 0, 360);
        gc.closePath();
        gc.clip();
        gc.setFill(Palette.ballColor(number));
        double bandHeight = radius * STRIPE_HALF_HEIGHT * 2;
        gc.fillRect(cx - radius, cy - bandHeight / 2, radius * 2, bandHeight);
        gc.restore();
    }

    private static void paintNumberDisc(GraphicsContext gc, int number, double cx, double cy, double radius) {
        gc.setFill(Palette.CUE_BALL);
        fillCircle(gc, cx, cy, radius * NUMBER_DISC);
        gc.setFill(Palette.BALL_NUMBER_INK);
        BallNumber.fillCentred(gc, number, radius * NUMBER_SIZE, cx, cy);
    }

    private static void fillCircle(GraphicsContext gc, double cx, double cy, double radius) {
        gc.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
    }
}
