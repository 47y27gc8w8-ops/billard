package com.example.billard.audio;

import java.util.function.DoubleSupplier;

/** Passes sounds on to another output, scaled by a volume read at the moment each sound starts. */
public final class ScaledOutput implements SoundOutput {

    private final SoundOutput target;
    private final DoubleSupplier volume;

    public ScaledOutput(SoundOutput target, DoubleSupplier volume) {
        this.target = target;
        this.volume = volume;
    }

    @Override
    public void play(Sample sample, double gain, double pan, double rate) {
        double scaledGain = gain * volume.getAsDouble();
        if (scaledGain > 0) {
            target.play(sample, scaledGain, pan, rate);
        }
    }

    @Override
    public void close() {
        target.close();
    }
}
