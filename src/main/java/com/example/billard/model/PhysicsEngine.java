package com.example.billard.model;

import java.util.List;

/**
 * Fixed-step simulation of balls rolling, bouncing and dropping into pockets. Balls decelerate at a constant
 * rate and exchange momentum along the line of centres. Spin is simplified to two effects: follow or draw is
 * stored on the ball and released as a velocity change when it hits another ball, and sideways spin (english)
 * kicks the ball along the cushion when it bounces. Both fade slowly while the ball travels.
 */
public final class PhysicsEngine {

    public static final double FIXED_STEP = 1.0 / 240.0;

    private static final double ROLLING_DECELERATION = 520.0;
    private static final double STOP_SPEED = 10.0;
    private static final double BALL_RESTITUTION = 0.96;
    private static final double CUSHION_RESTITUTION = 0.76;
    private static final double SPIN_FADE_PER_SECOND = 0.5;
    private static final double ROLL_SPIN_RELEASE_RATIO = 0.7;
    private static final double CUSHION_GRIP = 0.3;
    private static final double CUSHION_SIDE_SPIN_RETENTION = 0.5;
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
        fadeSpin(ball);
        double speed = ball.velocity().length();
        if (speed == 0) {
            return;
        }
        double slowedSpeed = speed - ROLLING_DECELERATION * FIXED_STEP;
        if (slowedSpeed < STOP_SPEED) {
            ball.stop();
        } else {
            ball.setVelocity(ball.velocity().times(slowedSpeed / speed));
        }
    }

    private void fadeSpin(Ball ball) {
        double keep = 1 - SPIN_FADE_PER_SECOND * FIXED_STEP;
        ball.setRollSpin(ball.rollSpin().times(keep));
        ball.setSideSpin(ball.sideSpin() * keep);
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
        double sideKickX = 0;
        double sideKickY = 0;
        boolean bounced = false;

        if (x < radius || x > table.width() - radius) {
            boolean movingIntoWall = (x < radius) == (vx < 0) && vx != 0;
            double wallSign = x < radius ? -1 : 1;
            x = Math.max(radius, Math.min(table.width() - radius, x));
            if (movingIntoWall) {
                sideKickY = wallSign * englishKick(ball, vx);
                vx = -vx * CUSHION_RESTITUTION;
                bounced = true;
            }
        }
        if (y < radius || y > table.height() - radius) {
            boolean movingIntoWall = (y < radius) == (vy < 0) && vy != 0;
            double wallSign = y < radius ? -1 : 1;
            y = Math.max(radius, Math.min(table.height() - radius, y));
            if (movingIntoWall) {
                sideKickX = -wallSign * englishKick(ball, vy);
                vy = -vy * CUSHION_RESTITUTION;
                bounced = true;
            }
        }
        ball.setPosition(new Vec2(x, y));
        ball.setVelocity(new Vec2(vx + sideKickX, vy + sideKickY));
        if (bounced) {
            ball.setSideSpin(ball.sideSpin() * CUSHION_SIDE_SPIN_RETENTION);
            listener.onCushionHit(ball);
        }
    }

    /**
     * Speed gained along the cushion from the ball's sideways spin: the cushion's friction turns the spin into
     * motion, but never more than a fraction of the speed the ball hit it with.
     */
    private double englishKick(Ball ball, double speedIntoCushion) {
        double limit = CUSHION_GRIP * Math.abs(speedIntoCushion);
        return Math.max(-limit, Math.min(limit, -ball.sideSpin()));
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
        releaseRollSpin(first);
        releaseRollSpin(second);
        listener.onBallCollision(first, second);
    }

    /** Follow pushes the ball on after the hit, draw pulls it back; either way the spin is used up. */
    private void releaseRollSpin(Ball ball) {
        ball.setVelocity(ball.velocity().plus(ball.rollSpin().times(ROLL_SPIN_RELEASE_RATIO)));
        ball.setRollSpin(Vec2.ZERO);
    }

    private void separate(Ball first, Ball second, Vec2 normal, double overlap) {
        Vec2 half = normal.times(overlap / 2);
        first.setPosition(first.position().minus(half));
        second.setPosition(second.position().plus(half));
    }
}
