package com.example.billard.ui;

import javafx.scene.Scene;
import javafx.scene.layout.Region;

/** Applies the stylesheet and palette to a scene. */
public final class Theme {

    private static final String STYLESHEET = "style.css";

    private Theme() {
    }

    public static void applyTo(Scene scene, Region root) {
        var stylesheet = Theme.class.getResource(STYLESHEET);
        if (stylesheet == null) {
            throw new IllegalStateException("Stylesheet not found on the module path: " + STYLESHEET);
        }
        root.setStyle(PaletteStyle.rootDeclarations());
        scene.getStylesheets().add(stylesheet.toExternalForm());
    }
}
