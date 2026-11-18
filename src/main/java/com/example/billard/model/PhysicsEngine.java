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
    /**
     * Clockwise english runs one way along the end cushions (left and right) and the other way along the long ones,
     * as seen from each cushion's own side.
     */
    private static final double END_CUSHION_KICK = 1;
    private static final double LONG_CUSHION_KICK = -1;

    /**
     * Where a ball ends up across one axis after meeting a cushion there: its position and speed along the axis,
     * the speed english adds along the cushion (across the other axis), and how hard it hit (0 if it did not).
     */
    private record Bounce(double position, double velocity, double sideKick, double impactSpeed) {
    }

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
        Vec2 travel = ball.velocity().times(FIXED_STEP);
        ball.setPosition(ball.position().plus(travel));
        turn(ball, travel);
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

    /** Rolls the ball over the distance it just travelled and turns it on the spot by its sideways spin. */
    private void turn(Ball ball, Vec2 travel) {
        double radius = table.ballRadius();
        ball.setOrientation(ball.orientation()
                .rolled(travel, radius)
                .spun(ball.sideSpin() * FIXED_STEP / radius));
    }

    private void fadeSpin(Ball ball) {
        double keep = 1 - SPIN_FADE_PER_SECOND * FIXED_STEP;
        ball.setRollSpin(ball.rollSpin().times(keep));
        ball.setSideSpin(ball.sideSpin() * keep);
    }

    private boolean tryPocket(Ball ball) {
        for (Table.Pocket pocket : table.pockets()) {
            if (pocket.center().distanceTo(ball.position()) < pocket.captureRadius()) {
                double speed = ball.velocity().length();
                ball.pocket();
                listener.onPocketed(ball, speed);
                return true;
            }
        }
        return false;
    }

    /**
     * Keeps the ball inside the cushions and bounces it off any it is moving into. English pushes the ball along
     * the cushion it hits, which is across the other axis: a bounce in x kicks the ball in y and the other way round.
     */
    private void bounceOffCushions(Ball ball) {
        if (table.containsCenter(ball.position())) {
            return;
        }
        Bounce acrossX = bounceAlong(ball, ball.position().x(), ball.velocity().x(), table.width(), END_CUSHION_KICK);
        Bounce acrossY = bounceAlong(ball, ball.position().y(), ball.velocity().y(), table.height(), LONG_CUSHION_KICK);
        ball.setPosition(new Vec2(acrossX.position(), acrossY.position()));
        ball.setVelocity(new Vec2(acrossX.velocity() + acrossY.sideKick(), acrossY.velocity() + acrossX.sideKick()));
        double impactSpeed = Math.max(acrossX.impactSpeed(), acrossY.impactSpeed());
        if (impactSpeed > 0) {
            ball.setSideSpin(ball.sideSpin() * CUSHION_SIDE_SPIN_RETENTION);
            listener.onCushionHit(ball, impactSpeed);
        }
    }

    /**
     * One axis against its two cushions, at 0 and {@code extent}.
     *
     * @param kickSign which way english kicks along this axis's cushions: {@link #END_CUSHION_KICK} or
     *                 {@link #LONG_CUSHION_KICK}
     */
    private Bounce bounceAlong(Ball ball, double position, double velocity, double extent, double kickSign) {
        double radius = table.ballRadius();
        boolean pastNearCushion = position < radius;
        boolean pastFarCushion = position > extent - radius;
        if (!pastNearCushion && !pastFarCushion) {
            return new Bounce(position, velocity, 0, 0);
        }
        double inside = Math.max(radius, Math.min(extent - radius, position));
        boolean movingIntoCushion = pastNearCushion == (velocity < 0) && velocity != 0;
        if (!movingIntoCushion) {
            return new Bounce(inside, velocity, 0, 0);
        }
        double wallSign = pastNearCushion ? -1 : 1;
        double sideKick = kickSign * wallSign * englishKick(ball, velocity);
        return new Bounce(inside, -velocity * CUSHION_RESTITUTION, sideKick, Math.abs(velocity));
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
        listener.onBallCollision(first, second, approachSpeed);
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
