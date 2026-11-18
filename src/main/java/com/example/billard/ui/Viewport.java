package com.example.billard.ui;

import com.example.billard.model.Table;
import com.example.billard.model.Vec2;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;

/**
 * Fits the table, rails and floor shadow included, into a pane and converts between pane pixels and table
 * coordinates.
 */
final class Viewport {

    private static final double BORDER = TableRenderer.RAIL_WIDTH + TableRenderer.FLOOR_MARGIN;

    private final double scale;
    private final double offsetX;
    private final double offsetY;

    Viewport(double paneWidth, double paneHeight, Table table) {
        double contentWidth = table.width() + 2 * BORDER;
        double contentHeight = table.height() + 2 * BORDER;
        this.scale = Math.max(0.01, Math.min(paneWidth / contentWidth, paneHeight / contentHeight));
        this.offsetX = (paneWidth - contentWidth * scale) / 2;
        this.offsetY = (paneHeight - contentHeight * scale) / 2;
    }

    /** Device pixels per pane pixel for a node's window: above 1 on scaled (high-DPI) displays. */
    static double outputScaleOf(Node node) {
        if (node.getScene() == null || node.getScene().getWindow() == null) {
            return 1;
        }
        return node.getScene().getWindow().getOutputScaleX();
    }

    /** Pane pixels per table unit. */
    double scale() {
        return scale;
    }

    /** Sets up the graphics context so that drawing uses table coordinates. */
    void apply(GraphicsContext gc) {
        gc.translate(offsetX, offsetY);
        gc.scale(scale, scale);
        gc.translate(BORDER, BORDER);
    }

    Vec2 toTable(double paneX, double paneY) {
        return new Vec2(
                (paneX - offsetX) / scale - BORDER,
                (paneY - offsetY) / scale - BORDER);
    }
}
