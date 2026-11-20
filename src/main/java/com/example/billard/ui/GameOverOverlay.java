package com.example.billard.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Covers the table once the match is decided: who won and why, then play again or back to the menu. Online, it
 * also shows how a rematch request stands, and covers the table when the match ends early because the other
 * player left.
 */
final class GameOverOverlay extends StackPane {

    private static final String PLAY_AGAIN = "Play again";

    private final Label winnerLabel = new Label();
    private final Label reasonLabel = new Label();
    private final Label noteLabel = new Label();
    private final Button playAgainButton = new Button(PLAY_AGAIN);
    private final Button menuButton = new Button("Main menu");

    GameOverOverlay(Runnable onPlayAgain, Runnable onMenu) {
        winnerLabel.getStyleClass().add("display-medium");
        reasonLabel.getStyleClass().add("lead");
        reasonLabel.setWrapText(true);
        noteLabel.getStyleClass().add("muted");
        noteLabel.setWrapText(true);
        showNote("");
        playAgainButton.getStyleClass().add("primary");
        playAgainButton.setOnAction(event -> onPlayAgain.run());
        menuButton.setOnAction(event -> onMenu.run());

        VBox card = new VBox(18, winnerLabel, reasonLabel, noteLabel, new HBox(14, playAgainButton, menuButton));
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

    /** Shows why the match cannot go on, with only the way back to the menu. Replaces any result shown. */
    void showEnded(String headline, String reason) {
        show(headline, reason);
        showNote("");
        playAgainButton.setVisible(false);
        playAgainButton.setManaged(false);
        menuButton.requestFocus();
    }

    /** Relabels the play-again button, for example while waiting for the other player to agree to a rematch. */
    void showPlayAgainState(String buttonText, boolean enabled) {
        playAgainButton.setText(buttonText);
        playAgainButton.setDisable(!enabled);
    }

    /** A line under the result, such as "Sam wants a rematch."; empty hides it. */
    void showNote(String note) {
        noteLabel.setText(note);
        noteLabel.setVisible(!note.isEmpty());
        noteLabel.setManaged(!note.isEmpty());
    }

    boolean isEnded() {
        return isVisible() && !playAgainButton.isVisible();
    }
}
