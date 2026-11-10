package com.example.billard.ui;

import javafx.scene.paint.Color;

/** Hands the {@link Palette} to the stylesheet as looked-up colours, so each hex value is written once. */
final class PaletteStyle {

    private PaletteStyle() {
    }

    static String rootDeclarations() {
        return declaration("ground", Palette.GROUND)
                + declaration("panel", Palette.PANEL)
                + declaration("panel-raised", Palette.PANEL_RAISED)
                + declaration("ink", Palette.INK)
                + declaration("ink-muted", Palette.INK_MUTED)
                + declaration("accent", Palette.ACCENT)
                + declaration("accent-ink", Palette.ACCENT_INK)
                + declaration("danger", Palette.DANGER)
                + declaration("cloth", Palette.CLOTH)
                + declaration("rail", Palette.RAIL);
    }

    private static String declaration(String name, Color color) {
        return "-" + name + ": " + toHex(color) + ";";
    }

    private static String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255));
    }
}
