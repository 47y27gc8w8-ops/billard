package com.example.billard.model;

import java.util.List;

/**
 * Playing surface geometry. Coordinates run from (0,0) at the top-left cushion edge to (width,height);
 * ball centres stay within one ball radius of those edges.
 */
public record Table(double width, double height, double ballRadius, List<Pocket> pockets) {

    private static final double STANDARD_WIDTH = 1000;
    private static final double STANDARD_HEIGHT = 500;
    private static final double STANDARD_BALL_RADIUS = 14;
    private static final double CORNER_POCKET_RADIUS = 34;
    private static final double SIDE_POCKET_RADIUS = 32;
    private static final double SIDE_POCKET_OVERHANG = 6;

    public record Pocket(Vec2 center, double captureRadius) {
    }

    public Table {
        pockets = List.copyOf(pockets);
    }

    public static Table standard() {
        double w = STANDARD_WIDTH;
        double h = STANDARD_HEIGHT;
        return new Table(w, h, STANDARD_BALL_RADIUS, List.of(
                new Pocket(new Vec2(0, 0), CORNER_POCKET_RADIUS),
                new Pocket(new Vec2(w, 0), CORNER_POCKET_RADIUS),
                new Pocket(new Vec2(0, h), CORNER_POCKET_RADIUS),
                new Pocket(new Vec2(w, h), CORNER_POCKET_RADIUS),
                new Pocket(new Vec2(w / 2, -SIDE_POCKET_OVERHANG), SIDE_POCKET_RADIUS),
                new Pocket(new Vec2(w / 2, h + SIDE_POCKET_OVERHANG), SIDE_POCKET_RADIUS)));
    }

    public Vec2 headSpot() {
        return new Vec2(width / 4, height / 2);
    }

    public Vec2 footSpot() {
        return new Vec2(width * 3 / 4, height / 2);
    }

    public boolean containsCenter(Vec2 center) {
        return center.x() >= ballRadius && center.x() <= width - ballRadius
                && center.y() >= ballRadius && center.y() <= height - ballRadius;
    }

    public boolean isInsidePocketZone(Vec2 center) {
        return pockets.stream().anyMatch(p -> p.center().distanceTo(center) < p.captureRadius() + ballRadius);
    }

    public Vec2 clampCenter(Vec2 point) {
        double x = Math.max(ballRadius, Math.min(width - ballRadius, point.x()));
        double y = Math.max(ballRadius, Math.min(height - ballRadius, point.y()));
        return new Vec2(x, y);
    }
}
