package com.example.billard.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.junit.jupiter.api.Test;

class SampleTest {

    private static byte[] wav(float rate, int channels, short... values) throws IOException {
        byte[] pcm = new byte[values.length * 2];
        for (int i = 0; i < values.length; i++) {
            pcm[2 * i] = (byte) values[i];
            pcm[2 * i + 1] = (byte) (values[i] >> 8);
        }
        AudioFormat format = new AudioFormat(rate, 16, channels, true, false);
        AudioInputStream stream =
                new AudioInputStream(new ByteArrayInputStream(pcm), format, values.length / channels);
        ByteArrayOutputStream file = new ByteArrayOutputStream();
        AudioSystem.write(stream, AudioFileFormat.Type.WAVE, file);
        return file.toByteArray();
    }

    @Test
    void readsMonoWav() throws IOException {
        Sample sample = Sample.read(new ByteArrayInputStream(wav(44_100f, 1, (short) 16384, (short) -16384)));

        assertEquals(2, sample.length());
        assertEquals(0.5f, sample.at(0), 1e-4);
        assertEquals(-0.5f, sample.at(1), 1e-4);
    }

    @Test
    void mixesStereoDownToMono() throws IOException {
        Sample sample = Sample.read(new ByteArrayInputStream(wav(44_100f, 2, (short) 16384, (short) 0)));

        assertEquals(1, sample.length());
        assertEquals(0.25f, sample.at(0), 1e-4);
    }

    @Test
    void rejectsOtherSampleRates() throws IOException {
        byte[] wrongRate = wav(22_050f, 1, (short) 0, (short) 0);

        assertThrows(IOException.class, () -> Sample.read(new ByteArrayInputStream(wrongRate)));
    }
}
