package com.example.billard.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

class TextureTest {

    private static final Color BASE = Color.web("#0F6E4F");

    @Test
    void noiseStaysBetweenZeroAndOne() {
        ValueNoise noise = new ValueNoise(7, 8, 8);
        for (double x = -3; x < 11; x += 0.37) {
            for (double y = -3; y < 11; y += 0.41) {
                double value = noise.at(x, y);
                assertTrue(value >= 0 && value <= 1, "noise out of range at " + x + ", " + y + ": " + value);
            }
        }
    }

    @Test
    void noiseRepeatsEveryPeriodSoTilesHaveNoSeam() {
        ValueNoise noise = new ValueNoise(7, 4, 6);
        for (double x = 0; x < 4; x += 0.29) {
            for (double y = 0; y < 6; y += 0.31) {
                assertEquals(noise.at(x, y), noise.at(x + 4, y), 1e-12);
                assertEquals(noise.at(x, y), noise.at(x, y + 6), 1e-12);
            }
        }
    }

    @Test
    void noiseRejectsAnEmptyPeriod() {
        assertThrows(IllegalArgumentException.class, () -> new ValueNoise(1, 0, 4));
    }

    @Test
    void feltIsTheSameEveryTimeForTheSameSeed() {
        assertArrayEquals(FeltTexture.generate(BASE, 3).argb(), FeltTexture.generate(BASE, 3).argb());
    }

    @Test
    void feltAveragesCloseToItsBaseColour() {
        PixelTile felt = FeltTexture.generate(BASE, 3);
        double green = 0;
        for (int pixel : felt.argb()) {
            green += ((pixel >> 8) & 0xFF) / 255.0;
        }
        assertEquals(BASE.getGreen(), green / felt.argb().length, 0.03);
    }

    @Test
    void feltIsNotFlat() {
        PixelTile felt = FeltTexture.generate(BASE, 3);
        long distinct = java.util.Arrays.stream(felt.argb()).distinct().count();
        assertTrue(distinct > 100, "expected a textured cloth, got " + distinct + " distinct colours");
    }

    @Test
    void woodTurnedAQuarterSwapsItsSidesAndKeepsEveryPixel() {
        PixelTile wood = WoodTexture.generate(Color.web("#7B4A26"), Color.web("#2E170A"), 5);
        PixelTile turned = wood.transposed();

        assertEquals(wood.width(), turned.height());
        assertEquals(wood.height(), turned.width());
        assertEquals(wood.pixel(100, 20), turned.pixel(20, 100));
    }

    @Test
    void tileRejectsTheWrongNumberOfPixels() {
        assertThrows(IllegalArgumentException.class, () -> new PixelTile(4, 4, new int[15]));
    }
}
