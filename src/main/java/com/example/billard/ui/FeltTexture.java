package com.example.billard.ui;

import java.util.SplittableRandom;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Billiard cloth as a seamless tile: per-pixel fuzz, short streaks along the nap, loose light fibres and a soft
 * mottling, all around one base colour. The tile is drawn one texel per screen pixel, so the fuzz stays crisp at any
 * window size.
 */
final class FeltTexture {

    static final int SIZE = 512;

    private static final long SEED = 0x5EED_F017L;
    private static final double FUZZ = 0.26;
    private static final double STREAK = 0.12;
    private static final double MOTTLE = 0.10;
    private static final double FIBRE_LIGHT = 0.26;
    private static final double FIBRES_PER_PIXEL = 0.008;
    private static final double DARK_FIBRE_SHARE = 0.35;
    private static final int MIN_FIBRE_LENGTH = 3;
    private static final int MAX_FIBRE_LENGTH = 10;

    private static Image clothTile;

    private FeltTexture() {
    }

    /** The cloth tile in the palette's cloth colour, generated on first use. */
    static Image clothTile() {
        if (clothTile == null) {
            clothTile = generate(Palette.CLOTH, SEED).toImage();
        }
        return clothTile;
    }

    static PixelTile generate(Color base, long seed) {
        double[] light = new double[SIZE * SIZE];
        addSurface(light, seed);
        addFibres(light, new SplittableRandom(seed));
        int[] argb = new int[light.length];
        for (int i = 0; i < light.length; i++) {
            argb[i] = PixelTile.shade(base, light[i]);
        }
        return new PixelTile(SIZE, SIZE, argb);
    }

    private static void addSurface(double[] light, long seed) {
        ValueNoise fuzz = new ValueNoise(seed, SIZE, SIZE);
        ValueNoise streaks = new ValueNoise(seed + 1, SIZE / 8, SIZE / 2);
        ValueNoise coarse = new ValueNoise(seed + 2, 4, 4);
        ValueNoise fine = new ValueNoise(seed + 3, 16, 16);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double mottle = 0.65 * coarse.at(x / 128.0, y / 128.0) + 0.35 * fine.at(x / 32.0, y / 32.0);
                light[y * SIZE + x] = 1
                        + FUZZ * (fuzz.lattice(x, y) - 0.5)
                        + STREAK * (streaks.at(x / 8.0, y / 2.0) - 0.5)
                        + MOTTLE * (mottle - 0.5);
            }
        }
    }

    /**
     * Short strokes at random angles, most lighter and some darker, wrapped at the edges so the tile stays
     * seamless.
     */
    private static void addFibres(double[] light, SplittableRandom random) {
        int count = (int) (SIZE * SIZE * FIBRES_PER_PIXEL);
        for (int i = 0; i < count; i++) {
            double x = random.nextDouble(SIZE);
            double y = random.nextDouble(SIZE);
            double angle = random.nextDouble(Math.PI);
            int length = random.nextInt(MIN_FIBRE_LENGTH, MAX_FIBRE_LENGTH + 1);
            double strength = FIBRE_LIGHT * (0.5 + random.nextDouble())
                    * (random.nextDouble() < DARK_FIBRE_SHARE ? -0.7 : 1);
            for (int step = 0; step < length; step++) {
                int px = Math.floorMod((int) (x + Math.cos(angle) * step), SIZE);
                int py = Math.floorMod((int) (y + Math.sin(angle) * step), SIZE);
                light[py * SIZE + px] += strength * (1 - (double) step / length);
            }
        }
    }
}
