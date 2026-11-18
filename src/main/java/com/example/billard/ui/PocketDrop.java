package com.example.billard.ui;

import com.example.billard.model.Orientation;
import com.example.billard.model.Vec2;

/**
 * One ball falling into a pocket, as the table shows it after the rules have already counted it as pocketed. The
 * ball carries on across the hole toward its back, rolling as it goes, then sinks: it shrinks with distance, darkens
 * as it leaves the lamp's light and slips under the lip, which is the edge of the hole.
 *
 * @param entry       where the ball's centre was when the pocket caught it
 * @param pocket      centre of the pocket's hole
 * @param holeRadius  radius of the hole
 * @param ballRadius  radius of the ball on the cloth
 * @param speed       how fast the ball was rolling as it dropped
 * @param orientation how the ball was turned as it dropped
 */
record PocketDrop(int number, Vec2 entry, Vec2 pocket, double holeRadius, double ballRadius, double speed,
                  Orientation orientation) {

    /** Even a ball that barely trickles in drops at least this fast, so it never hangs over the hole. */
    private static final double MIN_SPEED = 220;
    /** At this speed and above the ball runs right to the back of the hole before it is gone. */
    private static final double FULL_OVERSHOOT_SPEED = 600;
    private static final double MAX_TRAVEL_SECONDS = 0.3;
    private static final double SINK_SECONDS = 0.22;
    /** How small the ball looks at the bottom, as a share of its size on the cloth. */
    private static final double SMALLEST_SCALE = 0.6;

    /** What to draw at one moment of the drop. */
    record Frame(Vec2 centre, double radius, Orientation orientation, double darkness, double clipRadius) {
    }

    private Vec2 direction() {
        Vec2 toPocket = pocket.minus(entry);
        double length = toPocket.length();
        return length < 1e-9 ? new Vec2(1, 0) : toPocket.times(1 / length);
    }

    /** From where the ball was caught to where it comes to rest against the back of the hole. */
    private double pathLength() {
        double overshoot = Math.min(1, speed / FULL_OVERSHOOT_SPEED) * Math.max(0, holeRadius - ballRadius);
        return pocket.distanceTo(entry) + overshoot;
    }

    /** The ball eases to a stop, starting at its own speed: a quarter sine wave covers the path. */
    private double travelSeconds() {
        return Math.min(MAX_TRAVEL_SECONDS, Math.PI / 2 * pathLength() / Math.max(speed, MIN_SPEED));
    }

    double duration() {
        return travelSeconds() + SINK_SECONDS;
    }

    boolean isFinished(double elapsed) {
        return elapsed >= duration();
    }

    Frame frameAt(double elapsed) {
        double travel = travelSeconds();
        double progress = travel == 0 ? 1 : Math.clamp(elapsed / travel, 0, 1);
        Vec2 moved = direction().times(pathLength() * Math.sin(progress * Math.PI / 2));
        double sink = smoothStep(travel * 0.4, duration(), elapsed);
        return new Frame(
                entry.plus(moved),
                ballRadius * (1 - (1 - SMALLEST_SCALE) * sink),
                orientation.rolled(moved, ballRadius),
                sink,
                // Wide enough at first not to cut the ball where it overhangs the hole, closing to the lip as it sinks.
                holeRadius + ballRadius * (1 - sink));
    }

    private static double smoothStep(double from, double to, double value) {
        double t = Math.clamp((value - from) / (to - from), 0, 1);
        return t * t * (3 - 2 * t);
    }
}
