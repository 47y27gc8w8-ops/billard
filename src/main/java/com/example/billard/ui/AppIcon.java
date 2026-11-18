package com.example.billard.ui;

import java.net.URL;
import java.util.List;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * The 8-ball logo. Each size is drawn for its size (see {@code logo/} in the project root), so the window
 * title, taskbar and task switcher each pick a sharp one instead of scaling a single image.
 */
public final class AppIcon {

    static final List<Integer> SIZES = List.of(16, 24, 32, 48, 64, 128, 256, 512);
    private static final int LARGEST = 512;
    private static final String FOLDER = "/com/example/billard/icon/";

    private AppIcon() {
    }

    /** Gives the window every size of the logo; the platform chooses which ones to show where. */
    public static void applyTo(Stage stage) {
        stage.getIcons().setAll(SIZES.stream().map(AppIcon::load).toList());
    }

    /** The largest logo, smoothly scaled to {@code size} pixels, for showing inside the app. */
    public static Image scaledTo(double size) {
        return new Image(location(LARGEST).toExternalForm(), size, size, true, true);
    }

    static String resourceName(int size) {
        return FOLDER + "billard-" + size + ".png";
    }

    private static Image load(int size) {
        return new Image(location(size).toExternalForm());
    }

    private static URL location(int size) {
        URL location = AppIcon.class.getResource(resourceName(size));
        if (location == null) {
            throw new IllegalStateException("App icon not found on the module path: " + resourceName(size));
        }
        return location;
    }
}
