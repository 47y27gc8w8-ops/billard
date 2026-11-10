package com.example.billard.model;

import java.util.List;

/**
 * Fixed-step simulation of balls rolling, bouncing and dropping into pockets. No spin: balls decelerate at a
 * constant rate and exchange momentum along the line of centres.
 */
public final class PhysicsEngine {

    public static final double FIXED_STEP = 1.0 / 240.0;

    private static final double ROLLING_DECELERATION = 520.0;
    private static final double STOP_SPEED = 10.0;
    private static final double BALL_RESTITUTION = 0.96;
    private static final double CUSHION_RESTITUTION = 0.76;
    private static final int MAX_STEPS_PER_ADVANCE = 60;

    private final Table table;
    private final PhysicsListener listener;
    private double pendingTime;

    public PhysicsEngine(Table table, PhysicsListener listener) {
        this.table = table;
        this.listener = listener;
    }

    /** Simulates {@code seconds} of motion, in as many fixed steps as fit. A backlog beyond the cap is dropped. */
    public void advance(List<Ball> balls, double seconds) {
        pendingTime += seconds;
        int steps = 0;
        while (pendingTime >= FIXED_STEP && steps < MAX_STEPS_PER_ADVANCE) {
            step(balls);
            pendingTime -= FIXED_STEP;
            steps++;
        }
        if (steps == MAX_STEPS_PER_ADVANCE) {
            pendingTime = 0;
        }
    }

    void step(List<Ball> balls) {
        for (Ball ball : balls) {
            if (!ball.isPocketed()) {
                moveAndSlow(ball);
                if (!tryPocket(ball)) {
                    bounceOffCushions(ball);
                }
            }
        }
        resolveCollisions(balls);
    }

    private void moveAndSlow(Ball ball) {
        ball.setPosition(ball.position().plus(ball.velocity().times(FIXED_STEP)));
        double speed = ball.velocity().length();
        if (speed == 0) {
            return;
        }
        double slowedSpeed = speed - ROLLING_DECELERATION * FIXED_STEP;
        ball.setVelocity(slowedSpeed < STOP_SPEED ? Vec2.ZERO : ball.velocity().times(slowedSpeed / speed));
    }

    private boolean tryPocket(Ball ball) {
        for (Table.Pocket pocket : table.pockets()) {
            if (pocket.center().distanceTo(ball.position()) < pocket.captureRadius()) {
                ball.pocket();
                listener.onPocketed(ball);
                return true;
            }
        }
        return false;
    }

    private void bounceOffCushions(Ball ball) {
        double radius = table.ballRadius();
        double x = ball.position().x();
        double y = ball.position().y();
        double vx = ball.velocity().x();
        double vy = ball.velocity().y();
        boolean bounced = false;

        if (x < radius || x > table.width() - radius) {
            boolean movingIntoWall = (x < radius) == (vx < 0) && vx != 0;
            x = Math.max(radius, Math.min(table.width() - radius, x));
            if (movingIntoWall) {
                vx = -vx * CUSHION_RESTITUTION;
                bounced = true;
            }
        }
        if (y < radius || y > table.height() - radius) {
            boolean movingIntoWall = (y < radius) == (vy < 0) && vy != 0;
            y = Math.max(radius, Math.min(table.height() - radius, y));
            if (movingIntoWall) {
                vy = -vy * CUSHION_RESTITUTION;
                bounced = true;
            }
        }
        ball.setPosition(new Vec2(x, y));
        ball.setVelocity(new Vec2(vx, vy));
        if (bounced) {
            listener.onCushionHit(ball);
        }
    }

    private void resolveCollisions(List<Ball> balls) {
        for (int i = 0; i < balls.size(); i++) {
            for (int j = i + 1; j < balls.size(); j++) {
                Ball first = balls.get(i);
                Ball second = balls.get(j);
                if (!first.isPocketed() && !second.isPocketed()) {
                    resolvePair(first, second);
                }
            }
        }
    }

    private void resolvePair(Ball first, Ball second) {
        Vec2 offset = second.position().minus(first.position());
        double distance = offset.length();
        double minimumDistance = 2 * table.ballRadius();
        if (distance >= minimumDistance) {
            return;
        }
        Vec2 normal = distance == 0 ? new Vec2(1, 0) : offset.times(1 / distance);
        separate(first, second, normal, minimumDistance - distance);

        double approachSpeed = first.velocity().minus(second.velocity()).dot(normal);
        if (approachSpeed <= 0) {
            return;
        }
        double impulse = (1 + BALL_RESTITUTION) / 2 * approachSpeed;
        first.setVelocity(first.velocity().minus(normal.times(impulse)));
        second.setVelocity(second.velocity().plus(normal.times(impulse)));
        listener.onBallCollision(first, second);
    }

    private void separate(Ball first, Ball second, Vec2 normal, double overlap) {
        Vec2 half = normal.times(overlap / 2);
        first.setPosition(first.position().minus(half));
        second.setPosition(second.position().plus(half));
    }
}
