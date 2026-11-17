package com.example.billard.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class MixBusTest {

    private final MixBus bus = new MixBus();

    private static Sample constant(float value, int length) {
        float[] frames = new float[length];
        Arrays.fill(frames, value);
        return new Sample(frames);
    }

    @Test
    void centredVoiceIsEquallyLoudOnBothSides() {
        float[] out = new float[8];
        bus.start(new Voice(constant(0.5f, 100), 1.0, 0, 1.0));

        bus.render(out, 4);

        assertEquals(out[0], out[1], 1e-6);
        assertEquals(0.5 * Math.sqrt(0.5), out[0], 1e-6);
    }

    @Test
    void hardPannedVoiceIsSilentOnTheOtherSide() {
        float[] out = new float[8];
        bus.start(new Voice(constant(0.5f, 100), 1.0, -1, 1.0));

        bus.render(out, 4);

        assertEquals(0.5, out[0], 1e-6);
        assertEquals(0.0, out[1], 1e-6);
    }

    @Test
    void voiceStopsWhenItsSampleRunsOut() {
        float[] out = new float[2 * 64];
        bus.start(new Voice(constant(0.5f, 10), 1.0, 0, 1.0));

        bus.render(out, 64);

        assertEquals(0, bus.playingVoices());
        assertEquals(0.0, out[2 * 20], 1e-6, "nothing is played past the end of the sample");
    }

    @Test
    void fasterRateFinishesSooner() {
        float[] out = new float[2 * 60];
        bus.start(new Voice(constant(0.5f, 100), 1.0, 0, 2.0));

        bus.render(out, 60);

        assertEquals(0, bus.playingVoices(), "100 frames at double speed take 50 output frames");
    }

    @Test
    void manyLoudVoicesNeverExceedFullScale() {
        float[] out = new float[2 * 16];
        for (int i = 0; i < 20; i++) {
            bus.start(new Voice(constant(0.9f, 100), 1.0, 0, 1.0));
        }

        bus.render(out, 16);

        for (float value : out) {
            assertTrue(Math.abs(value) <= 1.0f, "limited to full scale, was " + value);
        }
    }

    @Test
    void limiterLeavesQuietSignalsAlone() {
        assertEquals(0.3f, MixBus.limit(0.3f));
        assertEquals(-0.3f, MixBus.limit(-0.3f));
        assertTrue(MixBus.limit(5f) <= 1f && MixBus.limit(5f) > 0.95f);
        assertTrue(MixBus.limit(0.9f) < 0.9f, "loud signals are bent down");
    }

    @Test
    void oldestVoiceMakesWayWhenTheBusIsFull() {
        for (int i = 0; i < MixBus.MAX_VOICES + 5; i++) {
            bus.start(new Voice(constant(0.1f, 1000), 1.0, 0, 1.0));
        }

        assertEquals(MixBus.MAX_VOICES, bus.playingVoices());
    }
}
