package com.example.billard.ui;

import com.example.billard.model.Ball;
import com.example.billard.model.BallKind;
import com.example.billard.model.GamePhase;
import com.example.billard.model.GameSession;
import com.example.billard.model.Opponent;
import com.example.billard.model.Player;
import com.example.billard.model.ShotPlanner;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * The match screen: player cards and status on top, the table in the middle, power and controls below. Runs the
 * frame loop that advances the match and keeps the screen in step with it. The settings card opens over all of it;
 * balls keep rolling meanwhile. Against the bot or a player online, the table and shot controls ignore input while
 * the opponent has the table.
 */
public final class GameScreen extends StackPane {

    /** Where the screen's buttons lead: another match with the same players, or back to the menu. */
    public record Navigation(Runnable onPlayAgain, Runnable onMenu) {
    }

    private static final double MAX_FRAME_SECONDS = 0.05;
    private static final double NANOS_PER_SECOND = 1e9;
    private static final double WIDE_LAYOUT_MIN_WIDTH = 1100;
    private static final double CARD_WIDTH = 250;
    private static final double SPIN_BUTTON_FACE_SIZE = 40;
    private static final String CONTROLS_HINT =
            "Aim: mouse or \u2190 \u2192 (Shift for fine)     Lock aim: right click     "
                    + "Power: wheel or \u2191 \u2193     Spin: click the cue ball     Shoot: click or Space";

    private final GameSession session;
    private final MatchMode mode;
    private final Optional<Opponent> opponent;
    private final BorderPane layout = new BorderPane();
    private final GameCanvas gameCanvas;
    private final PlayerCard playerOne;
    private final PlayerCard playerTwo;
    private final Label statusLabel = new Label();
    private final HBox playerRow = new HBox(24);
    private final HBox shotControls = new HBox(24);
    private final HBox matchButtons = new HBox(14);
    private final VBox bottomBar = new VBox(14);
    private final PowerMeter powerMeter;
    private final SpinPicker spinPicker;
    private final SoundEffects sounds;
    private final CueBallFace spinFace = new CueBallFace(SPIN_BUTTON_FACE_SIZE, false);
    private final Button spinButton = new Button("Spin");
    private final Button newMatchButton = new Button("New match");
    private final StackPane tableLayers;
    private final GameOverOverlay gameOverOverlay;
    private final SettingsOverlay settingsOverlay;
    private final AnimationTimer timer;
    private boolean wideLayout;

    /**
     * @param opponent who plays the seat that {@code mode} says is away; empty when both play at this keyboard
     */
    GameScreen(GameSession session, MatchMode mode, Optional<Opponent> opponent, GameAudio audio,
               Navigation navigation) {
        this.session = session;
        this.mode = mode;
        this.opponent = opponent;
        this.playerOne = new PlayerCard(mode.nameOf(Player.ONE));
        this.playerTwo = new PlayerCard(mode.nameOf(Player.TWO));
        this.gameCanvas = new GameCanvas(session);
        this.powerMeter = new PowerMeter(session::setPower);
        this.spinPicker = new SpinPicker(session, gameCanvas::requestFocus);
        this.gameOverOverlay = new GameOverOverlay(navigation.onPlayAgain(), navigation.onMenu());
        this.settingsOverlay = new SettingsOverlay(audio, gameCanvas::requestFocus);
        this.sounds = new SoundEffects(audio.effects(), SoundBank.standard(), session.table().width(),
                RandomGenerator.getDefault());
        session.addPhysicsListener(sounds);
        this.timer = createTimer();

        layout.setPadding(new Insets(28));
        this.tableLayers = new StackPane(gameCanvas, spinPicker, gameOverOverlay);
        layout.setCenter(tableLayers);
        // The overlays fit themselves to whatever room the table gets; they must not make the table area taller.
        spinPicker.setMinSize(0, 0);
        gameOverOverlay.setMinSize(0, 0);
        layout.setBottom(buildBottomBar(navigation));
        getChildren().addAll(layout, settingsOverlay);
        configureTopBar();
        widthProperty().addListener((observable, before, width) -> {
            boolean wide = width.doubleValue() >= WIDE_LAYOUT_MIN_WIDTH;
            if (wide != wideLayout) {
                arrangeBars(wide);
            }
        });
        arrangeBars(true);
    }

    /** A match on this computer: two people taking turns, or a person against the bot that {@code mode} names. */
    public static GameScreen offline(GameSession session, MatchMode mode, GameAudio audio, Navigation navigation) {
        return new GameScreen(session, mode, botFor(session, mode), audio, navigation);
    }

    public void start() {
        timer.start();
        Platform.runLater(gameCanvas::requestFocus);
    }

    public void stop() {
        timer.stop();
        opponent.ifPresent(Opponent::stop);
    }

    /** Puts {@code layer} over the table, beneath the game-over card. */
    void addTableLayer(Node layer) {
        tableLayers.getChildren().add(tableLayers.getChildren().indexOf(gameOverOverlay), layer);
    }

    /** Adds {@code control} after the power meter and spin button. */
    void addShotControl(Node control) {
        shotControls.getChildren().add(control);
    }

    /** Takes away the New match button, for matches where one player alone cannot restart. */
    void hideNewMatchButton() {
        newMatchButton.setVisible(false);
        newMatchButton.setManaged(false);
    }

    GameOverOverlay gameOverOverlay() {
        return gameOverOverlay;
    }

    private static Optional<Opponent> botFor(GameSession session, MatchMode mode) {
        return mode.botLevel().map(level ->
                new BotOpponent(session, Player.TWO, new ShotPlanner(level, RandomGenerator.getDefault())));
    }

    private void configureTopBar() {
        statusLabel.getStyleClass().add("status");
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setMinHeight(Region.USE_PREF_SIZE);
        playerOne.setPrefWidth(CARD_WIDTH);
        playerTwo.setPrefWidth(CARD_WIDTH);
        playerRow.setAlignment(Pos.CENTER_LEFT);
    }

    private void arrangeBars(boolean wide) {
        wideLayout = wide;
        arrangeTopBar(wide);
        arrangeBottomBar(wide);
    }

    /** Wide windows put the status between the cards; narrow ones give it its own line beneath them. */
    private void arrangeTopBar(boolean wide) {
        playerRow.getChildren().clear();
        if (wide) {
            HBox.setHgrow(statusLabel, Priority.ALWAYS);
            playerRow.getChildren().addAll(playerOne, statusLabel, playerTwo);
            layout.setTop(playerRow);
        } else {
            HBox.setHgrow(statusLabel, Priority.NEVER);
            playerRow.getChildren().addAll(playerOne, spacer(), playerTwo);
            VBox top = new VBox(12, playerRow, statusLabel);
            layout.setTop(top);
        }
    }

    private VBox buildBottomBar(Navigation navigation) {
        Button menuButton = new Button("Menu");
        menuButton.setOnAction(event -> navigation.onMenu().run());
        newMatchButton.setOnAction(event -> navigation.onPlayAgain().run());
        Button settingsButton = new Button("Settings");
        settingsButton.setOnAction(event -> settingsOverlay.open());
        Label hint = new Label(CONTROLS_HINT);
        hint.getStyleClass().add("muted");
        hint.setWrapText(true);

        configureSpinButton();

        shotControls.getChildren().addAll(powerMeter, spinButton);
        shotControls.setAlignment(Pos.CENTER_LEFT);
        matchButtons.getChildren().addAll(settingsButton, menuButton, newMatchButton);
        matchButtons.setAlignment(Pos.CENTER_LEFT);
        bottomBar.getChildren().setAll(new HBox(), hint);
        bottomBar.setPadding(new Insets(20, 0, 0, 0));
        return bottomBar;
    }

    /** Wide windows keep the shot controls and match buttons on one line; narrow ones stack them. */
    private void arrangeBottomBar(boolean wide) {
        Region controls = wide
                ? new HBox(24, shotControls, spacer(), matchButtons)
                : new VBox(14, shotControls, matchButtons);
        bottomBar.getChildren().set(0, controls);
    }

    private void configureSpinButton() {
        spinButton.setGraphic(spinFace);
        spinButton.setContentDisplay(ContentDisplay.RIGHT);
        spinButton.setGraphicTextGap(12);
        spinButton.getStyleClass().add("spin-button");
        spinButton.setAccessibleHelp("Choose where the cue hits the cue ball");
        spinButton.setOnAction(event -> spinPicker.open());
    }

    private static Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private AnimationTimer createTimer() {
        return new AnimationTimer() {
            private long previousNanos;

            @Override
            public void handle(long nowNanos) {
                double elapsed = 0;
                if (previousNanos != 0) {
                    elapsed = Math.min((nowNanos - previousNanos) / NANOS_PER_SECOND, MAX_FRAME_SECONDS);
                    advanceMatch(elapsed);
                }
                previousNanos = nowNanos;
                gameCanvas.draw(elapsed);
                refreshHud();
            }
        };
    }

    private void advanceMatch(double elapsedSeconds) {
        session.advance(elapsedSeconds);
        sounds.playQueuedClacks();
        opponent.ifPresent(player -> player.update(elapsedSeconds));
    }

    private void refreshHud() {
        playerOne.update(cardViewOf(Player.ONE));
        playerTwo.update(cardViewOf(Player.TWO));
        statusLabel.setText(StatusText.describe(session, mode));
        powerMeter.update(session.power());
        spinFace.show(session.tipContact());
        boolean waiting = isOpponentAtTheTable();
        gameCanvas.setInputEnabled(!waiting);
        powerMeter.setDisable(waiting);
        spinButton.setDisable(waiting || session.phase() != GamePhase.AIMING);
        showGameOverIfFinished(waiting);
    }

    private boolean isOpponentAtTheTable() {
        return opponent.map(Opponent::hasTheTable).orElse(false);
    }

    private PlayerCard.View cardViewOf(Player player) {
        BallKind group = session.match().groupOf(player).orElse(null);
        String groupText = group == null ? "Open table" : group.groupName();
        boolean active = session.currentPlayer() == player && session.phase() != GamePhase.GAME_OVER;
        return new PlayerCard.View(active, groupText, group, pottedBallsOf(group));
    }

    private List<Integer> pottedBallsOf(BallKind group) {
        if (group == null) {
            return List.of();
        }
        return session.balls().stream()
                .filter(Ball::isPocketed)
                .filter(ball -> ball.kind() == group)
                .map(Ball::number)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    /** Waits while the opponent still has the table, since an online result may yet be corrected. */
    private void showGameOverIfFinished(boolean waiting) {
        boolean finished = session.phase() == GamePhase.GAME_OVER;
        if (finished && !waiting && !gameOverOverlay.isVisible()) {
            gameOverOverlay.show(StatusText.winnerHeadline(session, mode), StatusText.gameOverReason(session, mode));
            sounds.playFanfare();
        }
    }
}
