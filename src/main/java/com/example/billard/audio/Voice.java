package com.example.billard.audio;

/** One sample being played: where it is in the sample, how loud it is on each side, and how fast it runs. */
final class Voice {

    private final Sample sample;
    private final float leftGain;
    private final float rightGain;
    private final double rate;
    private double position;

    Voice(Sample sample, double gain, double pan, double rate) {
        double angle = (Math.clamp(pan, -1.0, 1.0) + 1) * Math.PI / 4;
        this.sample = sample;
        this.leftGain = (float) (gain * Math.cos(angle));
        this.rightGain = (float) (gain * Math.sin(angle));
        this.rate = rate;
    }

    /**
     * Adds the next {@code frames} frames of this voice to an interleaved stereo buffer.
     *
     * @return false once the sample has played to its end
     */
    boolean mixInto(float[] stereo, int frames) {
        for (int frame = 0; frame < frames; frame++) {
            int index = (int) position;
            if (index + 1 >= sample.length()) {
                return false;
            }
            float fraction = (float) (position - index);
            float value = sample.at(index) + (sample.at(index + 1) - sample.at(index)) * fraction;
            stereo[2 * frame] += value * leftGain;
            stereo[2 * frame + 1] += value * rightGain;
            position += rate;
        }
        return true;
    }
}
