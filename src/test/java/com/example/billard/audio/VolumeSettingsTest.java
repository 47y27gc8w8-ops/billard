package com.example.billard.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.prefs.Preferences;
import org.junit.jupiter.api.Test;

class VolumeSettingsTest {

    private final Preferences store = new InMemoryPreferences();

    @Test
    void startsAtTheDefaultsWhenNothingIsSaved() {
        VolumeSettings settings = new VolumeSettings(store);

        assertEquals(VolumeSettings.DEFAULT_EFFECTS, settings.effects().get());
        assertEquals(VolumeSettings.DEFAULT_MUSIC, settings.music().get());
    }

    @Test
    void changesAreSavedAndComeBackNextTime() {
        VolumeSettings settings = new VolumeSettings(store);
        settings.effects().set(0.3);
        settings.music().set(0.85);

        VolumeSettings nextLaunch = new VolumeSettings(store);

        assertEquals(0.3, nextLaunch.effects().get());
        assertEquals(0.85, nextLaunch.music().get());
    }

    @Test
    void unusableSavedValuesFallBackToSafeOnes() {
        store.put(VolumeSettings.EFFECTS_KEY, "loud");
        store.putDouble(VolumeSettings.MUSIC_KEY, 7.5);

        VolumeSettings settings = new VolumeSettings(store);

        assertEquals(VolumeSettings.DEFAULT_EFFECTS, settings.effects().get(), "not a number: default");
        assertEquals(1.0, settings.music().get(), "too high: full volume, not louder");
    }

    @Test
    void levelsOutsideZeroToOneAreClampedBeforeSaving() {
        VolumeSettings settings = new VolumeSettings(store);

        settings.music().set(-0.4);
        settings.effects().set(Double.NaN);

        assertEquals(0.0, settings.music().get());
        assertEquals(0.0, store.getDouble(VolumeSettings.MUSIC_KEY, -1));
        assertEquals(VolumeSettings.DEFAULT_EFFECTS, settings.effects().get());
    }

    @Test
    void gainFollowsTheLevelOnALoudnessCurve() {
        VolumeSettings settings = new VolumeSettings(store);

        settings.music().set(0.5);
        settings.effects().set(0);

        assertEquals(0.25, settings.musicGain().get(), 1e-9, "half way is a quarter of the amplitude");
        assertEquals(0.0, settings.effectsGain(), "zero is silent");
        assertEquals(1.0, VolumeSettings.gainOf(1.0), "full is the recorded level");
    }
}
