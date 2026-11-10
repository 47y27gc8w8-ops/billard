package com.example.billard.ui;

import com.example.billard.model.Table;
import com.example.billard.model.Vec2;
import javafx.scene.canvas.GraphicsContext;

/** Fits the table, rails included, into a pane and converts between pane pixels and table coordinates. */
final class Viewport {

    private final double scale;
    private final double offsetX;
    private final double offsetY;

    Viewport(double paneWidth, double paneHeight, Table table) {
        double contentWidth = table.width() + 2 * TableRenderer.RAIL_WIDTH;
        double contentHeight = table.height() + 2 * TableRenderer.RAIL_WIDTH;
        this.scale = Math.max(0.01, Math.min(paneWidth / contentWidth, paneHeight / contentHeight));
        this.offsetX = (paneWidth - contentWidth * scale) / 2;
        this.offsetY = (paneHeight - contentHeight * scale) / 2;
    }

    /** Sets up the graphics context so that drawing uses table coordinates. */
    void apply(GraphicsContext gc) {
        gc.translate(offsetX, offsetY);
        gc.scale(scale, scale);
        gc.translate(TableRenderer.RAIL_WIDTH, TableRenderer.RAIL_WIDTH);
    }

    Vec2 toTable(double paneX, double paneY) {
        return new Vec2(
                (paneX - offsetX) / scale - TableRenderer.RAIL_WIDTH,
                (paneY - offsetY) / scale - TableRenderer.RAIL_WIDTH);
    }
}
