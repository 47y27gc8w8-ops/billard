package com.example.billard.model;

import java.util.random.RandomGenerator;

/**
 * How well the bot plays. Every level plans its shots with the same simulation of the real table; they differ in
 * how steady its hand is, how much it cares where the cue ball stops, and how many shots it tries before it
 * chooses.
 */
public enum BotLevel {

    /** Misses most pots and gives no thought to the next shot. You would have to try to lose. */
    EASY("Easy", Math.toRadians(4.5), 0.25, 0, CandidateSearch.STANDARD),
    /** Pots the easy ones and misses plenty. Newer players will struggle; regular players should win. */
    MEDIUM("Medium", Math.toRadians(1.0), 0.08, 2, CandidateSearch.STANDARD),
    /** Pots most shots and plays for position, but misses now and then. Hard to beat. */
    HARD("Hard", Math.toRadians(0.25), 0.03, 5, CandidateSearch.STANDARD),
    /** Never misses: the shot it plays is exactly the shot it simulated, chosen from thousands. */
    BOSS("Boss", 0, 0, 5, CandidateSearch.EXHAUSTIVE);

    private final String label;
    private final double aimError;
    private final double powerError;
    private final double leaveWeight;
    private final CandidateSearch search;

    /**
     * @param aimError    typical error in the shot direction, in radians (one standard deviation)
     * @param powerError  typical error in the shot power, as a fraction of full power
     * @param leaveWeight how much an easy next shot for itself, or none for the opponent, is worth; 0 ignores it
     */
    BotLevel(String label, double aimError, double powerError, double leaveWeight, CandidateSearch search) {
        this.label = label;
        this.aimError = aimError;
        this.powerError = powerError;
        this.leaveWeight = leaveWeight;
        this.search = search;
    }

    public String label() {
        return label;
    }

    double aimError() {
        return aimError;
    }

    double leaveWeight() {
        return leaveWeight;
    }

    CandidateSearch search() {
        return search;
    }

    /** {@code shot} as the bot's hand actually plays it: blurred by its aim and power error. */
    Shot blur(Shot shot, RandomGenerator random) {
        double angle = shot.angle() + random.nextGaussian() * aimError;
        double power = shot.power() + random.nextGaussian() * powerError;
        return new Shot(angle, Math.max(0, Math.min(1, power)), shot.tip());
    }
}
