package com.example.billard.model;

import java.util.random.RandomGenerator;

/**
 * How steady the bot's cue arm is. It picks its shot perfectly, then plays it with a random error of about this
 * size, so long and thin shots miss more often than short straight ones, as they would for a person.
 *
 * @param aimError   typical error in the shot direction, in radians (one standard deviation)
 * @param powerError typical error in the shot power, as a fraction of full power
 */
public record BotSkill(double aimError, double powerError) {

    /** A solid club player: pots most straight shots, misses some long cuts. */
    public static final BotSkill STANDARD = new BotSkill(Math.toRadians(0.5), 0.05);

    /** {@code shot} as the bot's hand actually plays it. */
    Shot blur(Shot shot, RandomGenerator random) {
        double angle = shot.angle() + random.nextGaussian() * aimError;
        double power = shot.power() + random.nextGaussian() * powerError;
        return new Shot(angle, Math.max(0, Math.min(1, power)), shot.tip());
    }
}
