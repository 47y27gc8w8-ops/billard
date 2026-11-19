package com.example.billard.ui;

import com.example.billard.model.GameSession;
import com.example.billard.model.Player;
import com.example.billard.model.TurnOutcome;
import java.util.ArrayList;
import java.util.List;

/** Plain-language messages for the status line and the game-over screen, naming players as {@code mode} does. */
final class StatusText {

    private StatusText() {
    }

    static String describe(GameSession session, MatchMode mode) {
        Player current = session.currentPlayer();
        String player = mode.nameOf(current);
        String recap = recap(session, mode);
        return switch (session.phase()) {
            case BALLS_MOVING -> "Balls rolling.";
            case PLACING_CUE -> mode.isBot(current)
                    ? recap + player + " has ball in hand."
                    : recap + player + " has ball in hand. Place the cue ball, then click or press Enter.";
            case AIMING -> describeAiming(session, mode, recap);
            case GAME_OVER -> gameOverReason(session, mode);
        };
    }

    private static String describeAiming(GameSession session, MatchMode mode, String recap) {
        Player current = session.currentPlayer();
        String player = mode.nameOf(current);
        if (mode.isBot(current)) {
            return recap + player + " is lining up a shot.";
        }
        if (session.isAimLocked()) {
            return "Aim locked. Set the power, then shoot. Right click to aim again.";
        }
        return recap + player + (session.match().isBreakShot() ? " to break." : " to shoot.");
    }

    static String winnerHeadline(GameSession session, MatchMode mode) {
        return session.winner().map(winner -> mode.nameOf(winner) + " wins").orElse("Match over");
    }

    static String gameOverReason(GameSession session, MatchMode mode) {
        return session.lastOutcome().map(outcome -> endingOf(outcome, mode)).orElse("");
    }

    private static String endingOf(TurnOutcome outcome, MatchMode mode) {
        String shooter = mode.nameOf(outcome.shooter());
        boolean shooterWon = outcome.winner().filter(outcome.shooter()::equals).isPresent();
        if (shooterWon) {
            return shooter + " potted the 8-ball and cleared the table.";
        }
        String mistake = outcome.foul().isFoul() ? "fouling on the 8-ball." : "potting the 8-ball too early.";
        return shooter + " lost by " + mistake;
    }

    private static String recap(GameSession session, MatchMode mode) {
        return session.lastOutcome().map(outcome -> recapOf(outcome, mode)).orElse("");
    }

    private static String recapOf(TurnOutcome outcome, MatchMode mode) {
        List<String> sentences = new ArrayList<>();
        outcome.assignedGroup().ifPresent(group ->
                sentences.add(mode.nameOf(outcome.shooter()) + " takes " + group.groupName().toLowerCase() + "."));
        if (outcome.foul().isFoul()) {
            sentences.add("Foul: " + outcome.foul().description() + ".");
        }
        if (outcome.eightRespotted()) {
            sentences.add("The 8-ball is re-spotted.");
        }
        return sentences.isEmpty() ? "" : String.join(" ", sentences) + " ";
    }
}
