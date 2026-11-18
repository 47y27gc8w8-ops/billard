package com.example.billard.ui;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Walnut as a seamless tile with the grain running along its width: warped growth rings, dark pore streaks and a
 * fine per-pixel grain, between a light and a dark wood tone.
 */
final class WoodTexture {

    static final int LENGTH = 1024;
    static final int DEPTH = 256;

    private static final long SEED = 0x0A1_7A1L;
    private static final int RINGS_ACROSS_TILE = 22;
    private static final double WARP = 16;
    private static final double FINE_WARP = 3;
    private static final double PORE_THRESHOLD = 0.6;
    private static final double GRAIN_NOISE = 0.07;

    private static Image along;
    private static Image across;

    private WoodTexture() {
    }

    /** Wood with the grain running left to right, generated on first use. */
    static Image alongTile() {
        if (along == null) {
            along = generate(Palette.WOOD_LIGHT, Palette.WOOD_DARK, SEED).toImage();
        }
        return along;
    }

    /** Wood with the grain running top to bottom, for the short rails. */
    static Image acrossTile() {
        if (across == null) {
            across = generate(Palette.WOOD_LIGHT, Palette.WOOD_DARK, SEED).transposed().toImage();
        }
        return across;
    }

    static PixelTile generate(Color light, Color dark, long seed) {
        ValueNoise warp = new ValueNoise(seed, LENGTH / 128, DEPTH / 32);
        ValueNoise fineWarp = new ValueNoise(seed + 1, LENGTH / 32, DEPTH / 8);
        ValueNoise pores = new ValueNoise(seed + 2, LENGTH / 16, DEPTH);
        ValueNoise grain = new ValueNoise(seed + 3, LENGTH, DEPTH);
        int[] argb = new int[LENGTH * DEPTH];
        for (int y = 0; y < DEPTH; y++) {
            for (int x = 0; x < LENGTH; x++) {
                double bent = y
                        + WARP * (warp.at(x / 128.0, y / 32.0) - 0.5) * 2
                        + FINE_WARP * (fineWarp.at(x / 32.0, y / 8.0) - 0.5) * 2;
                double ring = Math.pow(0.5 + 0.5 * Math.sin(2 * Math.PI * RINGS_ACROSS_TILE * bent / DEPTH), 3);
                double pore = Math.max(0, pores.at(x / 16.0, y) - PORE_THRESHOLD) * 2.5;
                double darkness = Math.min(1, 0.25 + 0.5 * ring + pore);
                double noise = 1 + GRAIN_NOISE * (grain.lattice(x, y) - 0.5);
                argb[y * LENGTH + x] = mix(light, dark, darkness, noise);
            }
        }
        return new PixelTile(LENGTH, DEPTH, argb);
    }

    private static int mix(Color light, Color dark, double darkness, double noise) {
        return PixelTile.packOpaque(
                (light.getRed() + (dark.getRed() - light.getRed()) * darkness) * noise,
                (light.getGreen() + (dark.getGreen() - light.getGreen()) * darkness) * noise,
                (light.getBlue() + (dark.getBlue() - light.getBlue()) * darkness) * noise);
    }
}
