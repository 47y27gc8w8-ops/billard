package com.example.billard.ui;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/** A generated texture as packed ARGB pixels, kept apart from JavaFX images so it can be built and tested anywhere. */
record PixelTile(int width, int height, int[] argb) {

    PixelTile {
        if (argb.length != width * height) {
            throw new IllegalArgumentException(
                    "Expected " + width * height + " pixels for " + width + " x " + height + ", got " + argb.length);
        }
    }

    int pixel(int x, int y) {
        return argb[y * width + x];
    }

    /** The same texture turned a quarter, so grain that ran across now runs down. */
    PixelTile transposed() {
        int[] turned = new int[argb.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                turned[x * height + y] = argb[y * width + x];
            }
        }
        return new PixelTile(height, width, turned);
    }

    Image toImage() {
        WritableImage image = new WritableImage(width, height);
        image.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), argb, 0, width);
        return image;
    }

    /** Packs a colour scaled by {@code light} into an opaque ARGB pixel. */
    static int shade(Color color, double light) {
        return packOpaque(color.getRed() * light, color.getGreen() * light, color.getBlue() * light);
    }

    static int packOpaque(double red, double green, double blue) {
        return 0xFF000000 | channel(red) << 16 | channel(green) << 8 | channel(blue);
    }

    private static int channel(double value) {
        return (int) Math.round(Math.max(0, Math.min(1, value)) * 255);
    }
}
