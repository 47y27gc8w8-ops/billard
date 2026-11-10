package com.example.billard.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Title screen: the name, a one-line pitch, the actions, and the rack on a field of cloth. */
public final class MenuScreen extends HBox {

    private static final double TEXT_COLUMN_WIDTH = 480;
    private static final double TEXT_COLUMN_PADDING = 44;
    private static final double RAIL_STRIP_WIDTH = 28;

    private final Button startButton = new Button("Start match");
    private final Button backButton = new Button("Back");
    private final VBox mainColumn;
    private final VBox howToColumn;

    public MenuScreen(Runnable onStart, Runnable onQuit) {
        this.mainColumn = buildMainColumn(onStart, onQuit);
        this.howToColumn = buildHowToColumn();
        howToColumn.setVisible(false);

        StackPane textSide = new StackPane(mainColumn, howToColumn);
        textSide.setMinWidth(TEXT_COLUMN_WIDTH);
        textSide.setMaxWidth(TEXT_COLUMN_WIDTH);
        textSide.setPadding(new Insets(TEXT_COLUMN_PADDING));

        Region railStrip = new Region();
        railStrip.getStyleClass().add("rail-strip");
        railStrip.setMinWidth(RAIL_STRIP_WIDTH);

        RackArt art = new RackArt();
        StackPane clothField = new StackPane(art);
        clothField.getStyleClass().add("cloth-field");
        HBox.setHgrow(clothField, Priority.ALWAYS);

        getChildren().addAll(textSide, railStrip, clothField);
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
        howTo.setOnAction(event -> showHowTo(true));
        Button quit = new Button("Quit");
        quit.setOnAction(event -> onQuit.run());

        HBox actions = new HBox(12, startButton, howTo, quit);
        VBox column = new VBox(28, title, pitch, actions);
        column.setAlignment(Pos.CENTER_LEFT);
        return column;
    }

    private VBox buildHowToColumn() {
        Label heading = new Label("How to play");
        heading.getStyleClass().add("display-medium");

        VBox rules = new VBox(14,
                rule("Aim", "Move the mouse, or use ← →. Hold Shift for fine steps."),
                rule("Power", "Scroll the wheel, use ↑ ↓, or click the power bar."),
                rule("Shoot", "Left click or press Space."),
                rule("Groups", "The first ball you pot on the open table decides your group: solids or stripes."),
                rule("Win", "Clear your group, then pot the 8-ball. Pot it early and you lose."),
                rule("Fouls", "Scratching, hitting the wrong ball first, or a shot where nothing is potted and no ball "
                        + "reaches a cushion gives your opponent ball in hand."));

        backButton.getStyleClass().add("primary");
        backButton.setOnAction(event -> showHowTo(false));

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

    private void showHowTo(boolean visible) {
        howToColumn.setVisible(visible);
        mainColumn.setVisible(!visible);
        (visible ? backButton : startButton).requestFocus();
    }
}
