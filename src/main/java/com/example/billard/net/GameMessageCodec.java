package com.example.billard.net;

import com.example.billard.model.BallKind;
import com.example.billard.model.Foul;
import com.example.billard.model.GamePhase;
import com.example.billard.model.Player;
import com.example.billard.model.Shot;
import com.example.billard.model.TableState;
import com.example.billard.model.TableState.BallState;
import com.example.billard.model.TipContact;
import com.example.billard.model.TurnOutcome;
import com.example.billard.model.Vec2;
import com.example.billard.net.GameMessage.Aim;
import com.example.billard.net.GameMessage.Chat;
import com.example.billard.net.GameMessage.CueBallMoved;
import com.example.billard.net.GameMessage.CueBallPlaced;
import com.example.billard.net.GameMessage.RematchRequest;
import com.example.billard.net.GameMessage.Settled;
import com.example.billard.net.GameMessage.ShotTaken;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Turns game messages into single lines of text and back. Numbers are written so they read back to exactly the
 * same double, which keeps both copies of a shot identical. Decoding checks every field and refuses anything else.
 *
 * <p>Formats, fields separated by single spaces:
 * <pre>
 * AIM angle power tipRight tipUp
 * MOVE x y
 * PLACE x y
 * SHOT angle power tipRight tipUp
 * SETTLED phase currentPlayer breakShot playerOneGroup outcome (x y pocketed) x 16
 * CHAT phrase
 * REMATCH
 * </pre>
 * The outcome is {@code -} or {@code shooter,nextPlayer,foul,winner,assignedGroup,eightRespotted}.
 */
public final class GameMessageCodec {

    private static final char OUTCOME_SEPARATOR = ',';

    private GameMessageCodec() {
    }

    public static String encode(GameMessage message) {
        return switch (message) {
            case Aim aim -> "AIM " + shotFields(new Shot(aim.angle(), aim.power(), aim.tip()));
            case CueBallMoved moved -> "MOVE " + point(moved.spot());
            case CueBallPlaced placed -> "PLACE " + point(placed.spot());
            case ShotTaken taken -> "SHOT " + shotFields(taken.shot());
            case Settled settled -> "SETTLED " + tableState(settled.state());
            case Chat chat -> "CHAT " + chat.phrase().name();
            case RematchRequest request -> "REMATCH";
        };
    }

    /** @throws ProtocolException if {@code line} is not exactly one well-formed message */
    public static GameMessage decode(String line) {
        Fields fields = Fields.of(line);
        String type = fields.nextText();
        GameMessage message = switch (type) {
            case "AIM" -> aimOf(readShot(fields));
            case "MOVE" -> new CueBallMoved(readPoint(fields));
            case "PLACE" -> new CueBallPlaced(readPoint(fields));
            case "SHOT" -> new ShotTaken(readShot(fields));
            case "SETTLED" -> new Settled(readTableState(fields));
            case "CHAT" -> new Chat(fields.nextEnum(QuickChat.class));
            case "REMATCH" -> new RematchRequest();
            default -> throw new ProtocolException("Unknown game message type: " + Fields.abbreviate(type));
        };
        fields.requireEnd();
        return message;
    }

    private static String shotFields(Shot shot) {
        return shot.angle() + " " + shot.power() + " " + shot.tip().right() + " " + shot.tip().up();
    }

    /** A shot as sent, with its power checked to lie between 0 and 1 so no shot can be harder than allowed. */
    private static Shot readShot(Fields fields) {
        double angle = fields.nextDouble();
        double power = fields.nextDouble();
        if (power < 0 || power > 1) {
            throw new ProtocolException("Shot power must lie between 0 and 1 but was " + power);
        }
        return new Shot(angle, power, readTip(fields));
    }

    private static Aim aimOf(Shot shot) {
        return new Aim(shot.angle(), shot.power(), shot.tip());
    }

    private static String point(Vec2 point) {
        return point.x() + " " + point.y();
    }

    private static TipContact readTip(Fields fields) {
        return new TipContact(fields.nextDouble(), fields.nextDouble());
    }

    private static Vec2 readPoint(Fields fields) {
        return new Vec2(fields.nextDouble(), fields.nextDouble());
    }

    private static String tableState(TableState state) {
        StringBuilder text = new StringBuilder()
                .append(state.phase().name()).append(' ')
                .append(state.currentPlayer().name()).append(' ')
                .append(Fields.flag(state.breakShot())).append(' ')
                .append(Fields.optional(state.playerOneGroup())).append(' ')
                .append(state.lastOutcome().map(GameMessageCodec::outcome).orElse(Fields.NONE));
        for (BallState ball : state.balls()) {
            text.append(' ').append(point(ball.position())).append(' ').append(Fields.flag(ball.pocketed()));
        }
        return text.toString();
    }

    private static TableState readTableState(Fields fields) {
        GamePhase phase = fields.nextEnum(GamePhase.class);
        Player current = fields.nextEnum(Player.class);
        boolean breakShot = fields.nextFlag();
        Optional<BallKind> playerOneGroup = fields.nextOptionalEnum(BallKind.class);
        Optional<TurnOutcome> outcome = readOutcome(fields.nextText());
        List<BallState> balls = new ArrayList<>();
        for (int number = 0; number < TableState.BALL_COUNT; number++) {
            balls.add(new BallState(number, readPoint(fields), fields.nextFlag()));
        }
        try {
            return new TableState(phase, current, breakShot, playerOneGroup, outcome, balls);
        } catch (IllegalArgumentException e) {
            throw new ProtocolException("Impossible table state: " + e.getMessage(), e);
        }
    }

    private static String outcome(TurnOutcome outcome) {
        return String.join(String.valueOf(OUTCOME_SEPARATOR),
                outcome.shooter().name(),
                outcome.nextPlayer().name(),
                outcome.foul().name(),
                Fields.optional(outcome.winner()),
                Fields.optional(outcome.assignedGroup()),
                Fields.flag(outcome.eightRespotted()));
    }

    private static Optional<TurnOutcome> readOutcome(String text) {
        if (Fields.NONE.equals(text)) {
            return Optional.empty();
        }
        Fields fields = Fields.of(text, OUTCOME_SEPARATOR);
        TurnOutcome outcome = new TurnOutcome(
                fields.nextEnum(Player.class),
                fields.nextEnum(Player.class),
                fields.nextEnum(Foul.class),
                fields.nextOptionalEnum(Player.class),
                fields.nextOptionalEnum(BallKind.class),
                fields.nextFlag());
        fields.requireEnd();
        return Optional.of(outcome);
    }
}
