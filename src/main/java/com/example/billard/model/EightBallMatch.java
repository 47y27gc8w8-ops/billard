package com.example.billard.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Rules of the match: whose turn it is, who owns which group, fouls and the win condition.
 *
 * <p>Simplified standard eight-ball. The break is an ordinary shot, except that the eight-ball is re-spotted
 * if it drops. The first legal pot on the open table decides the groups. A foul gives the opponent
 * ball in hand. The eight-ball wins the match when potted legally after the shooter's group is cleared, and
 * loses it when potted any other way.
 */
public final class EightBallMatch {

    private static final int GROUP_SIZE = 7;

    private final Map<Player, BallKind> groups = new EnumMap<>(Player.class);
    private final Map<BallKind, Integer> pottedPerGroup = new EnumMap<>(BallKind.class);
    private Player currentPlayer = Player.ONE;
    private boolean breakShot = true;

    public Player currentPlayer() {
        return currentPlayer;
    }

    public Optional<BallKind> groupOf(Player player) {
        return Optional.ofNullable(groups.get(player));
    }

    public boolean isTableOpen() {
        return groups.isEmpty();
    }

    public boolean isBreakShot() {
        return breakShot;
    }

    public TurnOutcome resolveShot(ShotReport report) {
        Player shooter = currentPlayer;
        boolean shootingAtEight = isShootingAtEight(shooter);
        Foul foul = detectFoul(report, shooter, shootingAtEight);
        boolean eightOnBreak = breakShot && report.pocketedEight();
        breakShot = false;

        Optional<Player> winner = eightOnBreak ? Optional.empty() : decideWinner(report, shooter, shootingAtEight, foul);
        Optional<BallKind> assignedGroup = winner.isPresent() ? Optional.empty() : assignGroupIfOpen(report, shooter, foul);
        countPottedBalls(report);

        currentPlayer = keepsTurn(report, shooter, foul, winner) ? shooter : shooter.opponent();
        return new TurnOutcome(shooter, currentPlayer, foul, winner, assignedGroup, eightOnBreak);
    }

    private boolean isShootingAtEight(Player player) {
        BallKind group = groups.get(player);
        return group != null && pottedPerGroup.getOrDefault(group, 0) >= GROUP_SIZE;
    }

    private Foul detectFoul(ShotReport report, Player shooter, boolean shootingAtEight) {
        if (report.pocketedCue()) {
            return Foul.CUE_POCKETED;
        }
        if (report.firstContact().isEmpty()) {
            return Foul.NO_CONTACT;
        }
        if (!isLegalTarget(report.firstContact().get(), shooter, shootingAtEight)) {
            return Foul.WRONG_BALL_FIRST;
        }
        if (report.pocketed().isEmpty() && !report.railAfterContact()) {
            return Foul.NO_RAIL;
        }
        return Foul.NONE;
    }

    private boolean isLegalTarget(BallKind target, Player shooter, boolean shootingAtEight) {
        if (shootingAtEight) {
            return target == BallKind.EIGHT;
        }
        BallKind group = groups.get(shooter);
        return group == null ? target.isGroup() : target == group;
    }

    private Optional<Player> decideWinner(ShotReport report, Player shooter, boolean shootingAtEight, Foul foul) {
        if (!report.pocketedEight()) {
            return Optional.empty();
        }
        boolean legalFinish = shootingAtEight && !foul.isFoul();
        return Optional.of(legalFinish ? shooter : shooter.opponent());
    }

    private Optional<BallKind> assignGroupIfOpen(ShotReport report, Player shooter, Foul foul) {
        if (!isTableOpen() || foul.isFoul()) {
            return Optional.empty();
        }
        Optional<BallKind> firstGroupPot = report.pocketed().stream().filter(BallKind::isGroup).findFirst();
        firstGroupPot.ifPresent(group -> {
            groups.put(shooter, group);
            groups.put(shooter.opponent(), group.opposite());
        });
        return firstGroupPot;
    }

    private void countPottedBalls(ShotReport report) {
        report.pocketed().stream()
                .filter(BallKind::isGroup)
                .forEach(group -> pottedPerGroup.merge(group, 1, Integer::sum));
    }

    private boolean keepsTurn(ShotReport report, Player shooter, Foul foul, Optional<Player> winner) {
        if (winner.isPresent()) {
            return true;
        }
        if (foul.isFoul()) {
            return false;
        }
        BallKind ownGroup = groups.get(shooter);
        return ownGroup != null && report.pocketed().contains(ownGroup);
    }
}
