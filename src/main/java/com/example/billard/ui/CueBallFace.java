package com.example.billard.ui;

import com.example.billard.model.TipContact;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

/**
 * The cue ball seen from behind the cue, with a mark where the tip will hit. Drawn small on the spin button
 * and large in the spin picker, where it also carries a crosshair and turns clicks into tip positions.
 */
final class CueBallFace extends Canvas {

    private static final double MARGIN = 4;
    private static final double MARK_SIZE = 0.13;
    private static final double MARK_RIM_SIZE = 0.19;
    private static final double MIN_MARK_RADIUS = 3;
    private static final double CROSSHAIR_WIDTH = 2;

    private final boolean detailed;
    private TipContact shown = TipContact.CENTER;

    CueBallFace(double size, boolean detailed) {
        this.detailed = detailed;
        resizeTo(size);
    }

    void resizeTo(double size) {
        setWidth(size);
        setHeight(size);
        redraw();
    }

    void show(TipContact contact) {
        if (!contact.equals(shown)) {
            shown = contact;
            redraw();
        }
    }

    /** The tip position under a mouse point, pulled onto the ball if the point lies outside it. */
    TipContact contactAt(double x, double y) {
        double radius = ballRadius();
        return new TipContact((x - centre()) / radius, (centre() - y) / radius);
    }

    private double centre() {
        return getWidth() / 2;
    }

    private double ballRadius() {
        return centre() - MARGIN;
    }

    private void redraw() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());
        double radius = ballRadius();
        gc.setFill(Palette.CUE_BALL);
        gc.fillOval(centre() - radius, centre() - radius, radius * 2, radius * 2);
        if (detailed) {
            drawCrosshair(gc, radius);
        }
        drawMark(gc, radius);
    }

    private void drawCrosshair(GraphicsContext gc, double radius) {
        gc.setStroke(Palette.BALL_CROSSHAIR);
        gc.setLineWidth(CROSSHAIR_WIDTH);
        gc.strokeLine(centre() - radius, centre(), centre() + radius, centre());
        gc.strokeLine(centre(), centre() - radius, centre(), centre() + radius);
        double half = radius / 2;
        gc.strokeOval(centre() - half, centre() - half, half * 2, half * 2);
    }

    private void drawMark(GraphicsContext gc, double radius) {
        double x = centre() + shown.right() * radius;
        double y = centre() - shown.up() * radius;
        double inner = Math.max(MIN_MARK_RADIUS, radius * MARK_SIZE);
        double rim = Math.max(MIN_MARK_RADIUS + 1.5, radius * MARK_RIM_SIZE);
        gc.setFill(Palette.BALL_NUMBER_INK);
        gc.fillOval(x - rim, y - rim, rim * 2, rim * 2);
        gc.setFill(Palette.ACCENT);
        gc.fillOval(x - inner, y - inner, inner * 2, inner * 2);
    }
}
