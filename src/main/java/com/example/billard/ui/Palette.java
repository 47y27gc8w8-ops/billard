package com.example.billard.ui;

import javafx.scene.paint.Color;

/**
 * The single source of colour for the game. Canvas drawing reads these constants directly; the stylesheet
 * reads the interface colours through {@link PaletteStyle}.
 *
 * <p>Direction: a pool hall at night. A dark room, one lamp over the table, worsted cloth, walnut rails and
 * warm smoked-wood panels. One accent, the blue of cue chalk, is reserved for the action, the cue tip and the
 * active player.
 */
public final class Palette {

    public static final Color GROUND = Color.web("#0B0D0C");
    public static final Color PANEL = Color.web("#1C1915");
    public static final Color PANEL_RAISED = Color.web("#2A251F");
    public static final Color INK = Color.web("#F2EDE3");
    public static final Color INK_MUTED = Color.web("#B9AF9F");
    public static final Color ACCENT = Color.web("#6FD3F7");
    public static final Color ACCENT_INK = Color.web("#0B1418");
    public static final Color DANGER = Color.web("#FF6B5E");

    public static final Color CLOTH = Color.web("#0F6E4F");
    public static final Color CLOTH_SPOT = Color.web("#E9E4D6");
    public static final Color WOOD_LIGHT = Color.web("#7B4A26");
    public static final Color WOOD_DARK = Color.web("#2E170A");
    public static final Color RAIL_SIGHT = Color.web("#ECE6D6");
    public static final Color POCKET_LINER = Color.web("#1E1712");
    public static final Color POCKET = Color.web("#030303");

    public static final Color GUIDE_LINE = Color.color(1, 1, 1, 0.55);
    public static final Color GUIDE_GHOST = Color.color(1, 1, 1, 0.8);
    public static final Color BALL_CROSSHAIR = Color.color(0.08, 0.09, 0.1, 0.22);

    public static final Color CUE_BALL = Color.web("#F4F1E8");
    public static final Color CUE_SHAFT = Color.web("#E6CD9E");
    public static final Color CUE_FERRULE = Color.web("#F3EEE2");
    public static final Color CUE_JOINT = Color.web("#C9A55A");
    public static final Color CUE_FOREARM = Color.web("#3A1C0E");
    public static final Color CUE_WRAP = Color.web("#141414");

    private static final Color[] BALL_COLORS = {
            Color.web("#F4C21E"), // 1, 9  yellow
            Color.web("#1C4FB4"), // 2, 10 blue
            Color.web("#D42A20"), // 3, 11 red
            Color.web("#5B2D93"), // 4, 12 purple
            Color.web("#F06A12"), // 5, 13 orange
            Color.web("#17803F"), // 6, 14 green
            Color.web("#7C1622"), // 7, 15 maroon
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
