package com.example.billard.audio;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * Plays sounds through one audio line that stays open for the whole game. A background thread mixes every playing
 * sound into small blocks and keeps the line's short buffer topped up, so a new sound is heard roughly
 * {@value #LINE_BUFFER_FRAMES} frames (about 35 ms) after {@link #play} is called, however many are already playing.
 */
public final class AudioMixer implements SoundOutput {

    private static final Logger LOG = Logger.getLogger(AudioMixer.class.getName());
    private static final int CHANNELS = 2;
    private static final int BYTES_PER_FRAME = 2 * CHANNELS;
    private static final int BLOCK_FRAMES = 256;
    private static final int LINE_BUFFER_FRAMES = 1536;
    private static final float MASTER_GAIN = 0.8f;
    private static final long CLOSE_WAIT_MILLIS = 500;

    private final SourceDataLine line;
    private final Queue<Voice> starting = new ConcurrentLinkedQueue<>();
    private final MixBus bus = new MixBus();
    private final Thread worker;
    private volatile boolean running = true;

    private AudioMixer(SourceDataLine line) {
        this.line = line;
        this.worker = new Thread(this::pump, "audio-mixer");
        worker.setDaemon(true);
        worker.setPriority(Thread.MAX_PRIORITY);
        worker.start();
    }

    /** Opens the default audio device, or returns {@link SoundOutput#SILENT} if there is none. */
    public static SoundOutput openDefault() {
        AudioFormat format = new AudioFormat(Sample.RATE, 16, CHANNELS, true, false);
        try {
            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.open(format, LINE_BUFFER_FRAMES * BYTES_PER_FRAME);
            line.start();
            return new AudioMixer(line);
        } catch (LineUnavailableException | IllegalArgumentException | SecurityException noDevice) {
            LOG.log(Level.WARNING, "No audio output available; the game will be silent", noDevice);
            return SILENT;
        }
    }

    @Override
    public void play(Sample sample, double gain, double pan, double rate) {
        if (running) {
            starting.add(new Voice(sample, gain * MASTER_GAIN, pan, rate));
        }
    }

    @Override
    public void close() {
        running = false;
        try {
            worker.join(CLOSE_WAIT_MILLIS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        line.stop();
        line.close();
    }

    /** Runs on the mixer thread; {@code line.write} blocks until there is room, which paces the loop. */
    private void pump() {
        float[] mix = new float[BLOCK_FRAMES * CHANNELS];
        byte[] pcm = new byte[BLOCK_FRAMES * BYTES_PER_FRAME];
        while (running) {
            for (Voice voice = starting.poll(); voice != null; voice = starting.poll()) {
                bus.start(voice);
            }
            bus.render(mix, BLOCK_FRAMES);
            toPcm(mix, pcm);
            line.write(pcm, 0, pcm.length);
        }
    }

    private static void toPcm(float[] mix, byte[] pcm) {
        for (int i = 0; i < mix.length; i++) {
            int value = Math.round(mix[i] * Short.MAX_VALUE);
            pcm[2 * i] = (byte) value;
            pcm[2 * i + 1] = (byte) (value >> 8);
        }
    }
}
