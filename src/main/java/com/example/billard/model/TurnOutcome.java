package com.example.billard.model;

import java.util.Optional;

/**
 * What a shot changed.
 *
 * @param shooter         who took the shot
 * @param nextPlayer      who shoots next
 * @param foul            the foul committed, or {@link Foul#NONE}
 * @param winner          set when the match ended with this shot
 * @param assignedGroup   the shooter's new group when this shot closed the open table
 * @param eightRespotted  true when the eight-ball dropped on the break and goes back on the table
 */
public record TurnOutcome(
        Player shooter,
        Player nextPlayer,
        Foul foul,
        Optional<Player> winner,
        Optional<BallKind> assignedGroup,
        boolean eightRespotted) {

    public boolean ballInHand() {
        return foul.isFoul();
    }
}
