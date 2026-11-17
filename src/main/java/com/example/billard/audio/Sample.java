package com.example.billard.audio;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/** A decoded sound, held as mono samples between -1 and 1 at {@link #RATE} frames per second. */
public final class Sample {

    public static final float RATE = 44_100f;

    private static final int BITS = 16;
    private static final float FULL_SCALE = 32_768f;

    private final float[] frames;

    public Sample(float[] frames) {
        this.frames = frames.clone();
    }

    /** Reads a 16-bit PCM WAV recorded at {@link #RATE}; stereo is mixed down to mono. */
    public static Sample read(InputStream wav) throws IOException {
        try (AudioInputStream source = AudioSystem.getAudioInputStream(new BufferedInputStream(wav))) {
            AudioFormat format = source.getFormat();
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED || format.getSampleSizeInBits() != BITS
                    || format.getSampleRate() != RATE) {
                throw new IOException("Expected 16-bit signed PCM at " + RATE + " Hz, got " + format);
            }
            return new Sample(toMono(source.readAllBytes(), format.getChannels(), format.isBigEndian()));
        } catch (UnsupportedAudioFileException notWav) {
            throw new IOException("Not a readable WAV file", notWav);
        }
    }

    private static float[] toMono(byte[] pcm, int channels, boolean bigEndian) {
        int bytesPerFrame = 2 * channels;
        float[] mono = new float[pcm.length / bytesPerFrame];
        for (int frame = 0; frame < mono.length; frame++) {
            float sum = 0;
            for (int channel = 0; channel < channels; channel++) {
                int offset = frame * bytesPerFrame + 2 * channel;
                sum += sixteenBit(pcm[offset], pcm[offset + 1], bigEndian);
            }
            mono[frame] = sum / channels / FULL_SCALE;
        }
        return mono;
    }

    private static short sixteenBit(byte first, byte second, boolean bigEndian) {
        return bigEndian
                ? (short) ((first << 8) | (second & 0xFF))
                : (short) ((second << 8) | (first & 0xFF));
    }

    public int length() {
        return frames.length;
    }

    float at(int index) {
        return frames[index];
    }
}
