package com.example.billard.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * The one light model every painter shares: a lamp above the table and a little toward the player, so highlights
 * sit up and left and shadows fall down and right.
 */
final class Lighting {

    /** Direction shadows fall, per unit of height above the cloth. */
    static final double SHADOW_DX = 0.36;
    static final double SHADOW_DY = 0.52;

    private static final Color CLEAR = Color.TRANSPARENT;

    private Lighting() {
    }

    static Color black(double alpha) {
        return Color.color(0, 0, 0, alpha);
    }

    static Color white(double alpha) {
        return Color.color(1, 1, 1, alpha);
    }

    /** A soft-edged elliptical shadow, darkest in the middle and gone at the rim. */
    static void fillSoftShadow(GraphicsContext gc, double cx, double cy, double radiusX, double radiusY, double alpha) {
        gc.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, black(alpha)), new Stop(0.55, black(alpha * 0.6)), new Stop(1, CLEAR)));
        gc.fillOval(cx - radiusX, cy - radiusY, radiusX * 2, radiusY * 2);
    }

    /** Light and shade over a flat-painted sphere: a broad sheen toward the lamp, darkening to the far rim. */
    static Paint sphereShade(double cx, double cy, double radius) {
        return new RadialGradient(0, 0, cx - radius * 0.35, cy - radius * 0.4, radius * 1.5, false,
                CycleMethod.NO_CYCLE,
                new Stop(0, white(0.38)),
                new Stop(0.2, white(0.08)),
                new Stop(0.45, CLEAR),
                new Stop(0.78, black(0.32)),
                new Stop(1, black(0.62)));
    }

    /** The lamp's reflection: a small, bright spot that fades out quickly. */
    static Paint specular() {
        return new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, white(0.95)), new Stop(0.4, white(0.55)), new Stop(1, CLEAR));
    }

    /**
     * Shading across a round rod (the cue) from one side to the other: dark edges, a highlight a third of the way
     * across, the base colour between.
     */
    static Paint cylinderShade(Color base, double fromX, double fromY, double toX, double toY) {
        return new LinearGradient(fromX, fromY, toX, toY, false, CycleMethod.NO_CYCLE,
                new Stop(0, base.deriveColor(0, 1, 0.45, 1)),
                new Stop(0.3, base.deriveColor(0, 0.85, 1.35, 1)),
                new Stop(0.55, base),
                new Stop(1, base.deriveColor(0, 1, 0.35, 1)));
    }
}
