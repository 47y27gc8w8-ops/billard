package com.example.billard.model;

import java.util.List;

/** Finds legal spots for a ball that is placed by hand. */
public final class BallPlacement {

    private static final int RINGS = 160;
    private static final int DIRECTIONS = 24;
    private static final double RING_STEP_IN_RADII = 0.5;

    private BallPlacement() {
    }

    public static boolean isFree(Table table, List<Ball> balls, Ball moving, Vec2 center) {
        if (!table.containsCenter(center) || table.isInsidePocketZone(center)) {
            return false;
        }
        double minimumDistance = 2 * table.ballRadius();
        return balls.stream()
                .filter(ball -> ball != moving && !ball.isPocketed())
                .noneMatch(ball -> ball.position().distanceTo(center) < minimumDistance);
    }

    /** The free spot closest to {@code preferred}, searched outward in rings. */
    public static Vec2 nearestFree(Table table, List<Ball> balls, Ball moving, Vec2 preferred) {
        for (int ring = 0; ring < RINGS; ring++) {
            double distance = ring * table.ballRadius() * RING_STEP_IN_RADII;
            for (int step = 0; step < DIRECTIONS; step++) {
                Vec2 candidate = preferred.plus(Vec2.fromAngle(2 * Math.PI * step / DIRECTIONS).times(distance));
                if (isFree(table, balls, moving, candidate)) {
                    return candidate;
                }
            }
        }
        throw new IllegalStateException("No free spot left on the table for ball " + moving.number());
    }
}
