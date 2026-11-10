package com.example.billard.ui;

import com.example.billard.model.Ball;
import com.example.billard.model.Table;
import com.example.billard.model.Vec2;
import java.util.List;
import javafx.scene.canvas.GraphicsContext;

/** Draws the table in world coordinates: rails, cushions, cloth, pockets, then the balls. */
final class TableRenderer {

    static final double RAIL_WIDTH = 64;

    private static final double CUSHION_WIDTH = 22;
    private static final double RAIL_CORNER = 44;
    private static final double SPOT_RADIUS = 4;
    private static final double SIGHT_RADIUS = 4.5;
    private static final int SIGHTS_ALONG_LONG_RAIL = 8;
    private static final int SIGHTS_ALONG_SHORT_RAIL = 4;

    private final Table table;

    TableRenderer(Table table) {
        this.table = table;
    }

    void drawSurface(GraphicsContext gc) {
        double w = table.width();
        double h = table.height();

        gc.setFill(Palette.RAIL);
        gc.fillRoundRect(-RAIL_WIDTH, -RAIL_WIDTH, w + 2 * RAIL_WIDTH, h + 2 * RAIL_WIDTH, RAIL_CORNER, RAIL_CORNER);
        gc.setFill(Palette.CUSHION);
        gc.fillRect(-CUSHION_WIDTH, -CUSHION_WIDTH, w + 2 * CUSHION_WIDTH, h + 2 * CUSHION_WIDTH);
        gc.setFill(Palette.CLOTH);
        gc.fillRect(0, 0, w, h);

        drawSights(gc);
        drawSpots(gc);
        drawPockets(gc);
    }

    void drawBalls(GraphicsContext gc, List<Ball> balls) {
        for (Ball ball : balls) {
            if (!ball.isPocketed()) {
                BallPainter.paint(gc, ball.number(), ball.position().x(), ball.position().y(), table.ballRadius());
            }
        }
    }

    private void drawSights(GraphicsContext gc) {
        double offset = (RAIL_WIDTH + CUSHION_WIDTH) / 2;
        gc.setFill(Palette.RAIL_SIGHT);
        for (int i = 1; i < SIGHTS_ALONG_LONG_RAIL; i++) {
            if (i != SIGHTS_ALONG_LONG_RAIL / 2) {
                double x = table.width() * i / SIGHTS_ALONG_LONG_RAIL;
                fillSight(gc, x, -offset);
                fillSight(gc, x, table.height() + offset);
            }
        }
        for (int i = 1; i < SIGHTS_ALONG_SHORT_RAIL; i++) {
            double y = table.height() * i / SIGHTS_ALONG_SHORT_RAIL;
            fillSight(gc, -offset, y);
            fillSight(gc, table.width() + offset, y);
        }
    }

    private void drawSpots(GraphicsContext gc) {
        gc.setFill(Palette.CLOTH_SPOT);
        for (Vec2 spot : List.of(table.headSpot(), table.footSpot())) {
            gc.fillOval(spot.x() - SPOT_RADIUS, spot.y() - SPOT_RADIUS, SPOT_RADIUS * 2, SPOT_RADIUS * 2);
        }
    }

    private void drawPockets(GraphicsContext gc) {
        gc.setFill(Palette.POCKET);
        for (Table.Pocket pocket : table.pockets()) {
            double r = pocket.captureRadius();
            gc.fillOval(pocket.center().x() - r, pocket.center().y() - r, r * 2, r * 2);
        }
    }

    private static void fillSight(GraphicsContext gc, double x, double y) {
        gc.fillOval(x - SIGHT_RADIUS, y - SIGHT_RADIUS, SIGHT_RADIUS * 2, SIGHT_RADIUS * 2);
    }
}
