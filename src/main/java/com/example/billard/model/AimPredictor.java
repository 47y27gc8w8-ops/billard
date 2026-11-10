package com.example.billard.model;

import java.util.List;
import java.util.Optional;

/** Casts a ray from the cue ball to find the first thing it would touch. */
public final class AimPredictor {

    private static final double NO_CONTACT = Double.POSITIVE_INFINITY;
    private static final double TOUCHING_TOLERANCE = 1e-6;

    private AimPredictor() {
    }

    public static AimPrediction predict(Table table, List<Ball> balls, Ball cueBall, double angle) {
        Vec2 origin = cueBall.position();
        Vec2 direction = Vec2.fromAngle(angle);
        double nearest = distanceToCushion(table, origin, direction);
        Optional<Ball> target = Optional.empty();

        for (Ball ball : balls) {
            if (ball == cueBall || ball.isPocketed()) {
                continue;
            }
            double distance = distanceToContact(origin, direction, ball.position(), 2 * table.ballRadius());
            if (distance < nearest) {
                nearest = distance;
                target = Optional.of(ball);
            }
        }
        return new AimPrediction(origin.plus(direction.times(nearest)), target);
    }

    private static double distanceToCushion(Table table, Vec2 origin, Vec2 direction) {
        double r = table.ballRadius();
        double alongX = distanceToWall(origin.x(), direction.x(), r, table.width() - r);
        double alongY = distanceToWall(origin.y(), direction.y(), r, table.height() - r);
        return Math.min(alongX, alongY);
    }

    private static double distanceToWall(double start, double speed, double low, double high) {
        if (speed == 0) {
            return NO_CONTACT;
        }
        double wall = speed > 0 ? high : low;
        return Math.max(0, (wall - start) / speed);
    }

    /** Distance along the ray at which a circle of radius {@code contactDistance} around the point would touch. */
    private static double distanceToContact(Vec2 origin, Vec2 direction, Vec2 point, double contactDistance) {
        Vec2 toPoint = point.minus(origin);
        double along = toPoint.dot(direction);
        if (along < 0) {
            return NO_CONTACT;
        }
        double missSquared = toPoint.dot(toPoint) - along * along;
        double reachSquared = contactDistance * contactDistance - missSquared;
        if (reachSquared < 0) {
            return NO_CONTACT;
        }
        return Math.max(0, along - Math.sqrt(reachSquared) + TOUCHING_TOLERANCE);
    }
}
