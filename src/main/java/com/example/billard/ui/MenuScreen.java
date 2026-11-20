package com.example.billard.ui;

import com.example.billard.model.BotLevel;
import com.example.billard.net.OnlineLink;
import java.util.List;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Title screen: the name, a one-line pitch, the actions, and the rack on the table under the lamp. A match starts
 * against a bot, whose level is picked on its own page, for two players sharing the table, or online against a
 * player on another computer, set up on the online page.
 */
public final class MenuScreen extends HBox {

    /** What the menu starts: a match on this computer, an online match over a connected link, or quitting. */
    public record Actions(Consumer<MatchMode> onStart, Consumer<OnlineLink> onOnlineStart, Runnable onQuit) {
    }

    private static final double TEXT_COLUMN_WIDTH = 480;
    private static final double TEXT_COLUMN_PADDING = 44;
    private static final double LOGO_SIZE = 88;
    private static final double BRAND_GAP = 6;
    private static final double LEVEL_BUTTON_WIDTH = 112;
    private static final BotLevel SUGGESTED_LEVEL = BotLevel.MEDIUM;

    private final Button botButton = new Button("Play vs bot");
    private final Button backButton = new Button("Back");
    private final Button suggestedLevelButton = new Button(SUGGESTED_LEVEL.label());
    private final VBox mainColumn;
    private final VBox levelColumn;
    private final VBox howToColumn;
    private final SettingsPanel settingsColumn;
    private final OnlineLobby onlineColumn;

    public MenuScreen(GameAudio audio, OnlinePreferences onlinePreferences, Actions actions) {
        this.mainColumn = buildMainColumn(actions.onStart(), actions.onQuit());
        this.levelColumn = buildLevelColumn(actions.onStart());
        this.howToColumn = buildHowToColumn();
        this.settingsColumn = new SettingsPanel(audio, this::showMain);
        this.onlineColumn = new OnlineLobby(onlinePreferences, actions.onOnlineStart(), this::showMain);
        levelColumn.setVisible(false);
        howToColumn.setVisible(false);
        settingsColumn.setVisible(false);
        onlineColumn.setVisible(false);

        StackPane textSide = new StackPane(mainColumn, levelColumn, howToColumn, settingsColumn, onlineColumn);
        textSide.setMinWidth(TEXT_COLUMN_WIDTH);
        textSide.setMaxWidth(TEXT_COLUMN_WIDTH);
        textSide.setPadding(new Insets(TEXT_COLUMN_PADDING));

        textSide.getStyleClass().add("menu-side");

        RackArt art = new RackArt();
        HBox.setHgrow(art, Priority.ALWAYS);

        getChildren().addAll(textSide, art);
    }

    private VBox buildMainColumn(Consumer<MatchMode> onStart, Runnable onQuit) {
        Label title = new Label("Billard");
        title.getStyleClass().add("display");
        VBox brand = new VBox(BRAND_GAP, new ImageView(AppIcon.scaledTo(LOGO_SIZE)), title);
        Label pitch = new Label("Eight-ball against the bot or a friend, here or online. Pot your group, then the "
                + "eight.");
        pitch.getStyleClass().add("lead");
        pitch.setWrapText(true);

        botButton.getStyleClass().add("primary");
        botButton.setOnAction(event -> showLevels());
        botButton.setAccessibleHelp("Choose a bot level, then start a match against the computer");
        Button twoPlayers = new Button("Two players");
        twoPlayers.setOnAction(event -> onStart.accept(MatchMode.twoPlayers()));
        twoPlayers.setAccessibleHelp("Start a match for two people taking turns at this computer");
        Button online = new Button("Play online");
        online.setOnAction(event -> showOnline());
        online.setAccessibleHelp("Host or join a match against a player on another computer");
        Button howTo = new Button("How to play");
        howTo.setOnAction(event -> showHowTo());
        Button settings = new Button("Settings");
        settings.setOnAction(event -> showSettings());
        Button quit = new Button("Quit");
        quit.setOnAction(event -> onQuit.run());

        VBox actions = new VBox(12, new HBox(12, botButton, twoPlayers), new HBox(12, online),
                new HBox(12, howTo, settings, quit));
        VBox column = new VBox(28, brand, pitch, actions);
        column.setAlignment(Pos.CENTER_LEFT);
        return column;
    }

    /** One button per bot level, each next to what playing it is like, and a way back. */
    private VBox buildLevelColumn(Consumer<MatchMode> onStart) {
        Label heading = new Label("Choose a bot");
        heading.getStyleClass().add("display-medium");

        VBox levels = new VBox(16);
        for (BotLevel level : BotLevel.values()) {
            Button start = level == SUGGESTED_LEVEL ? suggestedLevelButton : new Button(level.label());
            start.setMinWidth(LEVEL_BUTTON_WIDTH);
            start.setAccessibleHelp(describe(level));
            start.setOnAction(event -> onStart.accept(MatchMode.versusBot(level)));
            Label text = new Label(describe(level));
            text.setWrapText(true);
            HBox row = new HBox(16, start, text);
            row.setAlignment(Pos.CENTER_LEFT);
            levels.getChildren().add(row);
        }
        suggestedLevelButton.getStyleClass().add("primary");

        Button back = new Button("Back");
        back.setOnAction(event -> showMain());
        VBox column = new VBox(22, heading, scrolling(levels), back);
        column.setAlignment(Pos.CENTER_LEFT);
        return column;
    }

    /** What playing against {@code level} is like, for the level page and How to play. */
    private static String describe(BotLevel level) {
        return switch (level) {
            case EASY -> "Still learning. Misses most pots and never plays safe: you would have to try to lose.";
            case MEDIUM -> "A casual player. Pots the easy ones and misses plenty. Tough for beginners, "
                    + "beatable for regulars.";
            case HARD -> "A strong player. Pots most shots and plays for position, but misses now and then.";
            case BOSS -> "Never misses. Every shot is played perfectly; give it one turn and the match is over.";
        };
    }

    private VBox buildHowToColumn() {
        Label heading = new Label("How to play");
        heading.getStyleClass().add("display-medium");

        VBox rules = new VBox(14,
                rule("Opponent", "Play vs bot puts you against the computer at the level you pick: you are "
                        + "Player 1 and break, the bot plays as Player 2. Two players is for two people taking turns "
                        + "at the same table. New match and Play again keep your choice."),
                rule("Bot levels", levelSummary()),
                rule("Online", "Play online lets you host a match or join one. Directly: the host shares their "
                        + "address and the other player types it in; this works on the same network, or over the "
                        + "internet with a forwarded port or a shared VPN. Through a relay server: the host gets a "
                        + "room code to pass on, and nobody needs to change their router. The host breaks first, "
                        + "then the break alternates with every rematch. You see your opponent line up live, can "
                        + "send quick chat messages, and if the connection drops the game reconnects on its own for "
                        + "up to a minute."),
                rule("Bot's turn", "The bot thinks, places the cue ball if it has ball in hand, lines up and shoots "
                        + "on its own. The table and shot controls wait meanwhile; Settings, Menu and New match "
                        + "still work."),
                rule("Aim", "Move the mouse, or use \u2190 \u2192. Hold Shift for fine steps."),
                rule("Lock aim", "Right click fixes your aim so you can set the power. Right click again to aim anew."),
                rule("Power", "Scroll the wheel, use \u2191 \u2193, or click the power bar."),
                rule("Spin", "Press Spin and pick where the cue strikes the cue ball: low to draw it back, high to "
                        + "follow through, left or right for side. Centre takes the spin off."),
                rule("Shoot", "Left click, or press Space or Enter."),
                rule("Ball in hand", "After a foul, move the cue ball with the mouse or the arrow keys, then click "
                        + "or press Enter to place it."),
                rule("Full screen", "Press F to switch full screen on or off."),
                rule("Groups", "The first ball you pot on the open table decides your group: solids or stripes."),
                rule("Win", "Clear your group, then pot the 8-ball. Pot it early and you lose."),
                rule("Fouls", "Scratching, hitting the wrong ball first, or a shot where nothing is potted and no ball "
                        + "reaches a cushion gives your opponent ball in hand."));

        backButton.getStyleClass().add("primary");
        backButton.setOnAction(event -> showMain());

        VBox column = new VBox(22, heading, scrolling(rules), backButton);
        column.setAlignment(Pos.CENTER_LEFT);
        return column;
    }

    private static String levelSummary() {
        StringBuilder summary = new StringBuilder();
        for (BotLevel level : BotLevel.values()) {
            summary.append(summary.isEmpty() ? "" : "\n").append(level.label()).append(": ").append(describe(level));
        }
        return summary.toString();
    }

    /** Lets {@code content} scroll when the window is too short for all of it. */
    private static ScrollPane scrolling(VBox content) {
        ScrollPane scroller = new ScrollPane(content);
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.getStyleClass().add("how-to-scroller");
        VBox.setVgrow(scroller, Priority.ALWAYS);
        return scroller;
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
        botButton.requestFocus();
    }

    private void showLevels() {
        showOnly(levelColumn);
        suggestedLevelButton.requestFocus();
    }

    private void showHowTo() {
        showOnly(howToColumn);
        backButton.requestFocus();
    }

    private void showOnline() {
        showOnly(onlineColumn);
        onlineColumn.focusFirstControl();
    }

    private void showSettings() {
        showOnly(settingsColumn);
        settingsColumn.focusFirstControl();
    }

    /** Shows {@code column} alone; leaving the online page gives up any match being set up there. */
    private void showOnly(Node column) {
        if (column != onlineColumn) {
            onlineColumn.cancel();
        }
        for (Node each : List.of(mainColumn, levelColumn, howToColumn, settingsColumn, onlineColumn)) {
            each.setVisible(each == column);
        }
    }
}
