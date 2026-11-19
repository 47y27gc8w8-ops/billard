package com.example.billard.model;

/**
 * One stroke of the cue: the direction, how hard, and where the tip meets the cue ball.
 *
 * @param angle the direction the cue ball is sent in, in radians
 * @param power from 0 to 1, a fraction of the hardest possible shot
 * @param tip   where the cue strikes the cue ball, which decides its spin
 */
public record Shot(double angle, double power, TipContact tip) {

    private static final double MAX_SHOT_SPEED = 2400.0;

    /** Sets the cue ball rolling and spinning as this shot would. */
    public void strike(Ball cueBall) {
        Vec2 direction = Vec2.fromAngle(angle);
        double speed = power * MAX_SHOT_SPEED;
        cueBall.setVelocity(direction.times(speed));
        cueBall.setRollSpin(tip.rollSpin(direction, speed));
        cueBall.setSideSpin(tip.sideSpin(speed));
    }
}
