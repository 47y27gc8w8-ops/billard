package com.example.billard.ui;

import java.util.function.DoubleConsumer;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

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
        gc.setFill(Palette.PANEL_RAISED);
        gc.fillRect(0, 0, BAR_WIDTH, BAR_HEIGHT);
        gc.setFill(Palette.ACCENT);
        gc.fillRect(0, 0, BAR_WIDTH * power, BAR_HEIGHT);
    }

    private void choose(MouseEvent event, DoubleConsumer onPowerChosen) {
        onPowerChosen.accept(event.getX() / BAR_WIDTH);
    }
}
