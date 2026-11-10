package com.example.billard.ui;

import com.example.billard.model.BallKind;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.geometry.VPos;

/** Draws one ball in flat colour: body, optional stripe band, number disc and a small highlight. */
final class BallPainter {

    private static final double SHADOW_OFFSET = 0.16;
    private static final double STRIPE_HALF_HEIGHT = 0.56;
    private static final double NUMBER_DISC = 0.54;
    private static final double HIGHLIGHT_OFFSET = 0.38;
    private static final double HIGHLIGHT_SIZE = 0.2;
    private static final double MIN_RADIUS_FOR_NUMBER = 8;

    private BallPainter() {
    }

    static void paint(GraphicsContext gc, int number, double cx, double cy, double radius) {
        paintShadow(gc, cx, cy, radius);
        paintBody(gc, number, cx, cy, radius);
        if (number != 0 && radius >= MIN_RADIUS_FOR_NUMBER) {
            paintNumberDisc(gc, number, cx, cy, radius);
        }
        paintHighlight(gc, cx, cy, radius);
    }

    private static void paintShadow(GraphicsContext gc, double cx, double cy, double radius) {
        gc.setFill(Palette.BALL_SHADOW);
        fillCircle(gc, cx + radius * SHADOW_OFFSET, cy + radius * SHADOW_OFFSET * 1.4, radius);
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
        gc.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, radius * 0.74));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(String.valueOf(number), cx, cy + radius * 0.03);
    }

    private static void paintHighlight(GraphicsContext gc, double cx, double cy, double radius) {
        gc.setFill(Palette.BALL_HIGHLIGHT);
        fillCircle(gc, cx - radius * HIGHLIGHT_OFFSET, cy - radius * HIGHLIGHT_OFFSET, radius * HIGHLIGHT_SIZE);
    }

    private static void fillCircle(GraphicsContext gc, double cx, double cy, double radius) {
        gc.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
    }
}
