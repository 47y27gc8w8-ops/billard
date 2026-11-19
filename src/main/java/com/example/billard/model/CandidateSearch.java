package com.example.billard.model;

import java.util.List;

/**
 * Which shots the planner tries before it chooses. A wider search finds banks, kicks and combinations, and finer
 * control of where the cue ball ends up, at the cost of more simulations per turn.
 *
 * @param potPowers          powers tried for each direct pot
 * @param potTips            cue-ball spins tried for each direct pot
 * @param sweepDirections    how many evenly spaced directions the fallback sweep tries all round the cue ball
 * @param sweepPowers        powers tried in each sweep direction
 * @param placementDistances how far behind a ball the cue ball is put for a straight-in pot, with ball in hand
 * @param placementPowers    powers tried for each of those straight-in pots
 */
record CandidateSearch(
        List<Double> potPowers,
        List<TipContact> potTips,
        int sweepDirections,
        List<Double> sweepPowers,
        List<Double> placementDistances,
        List<Double> placementPowers) {

    private static final TipContact DRAW = new TipContact(0, -0.5);
    private static final TipContact DEEP_DRAW = new TipContact(0, -0.9);
    private static final TipContact FOLLOW = new TipContact(0, 0.4);
    private static final TipContact LEFT = new TipContact(-0.6, 0);
    private static final TipContact RIGHT = new TipContact(0.6, 0);

    /** A few hundred shots: enough to find the obvious pots and a safety. */
    static final CandidateSearch STANDARD = new CandidateSearch(
            List.of(0.3, 0.5, 0.75),
            List.of(TipContact.CENTER, DRAW, FOLLOW),
            72,
            List.of(0.55),
            List.of(70.0, 160.0),
            List.of(0.35, 0.6));

    /** A few thousand shots: every pot, bank and kick the bot can find, with spin to place the cue ball. */
    static final CandidateSearch EXHAUSTIVE = new CandidateSearch(
            List.of(0.2, 0.3, 0.4, 0.5, 0.65, 0.8, 1.0),
            List.of(TipContact.CENTER, DRAW, DEEP_DRAW, FOLLOW, LEFT, RIGHT),
            720,
            List.of(0.3, 0.55, 0.85),
            List.of(50.0, 90.0, 160.0, 260.0),
            List.of(0.25, 0.4, 0.6));

    CandidateSearch {
        potPowers = List.copyOf(potPowers);
        potTips = List.copyOf(potTips);
        sweepPowers = List.copyOf(sweepPowers);
        placementDistances = List.copyOf(placementDistances);
        placementPowers = List.copyOf(placementPowers);
    }
}
