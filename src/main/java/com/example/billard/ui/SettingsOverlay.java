package com.example.billard.ui;

import javafx.geometry.Insets;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** The settings card over the match screen. Escape, Done or a click beside the card closes it. */
final class SettingsOverlay extends StackPane {

    private final SettingsPanel panel;
    private final Runnable onClosed;

    /** @param onClosed runs after the overlay has been hidden, for instance to hand the focus back to the table */
    SettingsOverlay(GameAudio audio, Runnable onClosed) {
        this.onClosed = onClosed;
        this.panel = new SettingsPanel(audio, this::close);

        VBox card = new VBox(panel);
        card.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        card.setPadding(new Insets(36, 48, 36, 48));
        card.getStyleClass().add("overlay-card");

        getChildren().add(card);
        getStyleClass().add("overlay");
        setVisible(false);
        setOnMouseClicked(event -> {
            if (event.getTarget() == this) {
                close();
            }
        });
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
                event.consume();
            }
        });
    }

    void open() {
        setVisible(true);
        panel.focusFirstControl();
    }

    void close() {
        setVisible(false);
        onClosed.run();
    }
}
