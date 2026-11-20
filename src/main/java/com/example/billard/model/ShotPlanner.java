package com.example.billard.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Chooses the bot's shots by trying them out. Every candidate is played forward on a copy of the table with the
 * game's own physics and judged by the game's own rules, so the bot sees scratches, fouls and early eights exactly
 * as the match would score them. Among the shots that work it prefers those that still work when struck a little
 * off, and, depending on its {@link BotLevel}, those that leave an easy next shot for itself or no easy shot for
 * the opponent. Its level then blurs the chosen shot, so it can miss; at {@link BotLevel#BOSS} it plays the
 * simulated shot exactly.
 *
 * <p>Candidates, in the amounts the level's {@link CandidateSearch} sets: pots of every legal ball into every pocket
 * at several powers and spins, a sweep of plain shots all round the cue ball that finds banks, kicks and safeties,
 * and, with ball in hand, straight-in pots from spots behind each legal ball.
 */
public final class ShotPlanner {

    private static final double MAX_CUT_ANGLE = Math.toRadians(78);
    private static final double EASY_CUT_ANGLE = Math.toRadians(45);
    private static final int SHORTLIST_SIZE = 12;
    private static final double MAX_SIMULATED_SECONDS = 30;
    private static final int MAX_STEPS = (int) (MAX_SIMULATED_SECONDS / PhysicsEngine.FIXED_STEP);

    private static final double WIN_SCORE = 1000;
    private static final double LOSS_SCORE = -1000;
    private static final double FOUL_SCORE = -100;
    private static final double KEEP_TURN_SCORE = 100;
    private static final double POTTED_BALL_SCORE = 10;
    private static final int MAX_EASY_SHOTS_COUNTED = 6;

    private record Scored(BotMove move, double score) {
    }

    private final BotLevel level;
    private final CandidateSearch search;
    private final RandomGenerator random;

    public ShotPlanner(BotLevel level, RandomGenerator random) {
        this.level = level;
        this.search = level.search();
        this.random = random;
    }

    /**
     * The move the bot plays from {@code snapshot}, already blurred by its level. Candidates are simulated in
     * parallel, each on its own copy of the table; the search takes a fraction of a second, or about a second for the
     * Boss.
     */
    public BotMove plan(TableSnapshot snapshot) {
        BotMove best = candidates(snapshot).parallelStream()
                .map(move -> new Scored(move, score(snapshot, move)))
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .limit(SHORTLIST_SIZE)
                .map(scored -> new Scored(scored.move(), robustScore(snapshot, scored)))
                .max(Comparator.comparingDouble(Scored::score))
                .orElseThrow()
                .move();
        return best.withShot(level.blur(best.shot(), random));
    }

    private List<BotMove> candidates(TableSnapshot snapshot) {
        List<BotMove> moves = new ArrayList<>();
        addPots(snapshot, moves);
        addSweep(moves);
        if (snapshot.ballInHand()) {
            addPlacements(snapshot, moves);
        }
        return moves;
    }

    private void addPots(TableSnapshot snapshot, List<BotMove> moves) {
        Table table = snapshot.table();
        Vec2 cue = snapshot.cueBall().position();
        for (Ball target : legalBallsOnTable(snapshot.balls(), snapshot.match(), snapshot.shooter())) {
            for (Table.Pocket pocket : table.pockets()) {
                potAngle(table, cue, target.position(), pocket).ifPresent(angle -> addPotShots(moves, angle));
            }
        }
    }

    private void addPotShots(List<BotMove> moves, double angle) {
        for (double power : search.potPowers()) {
            search.potTips().forEach(tip -> moves.add(BotMove.from(new Shot(angle, power, tip))));
        }
    }

    private void addSweep(List<BotMove> moves) {
        for (int step = 0; step < search.sweepDirections(); step++) {
            double angle = 2 * Math.PI * step / search.sweepDirections();
            for (double power : search.sweepPowers()) {
                moves.add(BotMove.from(new Shot(angle, power, TipContact.CENTER)));
            }
        }
    }

    /** Spots straight behind each legal ball as seen from a pocket, where the pot is a plain straight shot. */
    private void addPlacements(TableSnapshot snapshot, List<BotMove> moves) {
        Table table = snapshot.table();
        for (Ball target : legalBallsOnTable(snapshot.balls(), snapshot.match(), snapshot.shooter())) {
            for (Table.Pocket pocket : table.pockets()) {
                Vec2 towardPocket = pocket.center().minus(target.position()).normalized();
                Vec2 ghost = ghostBall(table, target.position(), towardPocket);
                for (double distance : search.placementDistances()) {
                    Vec2 spot = ghost.minus(towardPocket.times(distance));
                    if (BallPlacement.isFree(table, snapshot.balls(), snapshot.cueBall(), spot)) {
                        addPlacedShots(moves, spot, towardPocket.angle());
                    }
                }
            }
        }
    }

    private void addPlacedShots(List<BotMove> moves, Vec2 spot, double angle) {
        for (double power : search.placementPowers()) {
            moves.add(BotMove.placedAt(spot, new Shot(angle, power, TipContact.CENTER)));
        }
    }

    /**
     * The direction to send the cue ball so it meets the target ball square for {@code pocket}, unless the cut is
     * too thin to make.
     */
    private static Optional<Double> potAngle(Table table, Vec2 cue, Vec2 target, Table.Pocket pocket) {
        Vec2 towardPocket = pocket.center().minus(target).normalized();
        Vec2 path = ghostBall(table, target, towardPocket).minus(cue);
        if (path.length() < table.ballRadius() || cutAngle(path, towardPocket) > MAX_CUT_ANGLE) {
            return Optional.empty();
        }
        return Optional.of(path.angle());
    }

    private static double cutAngle(Vec2 cuePath, Vec2 towardPocket) {
        return Math.acos(Math.max(-1, Math.min(1, cuePath.normalized().dot(towardPocket))));
    }

    /** Where the cue ball's centre must be at contact to send the target along {@code direction}. */
    private static Vec2 ghostBall(Table table, Vec2 target, Vec2 direction) {
        return target.minus(direction.times(2 * table.ballRadius()));
    }

    private static List<Ball> legalBallsOnTable(List<Ball> balls, EightBallMatch match, Player player) {
        Set<BallKind> legal = match.legalTargets(player);
        return balls.stream().filter(ball -> !ball.isPocketed() && legal.contains(ball.kind())).toList();
    }

    /**
     * The average score of the shot as aimed and when struck a typical error to either side. A bot that never
     * misses plays the shot as aimed, so its score stands.
     */
    private double robustScore(TableSnapshot snapshot, Scored scored) {
        if (level.aimError() == 0) {
            return scored.score();
        }
        Shot shot = scored.move().shot();
        double left = score(snapshot, scored.move().withShot(turned(shot, -level.aimError())));
        double right = score(snapshot, scored.move().withShot(turned(shot, level.aimError())));
        return (scored.score() + left + right) / 3;
    }

    private static Shot turned(Shot shot, double radians) {
        return new Shot(shot.angle() + radians, shot.power(), shot.tip());
    }

    /** Plays {@code move} on a copy of the table and judges where it leaves the match. */
    private double score(TableSnapshot snapshot, BotMove move) {
        List<Ball> balls = snapshot.copyOfBalls();
        Ball cueBall = TableSnapshot.cueBallOf(balls);
        move.cueSpot().ifPresent(cueBall::putOnTable);
        ShotRecorder recorder = new ShotRecorder();
        PhysicsEngine engine = new PhysicsEngine(snapshot.table(), recorder);
        move.shot().strike(cueBall);
        for (int step = 0; step < MAX_STEPS && balls.stream().anyMatch(Ball::isMoving); step++) {
            engine.step(balls);
        }
        EightBallMatch match = snapshot.match().copy();
        TurnOutcome outcome = match.resolveShot(recorder.report());
        int easyShotsLeft = easyShots(snapshot.table(), balls, match, outcome.nextPlayer());
        return judge(outcome, recorder.report().pocketed().size(), easyShotsLeft);
    }

    /**
     * Winning beats everything and losing is worst; a foul hands over ball in hand. Otherwise keeping the table is
     * good, and better with more balls down and an easy next shot; giving it up is better the fewer easy shots
     * the opponent is left.
     */
    private double judge(TurnOutcome outcome, int ballsPotted, int easyShotsLeft) {
        if (outcome.winner().isPresent()) {
            return outcome.winner().get() == outcome.shooter() ? WIN_SCORE : LOSS_SCORE;
        }
        if (outcome.foul().isFoul()) {
            return FOUL_SCORE;
        }
        double leave = level.leaveWeight() * easyShotsLeft;
        if (outcome.nextPlayer() != outcome.shooter()) {
            return -leave;
        }
        return KEEP_TURN_SCORE + POTTED_BALL_SCORE * ballsPotted + leave;
    }

    /** How many gentle cuts {@code player} has from where the cue ball stopped, with nothing in the way. */
    private static int easyShots(Table table, List<Ball> balls, EightBallMatch match, Player player) {
        Ball cueBall = TableSnapshot.cueBallOf(balls);
        if (cueBall.isPocketed()) {
            return 0;
        }
        int count = 0;
        for (Ball target : legalBallsOnTable(balls, match, player)) {
            for (Table.Pocket pocket : table.pockets()) {
                if (isEasyPot(table, balls, cueBall, target, pocket)) {
                    count++;
                }
            }
        }
        return Math.min(count, MAX_EASY_SHOTS_COUNTED);
    }

    private static boolean isEasyPot(Table table, List<Ball> balls, Ball cueBall, Ball target, Table.Pocket pocket) {
        Vec2 towardPocket = pocket.center().minus(target.position()).normalized();
        Vec2 path = ghostBall(table, target.position(), towardPocket).minus(cueBall.position());
        if (path.length() < table.ballRadius() || cutAngle(path, towardPocket) > EASY_CUT_ANGLE) {
            return false;
        }
        return AimPredictor.predict(table, balls, cueBall, path.angle()).target().filter(target::equals).isPresent();
    }
}
