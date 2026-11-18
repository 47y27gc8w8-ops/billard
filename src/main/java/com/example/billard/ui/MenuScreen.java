package com.example.billard.ui;

import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Title screen: the name, a one-line pitch, the actions, and the rack on the table under the lamp. */
public final class MenuScreen extends HBox {

    private static final double TEXT_COLUMN_WIDTH = 480;
    private static final double TEXT_COLUMN_PADDING = 44;

    private final Button startButton = new Button("Start match");
    private final Button backButton = new Button("Back");
    private final VBox mainColumn;
    private final VBox howToColumn;
    private final SettingsPanel settingsColumn;

    public MenuScreen(GameAudio audio, Runnable onStart, Runnable onQuit) {
        this.mainColumn = buildMainColumn(onStart, onQuit);
        this.howToColumn = buildHowToColumn();
        this.settingsColumn = new SettingsPanel(audio, this::showMain);
        howToColumn.setVisible(false);
        settingsColumn.setVisible(false);

        StackPane textSide = new StackPane(mainColumn, howToColumn, settingsColumn);
        textSide.setMinWidth(TEXT_COLUMN_WIDTH);
        textSide.setMaxWidth(TEXT_COLUMN_WIDTH);
        textSide.setPadding(new Insets(TEXT_COLUMN_PADDING));

        textSide.getStyleClass().add("menu-side");

        RackArt art = new RackArt();
        HBox.setHgrow(art, Priority.ALWAYS);

        getChildren().addAll(textSide, art);
    }

    private VBox buildMainColumn(Runnable onStart, Runnable onQuit) {
        Label title = new Label("Billard");
        title.getStyleClass().add("display");
        Label pitch = new Label("Eight-ball for two at one table. Pot your group, then the eight.");
        pitch.getStyleClass().add("lead");
        pitch.setWrapText(true);

        startButton.getStyleClass().add("primary");
        startButton.setOnAction(event -> onStart.run());
        Button howTo = new Button("How to play");
        howTo.setOnAction(event -> showHowTo());
        Button settings = new Button("Settings");
        settings.setOnAction(event -> showSettings());
        Button quit = new Button("Quit");
        quit.setOnAction(event -> onQuit.run());

        VBox actions = new VBox(12, new HBox(12, startButton, howTo), new HBox(12, settings, quit));
        VBox column = new VBox(28, title, pitch, actions);
        column.setAlignment(Pos.CENTER_LEFT);
        return column;
    }

    private VBox buildHowToColumn() {
        Label heading = new Label("How to play");
        heading.getStyleClass().add("display-medium");

        VBox rules = new VBox(14,
                rule("Aim", "Move the mouse, or use ← →. Hold Shift for fine steps."),
                rule("Lock aim", "Right click fixes your aim so you can set the power. Right click again to aim anew."),
                rule("Power","Scroll the wheel, use ↑ ↓, or click the power bar."),
                rule("Shoot", "Left click or press Space."),
                rule("Groups", "The first ball you pot on the open table decides your group: solids or stripes."),
                rule("Win", "Clear your group, then pot the 8-ball. Pot it early and you lose."),
                rule("Fouls", "Scratching, hitting the wrong ball first, or a shot where nothing is potted and no ball "
                        + "reaches a cushion gives your opponent ball in hand."));

        backButton.getStyleClass().add("primary");
        backButton.setOnAction(event -> showMain());

        VBox column = new VBox(22, heading, rules, backButton);
        column.setAlignment(Pos.CENTER_LEFT);
        return column;
    }

    private static VBox rule(String name, String text) {
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("rule-name");
        Label textLabel = new Label(text);
        textLabel.setWrapText(true);
        return new VBox(2, nameLabel, textLabel);
    }

    private void showMain() {
        showOnly(mainColumn);
        startButton.requestFocus();
    }

    private void showHowTo() {
        showOnly(howToColumn);
        backButton.requestFocus();
    }

    private void showSettings() {
        showOnly(settingsColumn);
        settingsColumn.focusFirstControl();
    }

    private void showOnly(VBox column) {
        for (VBox each : List.of(mainColumn, howToColumn, settingsColumn)) {
            each.setVisible(each == column);
        }
    }
}
