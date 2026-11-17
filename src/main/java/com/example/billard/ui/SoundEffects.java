package com.example.billard.ui;

import com.example.billard.audio.Sample;
import com.example.billard.audio.SoundOutput;
import com.example.billard.model.Ball;
import com.example.billard.model.PhysicsListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Turns what happens on the table into sound. Loudness follows how hard things hit, and each sound is placed left
 * or right by where on the table it happened.
 *
 * <p>Ball-on-ball clacks are gathered for one animation frame and played by {@link #playQueuedClacks()}: the
 * simulation can produce two dozen collisions within a single frame (the break), and starting that many clacks at
 * the same instant sounds like one harsh blast. Instead, a pile-up plays a recorded rack break, and an ordinary frame
 * plays only its few loudest clacks.
 */
final class SoundEffects implements PhysicsListener {

    static final double FULL_IMPACT_SPEED = 1600;
    static final double HARD_IMPACT_SPEED = 450;
    static final double QUIETEST_AUDIBLE_SPEED = 15;
    static final int RACK_BREAK_COLLISIONS = 6;
    static final int MAX_CLACKS_PER_FRAME = 3;

    private static final double LOUDNESS_CURVE = 0.7;
    private static final double PITCH_SPREAD = 0.04;
    private static final double STEREO_WIDTH = 0.6;
    private static final double CLACK_GAIN = 1.0;
    private static final double CUE_GAIN = 0.8;
    private static final double QUIETEST_CUE = 0.3;
    private static final double CUSHION_GAIN = 0.8;
    private static final double POCKET_GAIN = 0.75;
    private static final double QUIETEST_POCKET = 0.45;
    private static final double BREAK_GAIN = 1.0;
    private static final double FANFARE_GAIN = 0.6;

    private record Clack(double speed, double x) {
    }

    private final SoundOutput output;
    private final SoundBank bank;
    private final double tableWidth;
    private final RandomGenerator random;
    private final List<Clack> queuedClacks = new ArrayList<>();
    private final Map<List<Sample>, Sample> lastTake = new IdentityHashMap<>();

    SoundEffects(SoundOutput output, SoundBank bank, double tableWidth, RandomGenerator random) {
        this.output = output;
        this.bank = bank;
        this.tableWidth = tableWidth;
        this.random = random;
    }

    @Override
    public void onCueStruck(double power) {
        play(bank.cueStrikes(), CUE_GAIN * (QUIETEST_CUE + (1 - QUIETEST_CUE) * power), 0);
    }

    @Override
    public void onBallCollision(Ball first, Ball second, double impactSpeed) {
        if (impactSpeed >= QUIETEST_AUDIBLE_SPEED) {
            double x = (first.position().x() + second.position().x()) / 2;
            queuedClacks.add(new Clack(impactSpeed, x));
        }
    }

    @Override
    public void onCushionHit(Ball ball, double impactSpeed) {
        if (impactSpeed >= QUIETEST_AUDIBLE_SPEED) {
            play(bank.cushionThuds(), CUSHION_GAIN * loudness(impactSpeed), panOf(ball.position().x()));
        }
    }

    @Override
    public void onPocketed(Ball ball, double speed) {
        double gain = POCKET_GAIN * Math.max(QUIETEST_POCKET, loudness(speed));
        play(bank.pocketDrops(), gain, panOf(ball.position().x()));
    }

    /** Plays the clacks gathered since the last call. Call once per frame, right after advancing the game. */
    void playQueuedClacks() {
        queuedClacks.sort(Comparator.comparingDouble(Clack::speed).reversed());
        if (isRackBreak()) {
            Clack loudest = queuedClacks.get(0);
            play(bank.rackBreaks(), BREAK_GAIN * loudness(loudest.speed()), panOf(loudest.x()));
        } else {
            queuedClacks.stream().limit(MAX_CLACKS_PER_FRAME).forEach(this::playClack);
        }
        queuedClacks.clear();
    }

    void playFanfare() {
        play(bank.fanfares(), FANFARE_GAIN, 0);
    }

    private boolean isRackBreak() {
        return queuedClacks.size() >= RACK_BREAK_COLLISIONS
                && queuedClacks.get(0).speed() >= HARD_IMPACT_SPEED
                && !bank.rackBreaks().isEmpty();
    }

    private void playClack(Clack clack) {
        List<Sample> takes = clack.speed() >= HARD_IMPACT_SPEED ? bank.hardClacks() : bank.softClacks();
        play(takes, CLACK_GAIN * loudness(clack.speed()), panOf(clack.x()));
    }

    /** 0 to 1, rising steeply for soft hits and flattening out for hard ones, roughly as the ear hears it. */
    private static double loudness(double speed) {
        return Math.pow(Math.min(1.0, speed / FULL_IMPACT_SPEED), LOUDNESS_CURVE);
    }

    private double panOf(double x) {
        return STEREO_WIDTH * (2 * Math.clamp(x / tableWidth, 0.0, 1.0) - 1);
    }

    private void play(List<Sample> takes, double gain, double pan) {
        if (takes.isEmpty()) {
            return;
        }
        double rate = 1 + PITCH_SPREAD * (2 * random.nextDouble() - 1);
        output.play(pickTake(takes), gain, pan, rate);
    }

    /** A random take, but never the same one twice in a row. */
    private Sample pickTake(List<Sample> takes) {
        Sample previous = lastTake.get(takes);
        Sample next = takes.get(random.nextInt(takes.size()));
        if (next == previous && takes.size() > 1) {
            next = takes.get((takes.indexOf(next) + 1 + random.nextInt(takes.size() - 1)) % takes.size());
        }
        lastTake.put(takes, next);
        return next;
    }
}
