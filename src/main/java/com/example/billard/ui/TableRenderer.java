package com.example.billard.ui;

import com.example.billard.model.Ball;
import com.example.billard.model.Table;
import com.example.billard.model.Vec2;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * Draws the table in world coordinates, bottom layer first: its shadow on the floor, walnut rails, pocket liners,
 * the cloth bed, cushions, the lamp's light, sights, spots and pocket holes. Balls go on top in two passes, all
 * shadows before any ball, so no shadow falls across a neighbouring ball.
 */
final class TableRenderer {

    static final double RAIL_WIDTH = 64;
    /** Room around the rails for the table's shadow on the floor. */
    static final double FLOOR_MARGIN = 26;

    private static final double CUSHION_WIDTH = 22;
    private static final double RAIL_CORNER_RADIUS = 26;
    private static final double CORNER_JAW_START = 38;
    private static final double CORNER_JAW_CUT = 22;
    private static final double SIDE_JAW_GAP = 33;
    private static final double SIDE_JAW_CUT = 8;
    private static final double LINER_WIDTH = 12;
    private static final double OCCLUSION_DEPTH = 18;
    private static final double SPOT_RADIUS = 3.2;
    private static final double SIGHT_SIZE = 5.5;
    private static final int SIGHTS_ALONG_LONG_RAIL = 8;
    private static final int SIGHTS_ALONG_SHORT_RAIL = 4;
    private static final int FLOOR_SHADOW_LAYERS = 14;

    private final Table table;
    private final RollingBallPainter ballPainter = new RollingBallPainter();
    private final double width;
    private final double height;

    TableRenderer(Table table) {
        this.table = table;
        this.width = table.width();
        this.height = table.height();
    }

    /**
     * Draws everything except the balls.
     *
     * @param pixelsPerUnit device pixels per table unit, so textures land one texel per pixel
     */
    void drawSurface(GraphicsContext gc, double pixelsPerUnit) {
        drawFloorShadow(gc);
        drawRails(gc, pixelsPerUnit);
        drawPocketLiners(gc);
        gc.setFill(texture(FeltTexture.clothTile(), pixelsPerUnit));
        gc.fillRect(0, 0, width, height);
        drawCushions(gc, pixelsPerUnit);
        drawCushionOcclusion(gc);
        drawLampPool(gc);
        drawSights(gc);
        drawSpots(gc);
        drawPocketHoles(gc);
    }

    /** @param pixelsPerUnit device pixels per table unit, so each ball's surface is traced at screen resolution */
    void drawBalls(GraphicsContext gc, List<Ball> balls, double pixelsPerUnit) {
        double radius = table.ballRadius();
        List<Ball> onTable = balls.stream().filter(ball -> !ball.isPocketed()).toList();
        onTable.forEach(ball -> BallPainter.paintShadow(gc, ball.position().x(), ball.position().y(), radius));
        onTable.forEach(ball -> ballPainter.paint(gc, ball, radius, pixelsPerUnit));
    }

    /** Stacked, slightly offset outlines with low alpha: a cheap, very soft shadow under the whole table. */
    private void drawFloorShadow(GraphicsContext gc) {
        double step = FLOOR_MARGIN / FLOOR_SHADOW_LAYERS;
        gc.setFill(Lighting.black(0.09));
        for (int layer = FLOOR_SHADOW_LAYERS; layer >= 0; layer--) {
            double grow = layer * step;
            roundedOutline(gc, grow, RAIL_CORNER_RADIUS + grow, layer * step * 0.35, layer * step * 0.55);
            gc.fill();
        }
    }

    // ---- Rails ----

    /** Four mitred walnut rails, each with its grain running along it, varnished and rounded by light. */
    private void drawRails(GraphicsContext gc, double pixelsPerUnit) {
        Paint along = texture(WoodTexture.alongTile(), pixelsPerUnit);
        Paint across = texture(WoodTexture.acrossTile(), pixelsPerUnit);
        for (Edge edge : Edge.values()) {
            drawRail(gc, edge.isLong() ? along : across, edge);
        }
        drawMitreJoints(gc);
    }

    /** One rail: from its outer edge to the back of the cushion, both ends mitred where it meets the next rail. */
    private void drawRail(GraphicsContext gc, Paint wood, Edge edge) {
        double r = RAIL_WIDTH;
        double c = CUSHION_WIDTH;
        double length = edge.lengthOn(width, height);
        Polygon rail = Polygon.of(
                point(edge, -r, r), point(edge, length + r, r), point(edge, length + c, c), point(edge, -c, c));
        Vec2 outer = point(edge, 0, r);
        Vec2 inner = point(edge, 0, c);
        gc.save();
        roundedOutline(gc, 0, RAIL_CORNER_RADIUS, 0, 0);
        gc.clip();
        gc.setFill(wood);
        rail.fill(gc);
        gc.setFill(railProfile(outer.x(), outer.y(), inner.x(), inner.y()));
        rail.fill(gc);
        gc.restore();
    }

    /** Light across a rail: dark rounded outer edge, a varnish gleam, then shade down to the cushion. */
    static Paint railProfile(double outerX, double outerY, double innerX, double innerY) {
        return new LinearGradient(outerX, outerY, innerX, innerY, false, CycleMethod.NO_CYCLE,
                new Stop(0, Lighting.black(0.55)),
                new Stop(0.08, Lighting.black(0.1)),
                new Stop(0.2, Lighting.white(0.16)),
                new Stop(0.32, Lighting.white(0.04)),
                new Stop(0.75, Lighting.black(0.12)),
                new Stop(1, Lighting.black(0.45)));
    }

    private void drawMitreJoints(GraphicsContext gc) {
        double r = RAIL_WIDTH;
        double c = CUSHION_WIDTH;
        gc.setStroke(Lighting.black(0.45));
        gc.setLineWidth(1.2);
        gc.strokeLine(-r, -r, -c, -c);
        gc.strokeLine(width + r, -r, width + c, -c);
        gc.strokeLine(-r, height + r, -c, height + c);
        gc.strokeLine(width + r, height + r, width + c, height + c);
    }

    // ---- Pockets ----

    /** Leather cups set into the rails around each pocket; the cloth and cushions cover their inner half. */
    private void drawPocketLiners(GraphicsContext gc) {
        gc.save();
        roundedOutline(gc, 0, RAIL_CORNER_RADIUS, 0, 0);
        gc.clip();
        for (Table.Pocket pocket : table.pockets()) {
            double r = pocket.captureRadius() + LINER_WIDTH;
            double x = pocket.center().x();
            double y = pocket.center().y();
            gc.setFill(new RadialGradient(0, 0, x, y, r, false, CycleMethod.NO_CYCLE,
                    new Stop(0.6, Palette.POCKET),
                    new Stop(0.82, Palette.POCKET_LINER),
                    new Stop(0.93, Palette.POCKET_LINER.deriveColor(0, 1, 1.9, 1)),
                    new Stop(1, Palette.POCKET_LINER.deriveColor(0, 1, 0.7, 1))));
            gc.fillOval(x - r, y - r, r * 2, r * 2);
        }
        gc.restore();
    }

    /** Each hole falls away into black, with the lip catching a little light on the side away from the lamp. */
    private void drawPocketHoles(GraphicsContext gc) {
        for (Table.Pocket pocket : table.pockets()) {
            double r = pocket.captureRadius();
            double x = pocket.center().x();
            double y = pocket.center().y();
            gc.setFill(new RadialGradient(0, 0, x - r * 0.12, y - r * 0.15, r * 1.05, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Palette.POCKET),
                    new Stop(0.72, Palette.POCKET),
                    new Stop(0.9, Color.web("#15110E")),
                    new Stop(1, Color.web("#3A2E25"))));
            gc.fillOval(x - r, y - r, r * 2, r * 2);
        }
    }

    // ---- Cloth ----

    /** The six cushions as trapezoids, wider at the rail, cut back at each pocket into its jaws. */
    private void drawCushions(GraphicsContext gc, double pixelsPerUnit) {
        Paint cloth = texture(FeltTexture.clothTile(), pixelsPerUnit);
        for (Cushion cushion : cushions()) {
            drawCushion(gc, cloth, cushion);
        }
    }

    /** Two cushions on each long rail, split by the side pocket, and one on each short rail. */
    private List<Cushion> cushions() {
        double sideJaw = width / 2 - SIDE_JAW_GAP;
        List<Cushion> cushions = new ArrayList<>();
        for (Edge edge : Edge.values()) {
            double length = edge.lengthOn(width, height);
            Jaw first = new Jaw(CORNER_JAW_START, CORNER_JAW_CUT);
            Jaw last = new Jaw(length - CORNER_JAW_START, CORNER_JAW_CUT);
            if (edge.isLong()) {
                cushions.add(new Cushion(edge, first, new Jaw(sideJaw, SIDE_JAW_CUT)));
                cushions.add(new Cushion(edge, new Jaw(length - sideJaw, SIDE_JAW_CUT), last));
            } else {
                cushions.add(new Cushion(edge, first, last));
            }
        }
        return cushions;
    }

    private void drawCushion(GraphicsContext gc, Paint cloth, Cushion cushion) {
        Edge edge = cushion.edge();
        double start = cushion.start().at();
        double end = cushion.end().at();
        Polygon outline = Polygon.of(
                point(edge, start, 0),
                point(edge, end, 0),
                point(edge, end + cushion.end().cut(), CUSHION_WIDTH),
                point(edge, start - cushion.start().cut(), CUSHION_WIDTH));
        gc.setFill(cloth);
        outline.fill(gc);

        double middle = (start + end) / 2;
        Vec2 nose = point(edge, middle, 0);
        Vec2 back = point(edge, middle, CUSHION_WIDTH);
        gc.setFill(new LinearGradient(nose.x(), nose.y(), back.x(), back.y(), false, CycleMethod.NO_CYCLE,
                new Stop(0, Lighting.black(0.35)),
                new Stop(0.1, Lighting.white(0.12)),
                new Stop(0.3, Lighting.white(0.03)),
                new Stop(0.75, Lighting.black(0.2)),
                new Stop(1, Lighting.black(0.55))));
        outline.fill(gc);
    }

    /** The cushions keep lamp light off the cloth right beside them. */
    private void drawCushionOcclusion(GraphicsContext gc) {
        for (Edge edge : Edge.values()) {
            double length = edge.lengthOn(width, height);
            Vec2 from = point(edge, 0, 0);
            Vec2 to = point(edge, 0, -OCCLUSION_DEPTH);
            Vec2 far = point(edge, length, -OCCLUSION_DEPTH);
            gc.setFill(new LinearGradient(from.x(), from.y(), to.x(), to.y(), false, CycleMethod.NO_CYCLE,
                    new Stop(0, Lighting.black(0.38)), new Stop(1, Color.TRANSPARENT)));
            gc.fillRect(Math.min(from.x(), far.x()), Math.min(from.y(), far.y()),
                    Math.abs(far.x() - from.x()), Math.abs(far.y() - from.y()));
        }
    }

    /** One lamp over the middle: a faint bright pool, falling off into shade toward the rails. */
    private void drawLampPool(GraphicsContext gc) {
        double r = RAIL_WIDTH;
        gc.setFill(new RadialGradient(0, 0, 0.5, 0.48, 0.62, true, CycleMethod.NO_CYCLE,
                new Stop(0, Lighting.white(0.1)),
                new Stop(0.35, Color.TRANSPARENT),
                new Stop(0.7, Lighting.black(0.26)),
                new Stop(1, Lighting.black(0.55))));
        gc.save();
        roundedOutline(gc, 0, RAIL_CORNER_RADIUS, 0, 0);
        gc.clip();
        gc.fillRect(-r, -r, width + 2 * r, height + 2 * r);
        gc.restore();
    }

    private void drawSpots(GraphicsContext gc) {
        gc.setFill(Palette.CLOTH_SPOT.deriveColor(0, 1, 1, 0.7));
        for (Vec2 spot : List.of(table.headSpot(), table.footSpot())) {
            gc.fillOval(spot.x() - SPOT_RADIUS, spot.y() - SPOT_RADIUS, SPOT_RADIUS * 2, SPOT_RADIUS * 2);
        }
    }

    /** Mother-of-pearl diamonds inlaid along the rails. */
    private void drawSights(GraphicsContext gc) {
        double offset = (RAIL_WIDTH + CUSHION_WIDTH) / 2;
        for (int i = 1; i < SIGHTS_ALONG_LONG_RAIL; i++) {
            if (i != SIGHTS_ALONG_LONG_RAIL / 2) {
                double x = width * i / SIGHTS_ALONG_LONG_RAIL;
                drawSight(gc, x, -offset);
                drawSight(gc, x, height + offset);
            }
        }
        for (int i = 1; i < SIGHTS_ALONG_SHORT_RAIL; i++) {
            double y = height * i / SIGHTS_ALONG_SHORT_RAIL;
            drawSight(gc, -offset, y);
            drawSight(gc, width + offset, y);
        }
    }

    private static void drawSight(GraphicsContext gc, double x, double y) {
        double s = SIGHT_SIZE;
        double[] xs = {x, x + s, x, x - s};
        double[] ys = {y - s, y, y + s, y};
        gc.setFill(Lighting.black(0.5));
        gc.fillPolygon(shift(xs, 0.6), shift(ys, 0.9), 4);
        gc.setFill(new LinearGradient(x - s, y - s, x + s, y + s, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE), new Stop(0.5, Palette.RAIL_SIGHT), new Stop(1, Color.web("#B9B2A0"))));
        gc.fillPolygon(xs, ys, 4);
    }

    private static double[] shift(double[] values, double by) {
        double[] shifted = values.clone();
        for (int i = 0; i < shifted.length; i++) {
            shifted[i] += by;
        }
        return shifted;
    }

    // ---- Helpers ----

    /** The table point {@code along} an edge and {@code depth} out from the cloth into the rail. */
    private Vec2 point(Edge edge, double along, double depth) {
        return edge.toTable(along, depth, width, height);
    }

    /** A texture tiled in table units so that one texel covers one device pixel. */
    static Paint texture(Image tile, double pixelsPerUnit) {
        return new ImagePattern(tile, 0, 0, tile.getWidth() / pixelsPerUnit, tile.getHeight() / pixelsPerUnit, false);
    }

    /** Sets the current path to the table's outer outline, grown by {@code grow} and shifted by (dx, dy). */
    private void roundedOutline(GraphicsContext gc, double grow, double cornerRadius, double dx, double dy) {
        double left = -RAIL_WIDTH - grow + dx;
        double top = -RAIL_WIDTH - grow + dy;
        double right = width + RAIL_WIDTH + grow + dx;
        double bottom = height + RAIL_WIDTH + grow + dy;
        gc.beginPath();
        gc.moveTo(left + cornerRadius, top);
        gc.arcTo(right, top, right, bottom, cornerRadius);
        gc.arcTo(right, bottom, left, bottom, cornerRadius);
        gc.arcTo(left, bottom, left, top, cornerRadius);
        gc.arcTo(left, top, right, top, cornerRadius);
        gc.closePath();
    }

    /** Where a cushion ends along its edge, and how far its jaw is cut back from there at the rail. */
    private record Jaw(double at, double cut) {
    }

    /** One cushion: the stretch of an edge between two pocket jaws. */
    private record Cushion(Edge edge, Jaw start, Jaw end) {
    }

    /** A closed outline, ready to fill. */
    private record Polygon(double[] xs, double[] ys) {

        static Polygon of(Vec2... corners) {
            double[] xs = new double[corners.length];
            double[] ys = new double[corners.length];
            for (int i = 0; i < corners.length; i++) {
                xs[i] = corners[i].x();
                ys[i] = corners[i].y();
            }
            return new Polygon(xs, ys);
        }

        void fill(GraphicsContext gc) {
            gc.fillPolygon(xs, ys, xs.length);
        }
    }

    /**
     * The four cushion lines, each with its own frame: {@code along} runs the length of the edge and {@code depth}
     * runs outward from the cloth into the rail.
     */
    private enum Edge {
        TOP, BOTTOM, LEFT, RIGHT;

        boolean isLong() {
            return this == TOP || this == BOTTOM;
        }

        double lengthOn(double width, double height) {
            return isLong() ? width : height;
        }

        Vec2 toTable(double along, double depth, double width, double height) {
            return switch (this) {
                case TOP -> new Vec2(along, -depth);
                case BOTTOM -> new Vec2(along, height + depth);
                case LEFT -> new Vec2(-depth, along);
                case RIGHT -> new Vec2(width + depth, along);
            };
        }
    }
}
