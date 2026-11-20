package com.example.billard;

import com.example.billard.audio.AudioMixer;
import com.example.billard.audio.LoungeMusic;
import com.example.billard.audio.MusicPlayer;
import com.example.billard.audio.ScaledOutput;
import com.example.billard.audio.SoundOutput;
import com.example.billard.audio.VolumeSettings;
import com.example.billard.model.GameSession;
import com.example.billard.model.Table;
import com.example.billard.net.OnlineLink;
import com.example.billard.ui.AppIcon;
import com.example.billard.ui.GameAudio;
import com.example.billard.ui.GameScreen;
import com.example.billard.ui.MatchMode;
import com.example.billard.ui.MenuScreen;
import com.example.billard.ui.OnlineMatchScreen;
import com.example.billard.ui.OnlinePreferences;
import com.example.billard.ui.Theme;
import java.util.prefs.Preferences;
import java.util.random.RandomGenerator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Application shell: owns the window and the sound, and switches between the menu and a running match against the
 * bot, between two players, or online. An online session's link lives on across rematches until the player leaves.
 */
public final class BilliardApplication extends Application {

    private static final double START_WIDTH = 1280;
    private static final double START_HEIGHT = 800;
    private static final double MIN_WIDTH = 700;
    private static final double MIN_HEIGHT = 560;
    private static final double SCREEN_FILL = 0.94;
    private static final KeyCombination FULL_SCREEN_TOGGLE = new KeyCodeCombination(KeyCode.F);

    private final StackPane screenHost = new StackPane();
    private SoundOutput mixer = SoundOutput.SILENT;
    private MusicPlayer music;
    private GameAudio audio;
    private OnlinePreferences onlinePreferences;
    private GameScreen activeGame;
    private OnlineLink activeLink;

    @Override
    public void start(Stage stage) {
        Preferences preferences = Preferences.userNodeForPackage(BilliardApplication.class);
        VolumeSettings volumes = new VolumeSettings(preferences);
        onlinePreferences = new OnlinePreferences(preferences);
        mixer = AudioMixer.openDefault();
        audio = new GameAudio(new ScaledOutput(mixer, volumes::effectsGain), volumes);
        music = new MusicPlayer(LoungeMusic.TRACKS, volumes.musicGain(), RandomGenerator.getDefault());

        Rectangle2D usable = Screen.getPrimary().getVisualBounds();
        double width = Math.min(START_WIDTH, usable.getWidth() * SCREEN_FILL);
        double height = Math.min(START_HEIGHT, usable.getHeight() * SCREEN_FILL);
        Scene scene = new Scene(screenHost, width, height);
        Theme.applyTo(scene, screenHost);
        showMenu();

        stage.setTitle("Billard");
        AppIcon.applyTo(stage);
        stage.setMinWidth(Math.min(MIN_WIDTH, width));
        stage.setMinHeight(Math.min(MIN_HEIGHT, height));
        stage.setScene(scene);
        enableFullScreenToggle(stage, scene);
        stage.show();
        music.start();
    }

    @Override
    public void stop() {
        stopActiveGame();
        leaveOnlineSession();
        if (music != null) {
            music.close();
        }
        mixer.close();
    }

    /**
     * F switches full screen on and off from any screen. Escape is left to the game's own overlays, so it no
     * longer leaves full screen as well.
     */
    private static void enableFullScreenToggle(Stage stage, Scene scene) {
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setFullScreenExitHint("Press F to leave full screen");
        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (FULL_SCREEN_TOGGLE.match(event)) {
                stage.setFullScreen(!stage.isFullScreen());
                event.consume();
            }
        });
    }

    private void showMenu() {
        stopActiveGame();
        leaveOnlineSession();
        MenuScreen.Actions actions = new MenuScreen.Actions(this::startMatch, link -> startOnlineMatch(link, 0),
                Platform::exit);
        screenHost.getChildren().setAll(new MenuScreen(audio, onlinePreferences, actions));
    }

    private void startMatch(MatchMode mode) {
        stopActiveGame();
        GameSession session = new GameSession(Table.standard());
        activeGame = GameScreen.offline(session, mode, audio,
                new GameScreen.Navigation(() -> startMatch(mode), this::showMenu));
        screenHost.getChildren().setAll(activeGame);
        activeGame.start();
    }

    /** Starts match {@code matchNumber} of the online session on {@code link}, counting from 0. */
    private void startOnlineMatch(OnlineLink link, int matchNumber) {
        if (link.state() == OnlineLink.State.CLOSED) {
            return;
        }
        stopActiveGame();
        activeLink = link;
        activeGame = OnlineMatchScreen.open(link, matchNumber, audio, () -> startOnlineMatch(link, matchNumber + 1),
                this::showMenu);
        screenHost.getChildren().setAll(activeGame);
        activeGame.start();
    }

    /** Tells the other player this one has left, if an online session is running, and closes the connection. */
    private void leaveOnlineSession() {
        if (activeLink != null) {
            activeLink.close();
            activeLink = null;
        }
    }

    private void stopActiveGame() {
        if (activeGame != null) {
            activeGame.stop();
            activeGame = null;
        }
    }
}
