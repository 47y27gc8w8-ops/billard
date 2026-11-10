package com.example.billard.ui;

import javafx.scene.paint.Color;

/**
 * The single source of colour for the game. Canvas drawing reads these constants directly; the stylesheet
 * reads the same values through {@link PaletteStyle}.
 *
 * <p>Direction: a pool hall at night. Near-black green ground, flat cloth, walnut rails, and one accent,
 * the blue of cue chalk, reserved for the action, the cue tip and the active player.
 */
public final class Palette {

    public static final Color GROUND = Color.web("#0C1411");
    public static final Color PANEL = Color.web("#15231D");
    public static final Color PANEL_RAISED = Color.web("#1F3329");
    public static final Color INK = Color.web("#EEF1EC");
    public static final Color INK_MUTED = Color.web("#A9B9B0");
    public static final Color ACCENT = Color.web("#6FD3F7");
    public static final Color ACCENT_INK = Color.web("#0C1411");
    public static final Color DANGER = Color.web("#FF6B5E");

    public static final Color CLOTH = Color.web("#13705A");
    public static final Color CLOTH_SPOT = Color.web("#0F5C4A");
    public static final Color CUSHION = Color.web("#0E5444");
    public static final Color RAIL = Color.web("#4B2B18");
    public static final Color RAIL_SIGHT = Color.web("#D8CBB2");
    public static final Color POCKET = Color.web("#050807");
    public static final Color BALL_SHADOW = Color.color(0, 0, 0, 0.28);

    public static final Color GUIDE_LINE = Color.color(1, 1, 1, 0.6);
    public static final Color GUIDE_GHOST = Color.color(1, 1, 1, 0.85);
    public static final Color BALL_HIGHLIGHT = Color.color(1, 1, 1, 0.5);

    public static final Color CUE_BALL = Color.web("#F5F3EC");
    public static final Color CUE_SHAFT = Color.web("#E4C99B");
    public static final Color CUE_BUTT = Color.web("#2B1A10");

    private static final Color[] BALL_COLORS = {
            Color.web("#F2C230"), // 1, 9  yellow
            Color.web("#1F5FBF"), // 2, 10 blue
            Color.web("#D7352B"), // 3, 11 red
            Color.web("#6A3FA0"), // 4, 12 purple
            Color.web("#EE7A1C"), // 5, 13 orange
            Color.web("#1E8A4C"), // 6, 14 green
            Color.web("#7A1F2B"), // 7, 15 maroon
    };
    public static final Color BALL_NUMBER_INK = Color.web("#15171A");

    private Palette() {
    }

    /** Body colour of a numbered ball: the solid colour, or the stripe colour for 9 to 15. */
    public static Color ballColor(int number) {
        if (number == 0) {
            return CUE_BALL;
        }
        if (number == 8) {
            return BALL_NUMBER_INK;
        }
        return BALL_COLORS[(number - 1) % 8];
    }
}
