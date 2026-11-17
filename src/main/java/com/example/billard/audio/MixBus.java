package com.example.billard.audio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Sums all playing voices into one stereo block. Loud pile-ups are rounded off by a soft limiter instead of
 * clipping, and when too many voices play at once the oldest one makes way.
 */
final class MixBus {

    static final int MAX_VOICES = 32;

    private static final float LIMITER_KNEE = 0.7f;

    private final List<Voice> voices = new ArrayList<>();

    void start(Voice voice) {
        if (voices.size() == MAX_VOICES) {
            voices.remove(0);
        }
        voices.add(voice);
    }

    int playingVoices() {
        return voices.size();
    }

    /** Fills the first {@code frames} interleaved stereo frames of {@code stereo} with the next block. */
    void render(float[] stereo, int frames) {
        Arrays.fill(stereo, 0, 2 * frames, 0f);
        voices.removeIf(voice -> !voice.mixInto(stereo, frames));
        for (int i = 0; i < 2 * frames; i++) {
            stereo[i] = limit(stereo[i]);
        }
    }

    /** Leaves quiet signals untouched and bends loud ones smoothly towards full scale, never past it. */
    static float limit(float value) {
        float magnitude = Math.abs(value);
        if (magnitude <= LIMITER_KNEE) {
            return value;
        }
        float headroom = 1 - LIMITER_KNEE;
        float bent = LIMITER_KNEE + headroom * (float) Math.tanh((magnitude - LIMITER_KNEE) / headroom);
        return Math.copySign(bent, value);
    }
}
