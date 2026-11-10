package com.example.billard.ui;

import com.example.billard.model.Ball;
import com.example.billard.model.RackFactory;
import com.example.billard.model.Table;
import com.example.billard.model.Vec2;
import java.util.List;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;

/** The menu picture: a fresh rack and the cue lined up on the cue ball, redrawn to fit whatever size the pane has. */
final class RackArt extends Pane {

    private static final double RADIUS_SHARE_OF_WIDTH = 0.042;
    private static final double RADIUS_SHARE_OF_HEIGHT = 0.06;
    private static final double RACK_SHIFT_SHARE_OF_WIDTH = -0.12;
    private static final double CUE_BALL_SHARE_OF_WIDTH = 0.36;
    private static final double CUE_POWER = 0.1;

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
        double radius = Math.min(width * RADIUS_SHARE_OF_WIDTH, height * RADIUS_SHARE_OF_HEIGHT);
        List<Ball> balls = arrangeBalls(width, height, radius);

        for (Ball ball : balls) {
            BallPainter.paint(gc, ball.number(), ball.position().x(), ball.position().y(), radius);
        }
        CueRenderer.drawCue(gc, balls.get(0).position(), 0, CUE_POWER, radius);
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
