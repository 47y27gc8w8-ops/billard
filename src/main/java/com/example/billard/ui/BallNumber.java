package com.example.billard.ui;

import java.util.HashMap;
import java.util.Map;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextBoundsType;

/**
 * The number printed on a ball, centred by its ink. Centring a text's line box puts digits low, since the box
 * leaves room for accents above and descenders below, and a digit's advance can be wider on one side than the other.
 */
final class BallNumber {

    /** Font size the digits are measured at; other sizes scale from it. */
    private static final double REFERENCE_SIZE = 100;
    private static final Map<Integer, Point2D> INK_CENTRES = new HashMap<>();

    private BallNumber() {
    }

    /** Fills {@code number} in the current fill so its digits' visible middle lands on (x, y). */
    static void fillCentred(GraphicsContext gc, int number, double fontSize, double x, double y) {
        Point2D ink = INK_CENTRES.computeIfAbsent(number, BallNumber::measureInkCentre);
        double scale = fontSize / REFERENCE_SIZE;
        gc.setFont(font(fontSize));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.BASELINE);
        gc.fillText(String.valueOf(number), x - ink.getX() * scale, y - ink.getY() * scale);
    }

    private static Font font(double size) {
        return Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, size);
    }

    /** The middle of the digits' visible shape, measured from the start of the baseline. */
    private static Point2D measureInkCentre(int number) {
        Text text = new Text(String.valueOf(number));
        text.setFont(font(REFERENCE_SIZE));
        text.setTextOrigin(VPos.BASELINE);
        text.setBoundsType(TextBoundsType.VISUAL);
        Bounds ink = text.getLayoutBounds();
        return new Point2D(ink.getCenterX(), ink.getCenterY());
    }
}
