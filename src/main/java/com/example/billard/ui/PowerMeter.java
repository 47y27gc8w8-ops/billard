package com.example.billard.ui;

import java.util.function.DoubleConsumer;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

/** Horizontal shot-power bar. Click or drag on it to set the power. */
final class PowerMeter extends HBox {

    private static final double BAR_WIDTH = 220;
    private static final double BAR_HEIGHT = 22;

    private final Canvas bar = new Canvas(BAR_WIDTH, BAR_HEIGHT);
    private final Label percentLabel = new Label();
    private double shownPower = -1;

    PowerMeter(DoubleConsumer onPowerChosen) {
        super(14);
        setAlignment(Pos.CENTER_LEFT);
        Label caption = new Label("Power");
        caption.getStyleClass().add("muted");
        percentLabel.setMinWidth(48);
        getChildren().addAll(caption, bar, percentLabel);

        bar.setOnMousePressed(event -> choose(event, onPowerChosen));
        bar.setOnMouseDragged(event -> choose(event, onPowerChosen));
    }

    void update(double power) {
        if (power == shownPower) {
            return;
        }
        shownPower = power;
        percentLabel.setText(Math.round(power * 100) + "%");
        setAccessibleText("Shot power " + percentLabel.getText());
        GraphicsContext gc = bar.getGraphicsContext2D();
        drawGroove(gc);
        drawFill(gc, power);
    }

    /** A channel cut into the panel: darker than it, shaded under its top edge. */
    private static void drawGroove(GraphicsContext gc) {
        gc.setFill(Palette.GROUND);
        gc.fillRect(0, 0, BAR_WIDTH, BAR_HEIGHT);
        gc.setFill(new LinearGradient(0, 0, 0, BAR_HEIGHT, false, CycleMethod.NO_CYCLE,
                new Stop(0, Lighting.black(0.7)), new Stop(0.35, Color.TRANSPARENT),
                new Stop(1, Lighting.white(0.06))));
        gc.fillRect(0, 0, BAR_WIDTH, BAR_HEIGHT);
    }

    /** The chalk-blue charge, lit from above like the rest of the room. */
    private static void drawFill(GraphicsContext gc, double power) {
        double inset = 3;
        double width = (BAR_WIDTH - 2 * inset) * power;
        gc.setFill(new LinearGradient(0, inset, 0, BAR_HEIGHT - inset, false, CycleMethod.NO_CYCLE,
                new Stop(0, Palette.ACCENT.deriveColor(0, 0.7, 1.25, 1)),
                new Stop(0.45, Palette.ACCENT),
                new Stop(1, Palette.ACCENT.deriveColor(0, 1, 0.6, 1))));
        gc.fillRect(inset, inset, width, BAR_HEIGHT - 2 * inset);
    }

    private void choose(MouseEvent event, DoubleConsumer onPowerChosen) {
        onPowerChosen.accept(event.getX() / BAR_WIDTH);
    }
}
