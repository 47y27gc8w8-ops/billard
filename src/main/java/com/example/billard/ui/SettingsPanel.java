package com.example.billard.ui;

import com.example.billard.audio.LoungeMusic;
import com.example.billard.audio.SoundOutput;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Volume sliders for sound effects and music, plus the music credit. Changes apply at once and are saved. Moving
 * the effects slider plays a ball click at the new level so the player can hear it.
 */
final class SettingsPanel extends VBox {

    private static final double SLIDER_WIDTH = 260;
    private static final double TEXT_WIDTH = 400;
    private static final double SLIDER_STEP = 0.05;
    private static final double PREVIEW_GAIN = 0.8;

    private final Slider effectsSlider;

    SettingsPanel(GameAudio audio, Runnable onDone) {
        super(22);
        Label heading = new Label("Settings");
        heading.getStyleClass().add("display-medium");

        effectsSlider = volumeSlider(audio.volumes().effects());
        Slider musicSlider = volumeSlider(audio.volumes().music());
        playClickWhenSet(effectsSlider, audio.effects());

        Label credit = new Label(LoungeMusic.CREDIT);
        credit.getStyleClass().addAll("muted", "fine-print");
        credit.setWrapText(true);
        credit.setPrefWidth(TEXT_WIDTH);
        credit.setMinHeight(Region.USE_PREF_SIZE);

        Button doneButton = new Button("Done");
        doneButton.getStyleClass().add("primary");
        doneButton.setOnAction(event -> onDone.run());

        getChildren().addAll(heading, volumeRow("Sound effects", effectsSlider), volumeRow("Music", musicSlider),
                credit, doneButton);
        setAlignment(Pos.CENTER_LEFT);
    }

    void focusFirstControl() {
        effectsSlider.requestFocus();
    }

    private static Slider volumeSlider(DoubleProperty level) {
        Slider slider = new Slider(0, 1, level.get());
        slider.setBlockIncrement(SLIDER_STEP);
        slider.setPrefWidth(SLIDER_WIDTH);
        slider.valueProperty().bindBidirectional(level);
        return slider;
    }

    private static VBox volumeRow(String name, Slider slider) {
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("rule-name");
        nameLabel.setLabelFor(slider);
        Label percentLabel = new Label();
        percentLabel.setMinWidth(56);
        percentLabel.textProperty().bind(
                Bindings.createStringBinding(() -> percent(slider.getValue()), slider.valueProperty()));
        slider.accessibleTextProperty().bind(Bindings.concat(name, " volume ", percentLabel.textProperty()));

        HBox control = new HBox(14, slider, percentLabel);
        control.setAlignment(Pos.CENTER_LEFT);
        return new VBox(6, nameLabel, control);
    }

    static String percent(double level) {
        return Math.round(level * 100) + "%";
    }

    /**
     * Plays once when a drag ends, and on every click or key step (those set the value without dragging). Only
     * while this slider has focus, so a change made on another screen's panel does not click here too.
     */
    private static void playClickWhenSet(Slider slider, SoundOutput effects) {
        Runnable click = () -> {
            if (slider.isFocused()) {
                SoundBank.standard().hardClacks().stream().findFirst()
                        .ifPresent(sample -> effects.play(sample, PREVIEW_GAIN, 0, 1));
            }
        };
        slider.valueChangingProperty().addListener((observable, wasChanging, changing) -> {
            if (!changing) {
                click.run();
            }
        });
        slider.valueProperty().addListener((observable, before, now) -> {
            if (!slider.isValueChanging()) {
                click.run();
            }
        });
    }
}
