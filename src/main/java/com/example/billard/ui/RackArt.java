package com.example.billard.ui;

import com.example.billard.model.Ball;
import com.example.billard.model.RackFactory;
import com.example.billard.model.Table;
import com.example.billard.model.Vec2;
import java.util.List;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * The menu picture: a corner of the table under the lamp. A walnut rail and cushion down the left edge, felt, a
 * fresh rack and the cue lined up on the cue ball, redrawn to fit whatever size the pane has.
 */
final class RackArt extends Pane {

    private static final double RADIUS_SHARE_OF_WIDTH = 0.042;
    private static final double RADIUS_SHARE_OF_HEIGHT = 0.06;
    private static final double RACK_SHIFT_SHARE_OF_WIDTH = -0.12;
    private static final double CUE_BALL_SHARE_OF_WIDTH = 0.36;
    private static final double CUE_POWER = 0.1;
    private static final double RAIL_WIDTH = 36;
    private static final double CUSHION_WIDTH = 16;
    private static final double OCCLUSION_DEPTH = 22;

    private final Canvas canvas = new Canvas();

    RackArt() {
        getChildren().add(canvas);
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        canvas.widthProperty().addListener(observable -> redraw());
        canvas.heightProperty().addListener(observable -> redraw());
        setMinSize(0, 0);
        setAccessibleText("A freshly racked set of fifteen pool balls with a cue lined up on the cue ball.");
    }

    private void redraw() {
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);
        if (width < 1 || height < 1) {
            return;
        }
        double pixelsPerUnit = Viewport.outputScaleOf(this);
        drawCloth(gc, width, height, pixelsPerUnit);
        drawRailAndCushion(gc, height, pixelsPerUnit);

        double radius = Math.min(width * RADIUS_SHARE_OF_WIDTH, height * RADIUS_SHARE_OF_HEIGHT);
        List<Ball> balls = arrangeBalls(width, height, radius);
        balls.forEach(ball -> BallPainter.paintShadow(gc, ball.position().x(), ball.position().y(), radius));
        balls.forEach(ball ->
                BallPainter.paintSphere(gc, ball.number(), ball.position().x(), ball.position().y(), radius));
        CueRenderer.drawCue(gc, balls.get(0).position(), 0, CUE_POWER, radius);
    }

    private static void drawCloth(GraphicsContext gc, double width, double height, double pixelsPerUnit) {
        gc.setFill(TableRenderer.texture(FeltTexture.clothTile(), pixelsPerUnit));
        gc.fillRect(0, 0, width, height);
        gc.setFill(new RadialGradient(0, 0, 0.62, 0.5, 0.65, true, CycleMethod.NO_CYCLE,
                new Stop(0, Lighting.white(0.08)),
                new Stop(0.35, Color.TRANSPARENT),
                new Stop(0.8, Lighting.black(0.35)),
                new Stop(1, Lighting.black(0.6))));
        gc.fillRect(0, 0, width, height);
    }

    private static void drawRailAndCushion(GraphicsContext gc, double height, double pixelsPerUnit) {
        double cushionEdge = RAIL_WIDTH + CUSHION_WIDTH;
        gc.setFill(new LinearGradient(cushionEdge, 0, cushionEdge + OCCLUSION_DEPTH, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0, Lighting.black(0.45)), new Stop(1, Color.TRANSPARENT)));
        gc.fillRect(cushionEdge, 0, OCCLUSION_DEPTH, height);

        gc.setFill(new LinearGradient(cushionEdge, 0, RAIL_WIDTH, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0, Lighting.black(0.35)),
                new Stop(0.12, Lighting.white(0.1)),
                new Stop(0.7, Lighting.black(0.2)),
                new Stop(1, Lighting.black(0.55))));
        gc.fillRect(RAIL_WIDTH, 0, CUSHION_WIDTH, height);

        gc.setFill(TableRenderer.texture(WoodTexture.acrossTile(), pixelsPerUnit));
        gc.fillRect(0, 0, RAIL_WIDTH, height);
        gc.setFill(TableRenderer.railProfile(0, 0, RAIL_WIDTH, 0));
        gc.fillRect(0, 0, RAIL_WIDTH, height);
    }

    /** The standard rack, slid left so it clears the right edge, with the cue ball set back from it. */
    private static List<Ball> arrangeBalls(double width, double height, double radius) {
        List<Ball> balls = RackFactory.standardRack(new Table(width, height, radius, List.of()));
        Vec2 shift = new Vec2(width * RACK_SHIFT_SHARE_OF_WIDTH, 0);
        balls.stream().skip(1).forEach(ball -> ball.setPosition(ball.position().plus(shift)));
        balls.get(0).setPosition(new Vec2(width * CUE_BALL_SHARE_OF_WIDTH, height / 2));
        return balls;
    }
}
