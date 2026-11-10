package com.example.billard;

import com.example.billard.model.GameSession;
import com.example.billard.model.Table;
import com.example.billard.ui.GameScreen;
import com.example.billard.ui.MenuScreen;
import com.example.billard.ui.Theme;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

/** Application shell: owns the window and switches between the menu and a running match. */
public final class BilliardApplication extends Application {

    private static final double START_WIDTH = 1280;
    private static final double START_HEIGHT = 800;
    private static final double MIN_WIDTH = 700;
    private static final double MIN_HEIGHT = 560;
    private static final double SCREEN_FILL = 0.94;

    private final StackPane screenHost = new StackPane();
    private GameScreen activeGame;

    @Override
    public void start(Stage stage) {
        Rectangle2D usable = Screen.getPrimary().getVisualBounds();
        double width = Math.min(START_WIDTH, usable.getWidth() * SCREEN_FILL);
        double height = Math.min(START_HEIGHT, usable.getHeight() * SCREEN_FILL);
        Scene scene = new Scene(screenHost, width, height);
        Theme.applyTo(scene, screenHost);
        showMenu();

        stage.setTitle("Billard");
        stage.setMinWidth(Math.min(MIN_WIDTH, width));
        stage.setMinHeight(Math.min(MIN_HEIGHT, height));
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        stopActiveGame();
    }

    private void showMenu() {
        stopActiveGame();
        screenHost.getChildren().setAll(new MenuScreen(this::startMatch, Platform::exit));
    }

    private void startMatch() {
        stopActiveGame();
        activeGame = new GameScreen(new GameSession(Table.standard()), this::startMatch, this::showMenu);
        screenHost.getChildren().setAll(activeGame);
        activeGame.start();
    }

    private void stopActiveGame() {
        if (activeGame != null) {
            activeGame.stop();
            activeGame = null;
        }
    }
}
