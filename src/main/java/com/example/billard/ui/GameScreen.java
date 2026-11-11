package com.example.billard.ui;

import com.example.billard.model.Ball;
import com.example.billard.model.BallKind;
import com.example.billard.model.GamePhase;
import com.example.billard.model.GameSession;
import com.example.billard.model.Player;
import java.util.Comparator;
import java.util.List;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** The match screen: player cards and status on top, the table in the middle, power and controls below. */
public final class GameScreen extends BorderPane {

    private static final double MAX_FRAME_SECONDS = 0.05;
    private static final double NANOS_PER_SECOND = 1e9;
    private static final double WIDE_LAYOUT_MIN_WIDTH = 1100;
    private static final double CARD_WIDTH = 250;
    private static final String CONTROLS_HINT =
            "Aim: mouse or ← → (Shift for fine)     Lock aim: right click     Power: wheel or ↑ ↓     "
                    + "Shoot: click or Space";

    private final GameSession session;
    private final GameCanvas gameCanvas;
    private final PlayerCard playerOne = new PlayerCard(Player.ONE.label());
    private final PlayerCard playerTwo = new PlayerCard(Player.TWO.label());
    private final Label statusLabel = new Label();
    private final HBox playerRow = new HBox(24);
    private final PowerMeter powerMeter;
    private final StackPane gameOverOverlay = new StackPane();
    private final Label winnerLabel = new Label();
    private final Label reasonLabel = new Label();
    private final Button playAgainButton = new Button("Play again");
    private final AnimationTimer timer;
    private boolean wideLayout;

    public GameScreen(GameSession session, Runnable onNewMatch, Runnable onMenu) {
        this.session = session;
        this.gameCanvas = new GameCanvas(session);
        this.powerMeter = new PowerMeter(session::setPower);
        this.timer = createTimer();

        setPadding(new Insets(28));
        setCenter(new StackPane(gameCanvas, buildGameOverOverlay(onNewMatch, onMenu)));
        setBottom(buildBottomBar(onNewMatch, onMenu));
        configureTopBar();
        widthProperty().addListener((observable, before, width) -> {
            boolean wide = width.doubleValue() >= WIDE_LAYOUT_MIN_WIDTH;
            if (wide != wideLayout) {
                arrangeTopBar(wide);
            }
        });
        arrangeTopBar(true);
    }

    public void start() {
        timer.start();
        Platform.runLater(gameCanvas::requestFocus);
    }

    public void stop() {
        timer.stop();
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

    /** Wide windows put the status between the cards; narrow ones give it its own line beneath them. */
    private void arrangeTopBar(boolean wide) {
        wideLayout = wide;
        playerRow.getChildren().clear();
        if (wide) {
            HBox.setHgrow(statusLabel, Priority.ALWAYS);
            playerRow.getChildren().addAll(playerOne, statusLabel, playerTwo);
            setTop(playerRow);
        } else {
            HBox.setHgrow(statusLabel, Priority.NEVER);
            playerRow.getChildren().addAll(playerOne, spacer(), playerTwo);
            VBox top = new VBox(12, playerRow, statusLabel);
            setTop(top);
        }
    }

    private VBox buildBottomBar(Runnable onNewMatch, Runnable onMenu) {
        Button menuButton = new Button("Menu");
        menuButton.setOnAction(event -> onMenu.run());
        Button newMatch = new Button("New match");
        newMatch.setOnAction(event -> onNewMatch.run());
        Label hint = new Label(CONTROLS_HINT);
        hint.getStyleClass().add("muted");
        hint.setWrapText(true);

        HBox controls = new HBox(12, powerMeter, spacer(), menuButton, newMatch);
        controls.setAlignment(Pos.CENTER_LEFT);
        VBox bar = new VBox(14, controls, hint);
        bar.setPadding(new Insets(20, 0, 0, 0));
        return bar;
    }

    private StackPane buildGameOverOverlay(Runnable onNewMatch, Runnable onMenu) {
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

        gameOverOverlay.getChildren().add(card);
        gameOverOverlay.getStyleClass().add("overlay");
        gameOverOverlay.setVisible(false);
        return gameOverOverlay;
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
                if (previousNanos != 0) {
                    double elapsed = (nowNanos - previousNanos) / NANOS_PER_SECOND;
                    session.advance(Math.min(elapsed, MAX_FRAME_SECONDS));
                }
                previousNanos = nowNanos;
                gameCanvas.draw();
                refreshHud();
            }
        };
    }

    private void refreshHud() {
        playerOne.update(cardViewOf(Player.ONE));
        playerTwo.update(cardViewOf(Player.TWO));
        statusLabel.setText(StatusText.describe(session));
        powerMeter.update(session.power());
        showGameOverIfFinished();
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

    private void showGameOverIfFinished() {
        boolean finished = session.phase() == GamePhase.GAME_OVER;
        if (finished && !gameOverOverlay.isVisible()) {
            winnerLabel.setText(StatusText.winnerHeadline(session));
            reasonLabel.setText(StatusText.gameOverReason(session));
            gameOverOverlay.setVisible(true);
            playAgainButton.requestFocus();
        }
    }
}
