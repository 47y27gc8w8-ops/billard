package com.example.billard.audio;

/** Somewhere sounds can be played. Calls return at once; the sound starts within a few milliseconds. */
public interface SoundOutput extends AutoCloseable {

    /** Plays nothing; used when there is no audio device. */
    SoundOutput SILENT = (sample, gain, pan, rate) -> {
    };

    /**
     * @param gain 0 is silent, 1 is the sample's recorded level
     * @param pan  -1 is fully left, 0 centre, 1 fully right
     * @param rate playback speed; 1.05 plays 5% faster and higher
     */
    void play(Sample sample, double gain, double pan, double rate);

    /** Releases the audio device. Playing after closing does nothing. */
    @Override
    default void close() {
    }
}
