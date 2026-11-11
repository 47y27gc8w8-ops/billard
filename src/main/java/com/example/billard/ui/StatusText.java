package com.example.billard.ui;

import com.example.billard.model.GameSession;
import com.example.billard.model.TurnOutcome;
import java.util.ArrayList;
import java.util.List;

/** Plain-language messages for the status line and the game-over screen. */
final class StatusText {

    private StatusText() {
    }

    static String describe(GameSession session) {
        String player = session.currentPlayer().label();
        return switch (session.phase()) {
            case BALLS_MOVING -> "Balls rolling.";
            case PLACING_CUE -> recap(session) + player + " has ball in hand. Place the cue ball, then click or press Enter.";
            case AIMING -> session.isAimLocked()
                    ? "Aim locked. Set the power, then shoot. Right click to aim again."
                    : recap(session) + (session.match().isBreakShot() ? player + " to break." : player + " to shoot.");
            case GAME_OVER -> gameOverReason(session);
        };
    }

    static String winnerHeadline(GameSession session) {
        return session.winner().map(winner -> winner.label() + " wins").orElse("Match over");
    }

    static String gameOverReason(GameSession session) {
        return session.lastOutcome().map(StatusText::endingOf).orElse("");
    }

    private static String endingOf(TurnOutcome outcome) {
        String shooter = outcome.shooter().label();
        boolean shooterWon = outcome.winner().filter(outcome.shooter()::equals).isPresent();
        if (shooterWon) {
            return shooter + " potted the 8-ball and cleared the table.";
        }
        return shooter + " lost by " + (outcome.foul().isFoul() ? "fouling on the 8-ball." : "potting the 8-ball too early.");
    }

    private static String recap(GameSession session) {
        return session.lastOutcome().map(StatusText::recapOf).orElse("");
    }

    private static String recapOf(TurnOutcome outcome) {
        List<String> sentences = new ArrayList<>();
        outcome.assignedGroup().ifPresent(group ->
                sentences.add(outcome.shooter().label() + " takes " + group.groupName().toLowerCase() + "."));
        if (outcome.foul().isFoul()) {
            sentences.add("Foul: " + outcome.foul().description() + ".");
        }
        if (outcome.eightRespotted()) {
            sentences.add("The 8-ball is re-spotted.");
        }
        return sentences.isEmpty() ? "" : String.join(" ", sentences) + " ";
    }
}
