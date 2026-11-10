package com.example.billard.ui;

import com.example.billard.model.BallKind;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;

/** One player's panel: name, group, whose turn it is, and the group's balls as they go down. */
final class PlayerCard extends HBox {

    /** Everything the card shows. Equal views mean nothing needs redrawing. */
    record View(boolean active, String groupText, BallKind group, List<Integer> pottedNumbers) {
    }

    private static final int SLOTS = 7;
    private static final double SLOT_WIDTH = 30;
    private static final double SLOT_RADIUS = 11;
    private static final double STRIP_HEIGHT = 28;

    private final String playerName;
    private final Label stateLabel = new Label();
    private final Canvas pottedStrip = new Canvas(SLOTS * SLOT_WIDTH, STRIP_HEIGHT);
    private View shown;

    PlayerCard(String playerName) {
        this.playerName = playerName;
        getStyleClass().add("player-card");

        Region turnMarker = new Region();
        turnMarker.getStyleClass().add("turn-marker");
        turnMarker.setMinWidth(6);

        Label nameLabel = new Label(playerName);
        nameLabel.getStyleClass().add("player-name");
        stateLabel.getStyleClass().add("muted");

        VBox text = new VBox(2, nameLabel, stateLabel, pottedStrip);
        text.setPadding(new Insets(0, 0, 0, 14));
        VBox.setMargin(pottedStrip, new Insets(8, 0, 0, 0));
        HBox.setHgrow(text, Priority.ALWAYS);
        getChildren().addAll(turnMarker, text);
    }

    void update(View view) {
        if (view.equals(shown)) {
            return;
        }
        shown = view;
        if (view.active()) {
            getStyleClass().add("active");
        } else {
            getStyleClass().remove("active");
        }
        stateLabel.setText(view.active() ? view.groupText() + " · shooting" : view.groupText());
        setAccessibleText(playerName + ", " + stateLabel.getText() + ", " + view.pottedNumbers().size() + " balls potted");
        drawStrip(view);
    }

    private void drawStrip(View view) {
        GraphicsContext gc = pottedStrip.getGraphicsContext2D();
        gc.clearRect(0, 0, pottedStrip.getWidth(), pottedStrip.getHeight());
        if (view.group() == null) {
            return;
        }
        for (int slot = 0; slot < SLOTS; slot++) {
            double cx = slot * SLOT_WIDTH + SLOT_WIDTH / 2;
            double cy = STRIP_HEIGHT / 2;
            if (slot < view.pottedNumbers().size()) {
                BallPainter.paint(gc, view.pottedNumbers().get(slot), cx, cy, SLOT_RADIUS);
            } else {
                gc.setFill(Palette.PANEL_RAISED);
                gc.fillOval(cx - SLOT_RADIUS, cy - SLOT_RADIUS, SLOT_RADIUS * 2, SLOT_RADIUS * 2);
            }
        }
    }
}
