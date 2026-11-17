package com.example.billard.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScaledOutputTest {

    private final Sample sample = new Sample(new float[] {0.5f});
    private final List<Double> playedGains = new ArrayList<>();
    private final SoundOutput recorder = (played, gain, pan, rate) -> playedGains.add(gain);
    private double volume = 1.0;
    private final ScaledOutput output = new ScaledOutput(recorder, () -> volume);

    @Test
    void scalesEachSoundByTheCurrentVolume() {
        volume = 0.5;
        output.play(sample, 0.8, 0, 1);
        volume = 0.25;
        output.play(sample, 0.8, 0, 1);

        assertEquals(List.of(0.4, 0.2), playedGains);
    }

    @Test
    void playsNothingAtZeroVolume() {
        volume = 0;

        output.play(sample, 1.0, 0, 1);

        assertTrue(playedGains.isEmpty());
    }

    @Test
    void closingClosesTheRealOutput() {
        boolean[] closed = {false};
        SoundOutput device = new SoundOutput() {
            @Override
            public void play(Sample played, double gain, double pan, double rate) {
            }

            @Override
            public void close() {
                closed[0] = true;
            }
        };

        new ScaledOutput(device, () -> 1).close();

        assertTrue(closed[0]);
    }
}
