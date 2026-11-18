package com.example.billard.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Covers the table once the match is decided: who won and why, then a new match or back to the menu. */
final class GameOverOverlay extends StackPane {

    private final Label winnerLabel = new Label();
    private final Label reasonLabel = new Label();
    private final Button playAgainButton = new Button("Play again");

    GameOverOverlay(Runnable onNewMatch, Runnable onMenu) {
        winnerLabel.getStyleClass().add("display-medium");
        reasonLabel.getStyleClass().add("lead");
        reasonLabel.setWrapText(true);
        playAgainButton.getStyleClass().add("primary");
        playAgainButton.setOnAction(event -> onNewMatch.run());
        Button menuButton = new Button("Main menu");
        menuButton.setOnAction(event -> onMenu.run());

        VBox card = new VBox(18, winnerLabel, reasonLabel, new HBox(14, playAgainButton, menuButton));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        card.setPadding(new Insets(40, 48, 40, 48));
        card.getStyleClass().add("overlay-card");

        getChildren().add(card);
        getStyleClass().add("overlay");
        setVisible(false);
    }

    /** Shows the result and puts the focus on playing again. */
    void show(String headline, String reason) {
        winnerLabel.setText(headline);
        reasonLabel.setText(reason);
        setVisible(true);
        playAgainButton.requestFocus();
    }
}
