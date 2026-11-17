package com.example.billard.ui;

import com.example.billard.audio.Sample;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The game's recorded sounds, several takes per kind so repeated hits do not sound identical. Files are named
 * {@code <kind>_1.wav}, {@code <kind>_2.wav} and so on; a kind whose files are missing stays empty and silent.
 */
record SoundBank(
        List<Sample> cueStrikes,
        List<Sample> softClacks,
        List<Sample> hardClacks,
        List<Sample> cushionThuds,
        List<Sample> pocketDrops,
        List<Sample> rackBreaks,
        List<Sample> fanfares) {

    private static final Logger LOG = Logger.getLogger(SoundBank.class.getName());
    private static final String FOLDER = "/com/example/billard/sounds/";

    private static final class Standard {
        private static final SoundBank BANK = new SoundBank(
                takes("cue"), takes("ball_soft"), takes("ball_hard"), takes("cushion"), takes("pot"),
                takes("break"), takes("success"));
    }

    /** The sounds shipped with the game, loaded once on first use. */
    static SoundBank standard() {
        return Standard.BANK;
    }

    private static List<Sample> takes(String kind) {
        List<Sample> takes = new ArrayList<>();
        for (int take = 1; ; take++) {
            String file = kind + "_" + take + ".wav";
            try (InputStream wav = SoundBank.class.getResourceAsStream(FOLDER + file)) {
                if (wav == null) {
                    break;
                }
                takes.add(Sample.read(wav));
            } catch (IOException unreadable) {
                LOG.log(Level.WARNING, "Skipping unreadable sound " + file, unreadable);
            }
        }
        if (takes.isEmpty()) {
            LOG.warning("No sounds found for '" + kind + "'; it will be silent");
        }
        return List.copyOf(takes);
    }
}
