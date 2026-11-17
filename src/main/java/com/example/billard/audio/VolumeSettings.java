package com.example.billard.audio;

import java.util.prefs.Preferences;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

/**
 * The player's volume choices for sound effects and music, each from 0 (off) to 1 (full). They are saved as soon as
 * they change and come back the next time the game starts.
 */
public final class VolumeSettings {

    public static final double DEFAULT_EFFECTS = 1.0;
    public static final double DEFAULT_MUSIC = 0.6;

    static final String EFFECTS_KEY = "effectsVolume";
    static final String MUSIC_KEY = "musicVolume";

    private final DoubleProperty effects;
    private final DoubleProperty music;
    private final DoubleBinding musicGain;

    /**
     * Reads the saved choices from {@code store} (defaults for anything missing or unusable) and saves every later
     * change back to it.
     */
    public VolumeSettings(Preferences store) {
        this.effects = savedLevel(store, EFFECTS_KEY, DEFAULT_EFFECTS);
        this.music = savedLevel(store, MUSIC_KEY, DEFAULT_MUSIC);
        this.musicGain = Bindings.createDoubleBinding(() -> gainOf(music.get()), music);
    }

    /** Sound effects level, 0 to 1, as the player set it. */
    public DoubleProperty effects() {
        return effects;
    }

    /** Music level, 0 to 1, as the player set it. */
    public DoubleProperty music() {
        return music;
    }

    /** The factor to multiply effect sounds by. */
    public double effectsGain() {
        return gainOf(effects.get());
    }

    /** The factor to play music at; follows the music level. */
    public DoubleBinding musicGain() {
        return musicGain;
    }

    /**
     * Hearing is logarithmic, so a straight line from level to amplitude crams all the audible change into the
     * bottom of the slider. Squaring spreads it out: half way is a quarter of the amplitude, about 12 dB down.
     */
    static double gainOf(double level) {
        return level * level;
    }

    private static DoubleProperty savedLevel(Preferences store, String key, double fallback) {
        DoubleProperty level = new SimpleDoubleProperty(usableLevel(store.getDouble(key, fallback), fallback));
        level.addListener((observable, before, now) -> {
            double usable = usableLevel(now.doubleValue(), fallback);
            if (usable != now.doubleValue()) {
                level.set(usable);
            } else {
                store.putDouble(key, usable);
            }
        });
        return level;
    }

    private static double usableLevel(double level, double fallback) {
        if (Double.isNaN(level)) {
            return fallback;
        }
        return Math.clamp(level, 0.0, 1.0);
    }
}
