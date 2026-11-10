package com.example.billard.model;

import static com.example.billard.model.BallKind.CUE;
import static com.example.billard.model.BallKind.EIGHT;
import static com.example.billard.model.BallKind.SOLID;
import static com.example.billard.model.BallKind.STRIPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EightBallMatchTest {

    private final EightBallMatch match = new EightBallMatch();

    private static ShotReport shot(BallKind firstContact, boolean rail, BallKind... pocketed) {
        return new ShotReport(Optional.ofNullable(firstContact), List.of(pocketed), rail);
    }

    private void playOpeningPot() {
        match.resolveShot(shot(SOLID, true, SOLID));
    }

    @Test
    void firstLegalPotOnOpenTableAssignsGroupsAndKeepsTheTurn() {
        TurnOutcome outcome = match.resolveShot(shot(SOLID, true, STRIPE));

        assertEquals(Optional.of(STRIPE), outcome.assignedGroup());
        assertEquals(Optional.of(STRIPE), match.groupOf(Player.ONE));
        assertEquals(Optional.of(SOLID), match.groupOf(Player.TWO));
        assertEquals(Player.ONE, outcome.nextPlayer());
    }

    @Test
    void missingEveryPotPassesTheTurnWithoutAFoulWhenARailIsHit() {
        TurnOutcome outcome = match.resolveShot(shot(SOLID, true));

        assertEquals(Foul.NONE, outcome.foul());
        assertEquals(Player.TWO, outcome.nextPlayer());
        assertTrue(match.isTableOpen());
    }

    @Test
    void pocketingTheCueBallIsAFoulWithBallInHand() {
        TurnOutcome outcome = match.resolveShot(shot(SOLID, true, SOLID, CUE));

        assertEquals(Foul.CUE_POCKETED, outcome.foul());
        assertTrue(outcome.ballInHand());
        assertEquals(Player.TWO, outcome.nextPlayer());
        assertTrue(outcome.assignedGroup().isEmpty(), "no group is assigned on a foul");
    }

    @Test
    void hittingNothingIsAFoul() {
        assertEquals(Foul.NO_CONTACT, match.resolveShot(shot(null, true)).foul());
    }

    @Test
    void hittingTheEightFirstOnAnOpenTableIsAFoul() {
        assertEquals(Foul.WRONG_BALL_FIRST, match.resolveShot(shot(EIGHT, true)).foul());
    }

    @Test
    void hittingTheOpponentsGroupFirstIsAFoul() {
        playOpeningPot();

        assertEquals(Foul.WRONG_BALL_FIRST, match.resolveShot(shot(STRIPE, true, SOLID)).foul());
    }

    @Test
    void noPotAndNoCushionAfterContactIsAFoul() {
        assertEquals(Foul.NO_RAIL, match.resolveShot(shot(SOLID, false)).foul());
    }

    @Test
    void pottingTheEightBeforeClearingTheGroupLosesTheMatch() {
        playOpeningPot();

        TurnOutcome outcome = match.resolveShot(shot(SOLID, true, EIGHT));

        assertEquals(Optional.of(Player.TWO), outcome.winner());
    }

    @Test
    void clearingTheGroupThenPottingTheEightWins() {
        playOpeningPot();
        for (int i = 0; i < 6; i++) {
            match.resolveShot(shot(SOLID, true, SOLID));
        }

        TurnOutcome outcome = match.resolveShot(shot(EIGHT, true, EIGHT));

        assertEquals(Optional.of(Player.ONE), outcome.winner());
    }

    @Test
    void pottingTheEightWhileScratchingLosesEvenWhenShootingAtIt() {
        playOpeningPot();
        for (int i = 0; i < 6; i++) {
            match.resolveShot(shot(SOLID, true, SOLID));
        }

        TurnOutcome outcome = match.resolveShot(shot(EIGHT, true, EIGHT, CUE));

        assertEquals(Optional.of(Player.TWO), outcome.winner());
    }

    @Test
    void eightOnTheBreakIsRespottedAndDoesNotEndTheMatch() {
        TurnOutcome outcome = match.resolveShot(shot(SOLID, true, EIGHT));

        assertTrue(outcome.eightRespotted());
        assertTrue(outcome.winner().isEmpty());
        assertFalse(match.isBreakShot());
    }

    @Test
    void afterClearingTheGroupOnlyTheEightIsALegalFirstContact() {
        playOpeningPot();
        for (int i = 0; i < 6; i++) {
            match.resolveShot(shot(SOLID, true, SOLID));
        }

        assertEquals(Foul.WRONG_BALL_FIRST, match.resolveShot(shot(SOLID, true)).foul());
    }
}
