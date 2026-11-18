package com.example.billard.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class AppIconTest {

    private static final int OPAQUE = 0xFF;

    private static BufferedImage read(int size) throws IOException {
        try (InputStream png = AppIcon.class.getResourceAsStream(AppIcon.resourceName(size))) {
            assertNotNull(png, "missing " + AppIcon.resourceName(size));
            return ImageIO.read(png);
        }
    }

    private static int alpha(BufferedImage image, int x, int y) {
        return image.getRGB(x, y) >>> 24;
    }

    @Test
    void everySizeIsDrawnAtThatSize() throws IOException {
        for (int size : AppIcon.SIZES) {
            BufferedImage image = read(size);
            assertEquals(size, image.getWidth(), "width of " + size);
            assertEquals(size, image.getHeight(), "height of " + size);
        }
    }

    @Test
    void cornersAreTransparentAndCentreIsSolid() throws IOException {
        for (int size : AppIcon.SIZES) {
            BufferedImage image = read(size);
            assertEquals(0, alpha(image, 0, 0), "top-left corner of " + size);
            assertEquals(0, alpha(image, size - 1, size - 1), "bottom-right corner of " + size);
            assertEquals(OPAQUE, alpha(image, size / 2, size / 2), "centre of " + size);
        }
    }
}
