package com.example.billard.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.billard.audio.Sample;
import com.example.billard.audio.SoundOutput;
import com.example.billard.model.Ball;
import com.example.billard.model.Vec2;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SoundEffectsTest {

    private static final double TABLE_WIDTH = 1000;

    private record Played(Sample sample, double gain, double pan, double rate) {
    }

    private final List<Played> played = new ArrayList<>();
    private final SoundOutput output = (sample, gain, pan, rate) -> played.add(new Played(sample, gain, pan, rate));

    private static List<Sample> takes(int count) {
        List<Sample> takes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            takes.add(new Sample(new float[] {0, 0}));
        }
        return List.copyOf(takes);
    }

    private final SoundBank bank = new SoundBank(takes(2), takes(2), takes(3), takes(2), takes(2), takes(1), takes(1));
    private final SoundEffects effects = new SoundEffects(output, bank, TABLE_WIDTH, new Random(7));

    private static Ball ballAt(double x) {
        return new Ball(1, new Vec2(x, 250));
    }

    @Test
    void clacksWaitForTheEndOfTheFrame() {
        effects.onBallCollision(ballAt(400), ballAt(420), 800);

        assertTrue(played.isEmpty());
        effects.playQueuedClacks();
        assertEquals(1, played.size());
    }

    @Test
    void breakPileUpPlaysOneRackBreakInsteadOfManyClacks() {
        for (int i = 0; i < 24; i++) {
            effects.onBallCollision(ballAt(700), ballAt(720), 100 + i * 90);
        }

        effects.playQueuedClacks();

        assertEquals(1, played.size());
        assertTrue(bank.rackBreaks().contains(played.get(0).sample()));
    }

    @Test
    void ordinaryFramePlaysOnlyItsLoudestClacks() {
        double[] speeds = {200, 900, 50, 600, 300};
        for (double speed : speeds) {
            effects.onBallCollision(ballAt(500), ballAt(520), speed);
        }

        effects.playQueuedClacks();

        assertEquals(SoundEffects.MAX_CLACKS_PER_FRAME, played.size());
        assertTrue(played.get(0).gain() > played.get(1).gain());
        assertTrue(played.get(1).gain() > played.get(2).gain());
    }

    @Test
    void queueIsEmptyAfterPlaying() {
        effects.onBallCollision(ballAt(500), ballAt(520), 800);
        effects.playQueuedClacks();
        played.clear();

        effects.playQueuedClacks();

        assertTrue(played.isEmpty());
    }

    @Test
    void hardHitsUseHardClacksAndSoftHitsSoftOnes() {
        effects.onBallCollision(ballAt(500), ballAt(520), SoundEffects.HARD_IMPACT_SPEED + 100);
        effects.playQueuedClacks();
        effects.onBallCollision(ballAt(500), ballAt(520), SoundEffects.HARD_IMPACT_SPEED - 100);
        effects.playQueuedClacks();

        assertTrue(bank.hardClacks().contains(played.get(0).sample()));
        assertTrue(bank.softClacks().contains(played.get(1).sample()));
    }

    @Test
    void barelyTouchingBallsMakeNoSound() {
        effects.onBallCollision(ballAt(500), ballAt(520), SoundEffects.QUIETEST_AUDIBLE_SPEED / 2);
        effects.onCushionHit(ballAt(10), SoundEffects.QUIETEST_AUDIBLE_SPEED / 2);
        effects.playQueuedClacks();

        assertTrue(played.isEmpty());
    }

    @Test
    void harderCushionHitsAreLouder() {
        effects.onCushionHit(ballAt(10), 200);
        effects.onCushionHit(ballAt(10), 1200);

        assertTrue(played.get(1).gain() > played.get(0).gain());
    }

    @Test
    void soundsComeFromTheirSideOfTheTable() {
        effects.onCushionHit(ballAt(0), 500);
        effects.onCushionHit(ballAt(TABLE_WIDTH), 500);
        effects.onPocketed(ballAt(TABLE_WIDTH / 2), 500);

        assertTrue(played.get(0).pan() < 0);
        assertTrue(played.get(1).pan() > 0);
        assertEquals(0.0, played.get(2).pan(), 1e-9);
    }

    @Test
    void slowBallStillDropsAudiblyIntoThePocket() {
        effects.onPocketed(ballAt(0), 1);

        assertTrue(played.get(0).gain() > 0.3);
    }

    @Test
    void harderCueStrikesAreLouder() {
        effects.onCueStruck(0.1);
        effects.onCueStruck(1.0);

        assertTrue(played.get(1).gain() > played.get(0).gain());
    }

    @Test
    void sameTakeIsNeverPlayedTwiceInARow() {
        for (int i = 0; i < 30; i++) {
            effects.onCushionHit(ballAt(500), 500);
        }

        for (int i = 1; i < played.size(); i++) {
            assertNotEquals(played.get(i - 1).sample(), played.get(i).sample());
        }
    }

    @Test
    void pitchVariesOnlySlightly() {
        for (int i = 0; i < 30; i++) {
            effects.onCushionHit(ballAt(500), 500);
        }

        assertTrue(played.stream().allMatch(p -> p.rate() > 0.95 && p.rate() < 1.05));
    }

    @Test
    void standardBankHasRecordingsForEverySound() {
        SoundBank standard = SoundBank.standard();

        assertEquals(4, standard.cueStrikes().size());
        assertEquals(3, standard.softClacks().size());
        assertEquals(4, standard.hardClacks().size());
        assertEquals(3, standard.cushionThuds().size());
        assertEquals(3, standard.pocketDrops().size());
        assertEquals(2, standard.rackBreaks().size());
        assertEquals(1, standard.fanfares().size());
    }
}
