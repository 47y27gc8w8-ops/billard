package com.example.billard.ui;

import com.example.billard.model.Ball;
import com.example.billard.model.BallKind;
import com.example.billard.model.Orientation;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Paints balls as turned spheres, so the stripe and number discs travel round as a ball rolls and disappear over
 * its edge. The painted surface is traced per device pixel: each pixel's point on the visible half of the sphere
 * is turned back into the ball's own frame and coloured by what lies there. The number is drawn as text mapped onto
 * its disc, then {@link BallPainter#paintShading} adds the lamp's light, which stays put while the ball turns.
 *
 * <p>Ball frame: the number discs sit on the +z and -z axis, and a stripe ball's band runs round the y axis,
 * through both discs, as on a real ball.
 */
final class RollingBallPainter {

    /**
     * Cosine of the number disc's angular radius. The disc's sine is {@link BallPainter#NUMBER_DISC}, so it spans that
     * share of the radius when it faces up, the same as on a ball painted flat.
     */
    private static final double NUMBER_DISC_COS = Math.sqrt(1 - BallPainter.NUMBER_DISC * BallPainter.NUMBER_DISC);
    /** The number fades out as its disc turns edge-on, between these facing values. */
    private static final double NUMBER_HIDDEN_FACING = 0.12;
    private static final double NUMBER_SHOWN_FACING = 0.4;
    /**
     * Above this facing the disc's centre sits within half a radius of the ball's centre, and the number spans
     * under half a radius from there, so it stays inside the ball without a clip.
     */
    private static final double NUMBER_CLIP_FACING = 0.88;
    private static final int SAMPLES = 3;

    private final Map<Integer, TracedSurface> surfaces = new HashMap<>();
    private int[] pixels = new int[0];

    /** A ball's traced image and the turn it shows, so a ball at rest is not traced again every frame. */
    private record TracedSurface(WritableImage image, Orientation orientation) {
    }

    /** The ball with shading, but without its shadow. */
    void paint(GraphicsContext gc, Ball ball, double radius, double pixelsPerUnit) {
        paint(gc, ball.number(), ball.orientation(), ball.position().x(), ball.position().y(), radius, pixelsPerUnit);
    }

    /** A ball given by its number and turn rather than as a {@link Ball}, for one that is no longer on the table. */
    void paint(GraphicsContext gc, int number, Orientation orientation, double cx, double cy, double radius,
               double pixelsPerUnit) {
        if (number == 0) {
            BallPainter.paintSphere(gc, 0, cx, cy, radius);
            return;
        }
        double[] turn = orientation.matrix();
        int size = Math.max(4, (int) Math.ceil(radius * 2 * pixelsPerUnit));
        WritableImage surface = surfaceOf(number, orientation, turn, size);
        gc.drawImage(surface, cx - radius, cy - radius, radius * 2, radius * 2);
        paintNumber(gc, number, turn, cx, cy, radius);
        BallPainter.paintShading(gc, cx, cy, radius);
    }

    /**
     * One image per ball number, retraced only when the ball has turned or the zoom changed. Each number must be
     * drawn at most once per frame: the canvas reads the image when it renders, not when it is drawn.
     */
    private WritableImage surfaceOf(int number, Orientation orientation, double[] turn, int size) {
        TracedSurface traced = surfaces.get(number);
        if (traced != null && (int) traced.image().getWidth() == size && traced.orientation().equals(orientation)) {
            return traced.image();
        }
        WritableImage surface = traced != null && (int) traced.image().getWidth() == size
                ? traced.image()
                : new WritableImage(size, size);
        surfaces.put(number, new TracedSurface(surface, orientation));
        if (pixels.length < size * size) {
            pixels = new int[size * size];
        }
        int body = argb(Palette.ballColor(number));
        int white = argb(Palette.CUE_BALL);
        boolean striped = BallKind.fromNumber(number) == BallKind.STRIPE;
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                pixels[py * size + px] = tracePixel(px, py, size, turn, striped, body, white);
            }
        }
        surface.getPixelWriter().setPixels(0, 0, size, size, PixelFormat.getIntArgbInstance(), pixels, 0, size);
        return surface;
    }

    /** Averages a small grid of samples, which smooths the ball's rim and the edges of its markings. */
    private static int tracePixel(int px, int py, int size, double[] m, boolean striped, int body, int white) {
        int covered = 0;
        int red = 0;
        int green = 0;
        int blue = 0;
        for (int sy = 0; sy < SAMPLES; sy++) {
            double ny = (py + (sy + 0.5) / SAMPLES) / size * 2 - 1;
            for (int sx = 0; sx < SAMPLES; sx++) {
                double nx = (px + (sx + 0.5) / SAMPLES) / size * 2 - 1;
                double flat = nx * nx + ny * ny;
                if (flat > 1) {
                    continue;
                }
                double nz = Math.sqrt(1 - flat);
                // The inverse of a rotation is its transpose: world point back to the ball's own frame.
                double by = m[1] * nx + m[4] * ny + m[7] * nz;
                double bz = m[2] * nx + m[5] * ny + m[8] * nz;
                boolean onNumberDisc = Math.abs(bz) > NUMBER_DISC_COS;
                boolean onWhiteCap = striped && Math.abs(by) > BallPainter.STRIPE_HALF_HEIGHT;
                int colour = onNumberDisc || onWhiteCap ? white : body;
                red += (colour >> 16) & 0xFF;
                green += (colour >> 8) & 0xFF;
                blue += colour & 0xFF;
                covered++;
            }
        }
        if (covered == 0) {
            return 0;
        }
        int alpha = covered * 255 / (SAMPLES * SAMPLES);
        return alpha << 24 | (red / covered) << 16 | (green / covered) << 8 | (blue / covered);
    }

    /**
     * Draws the number on whichever disc faces the viewer, through the affine map that takes the disc's flat
     * plane onto the screen: upright when the disc faces straight up, squashed and turned as it rolls away.
     */
    private static void paintNumber(GraphicsContext gc, int number, double[] m, double cx, double cy, double radius) {
        double facing = m[8];
        double side = Math.signum(facing);
        double opacity = smoothStep(NUMBER_HIDDEN_FACING, NUMBER_SHOWN_FACING, Math.abs(facing));
        if (opacity <= 0 || radius * 2 < 1) {
            return;
        }
        gc.save();
        // Clipping is costly, and a number facing mostly up cannot reach past the ball's rim, so only clip near
        // the rim.
        if (Math.abs(facing) < NUMBER_CLIP_FACING) {
            BallPainter.clipToCircle(gc, cx, cy, radius);
        }
        gc.setGlobalAlpha(opacity);
        // Seen from outside, the far disc's frame is the near one turned half round the y axis, so x flips.
        // The origin goes to the middle of the disc's rim, which is where the white circle seen on screen is
        // centred, not the top of the disc, which drifts toward the ball's edge as the disc turns away.
        double rimDepth = radius * NUMBER_DISC_COS * side;
        gc.transform(side * m[0], side * m[3], m[1], m[4], cx + rimDepth * m[2], cy + rimDepth * m[5]);
        gc.setFill(Palette.BALL_NUMBER_INK);
        BallNumber.fillCentred(gc, number, radius * BallPainter.NUMBER_SIZE, 0, 0);
        gc.restore();
    }

    private static double smoothStep(double from, double to, double value) {
        double t = Math.clamp((value - from) / (to - from), 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static int argb(Color colour) {
        return 0xFF << 24
                | (int) Math.round(colour.getRed() * 255) << 16
                | (int) Math.round(colour.getGreen() * 255) << 8
                | (int) Math.round(colour.getBlue() * 255);
    }
}
