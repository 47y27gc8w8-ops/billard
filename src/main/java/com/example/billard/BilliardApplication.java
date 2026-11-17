package com.example.billard;

import com.example.billard.audio.AudioMixer;
import com.example.billard.audio.LoungeMusic;
import com.example.billard.audio.MusicPlayer;
import com.example.billard.audio.ScaledOutput;
import com.example.billard.audio.SoundOutput;
import com.example.billard.audio.VolumeSettings;
import com.example.billard.model.GameSession;
import com.example.billard.model.Table;
import com.example.billard.ui.GameAudio;
import com.example.billard.ui.GameScreen;
import com.example.billard.ui.MenuScreen;
import com.example.billard.ui.Theme;
import java.util.prefs.Preferences;
import java.util.random.RandomGenerator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

/** Application shell: owns the window and the sound, and switches between the menu and a running match. */
public final class BilliardApplication extends Application {

    private static final double START_WIDTH = 1280;
    private static final double START_HEIGHT = 800;
    private static final double MIN_WIDTH = 700;
    private static final double MIN_HEIGHT = 560;
    private static final double SCREEN_FILL = 0.94;

    private final StackPane screenHost = new StackPane();
    private SoundOutput mixer = SoundOutput.SILENT;
    private MusicPlayer music;
    private GameAudio audio;
    private GameScreen activeGame;

    @Override
    public void start(Stage stage) {
        VolumeSettings volumes = new VolumeSettings(Preferences.userNodeForPackage(BilliardApplication.class));
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
        stage.setMinWidth(Math.min(MIN_WIDTH, width));
        stage.setMinHeight(Math.min(MIN_HEIGHT, height));
        stage.setScene(scene);
        stage.show();
        music.start();
    }

    @Override
    public void stop() {
        stopActiveGame();
        if (music != null) {
            music.close();
        }
        mixer.close();
    }

    private void showMenu() {
        stopActiveGame();
        screenHost.getChildren().setAll(new MenuScreen(audio, this::startMatch, Platform::exit));
    }

    private void startMatch() {
        stopActiveGame();
        activeGame = new GameScreen(new GameSession(Table.standard()), audio, this::startMatch, this::showMenu);
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
