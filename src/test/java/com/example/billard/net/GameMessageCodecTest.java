package com.example.billard.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.billard.model.GamePhase;
import com.example.billard.model.GameSession;
import com.example.billard.model.Shot;
import com.example.billard.model.Table;
import com.example.billard.model.TableState;
import com.example.billard.model.TipContact;
import com.example.billard.model.Vec2;
import com.example.billard.net.GameMessage.Aim;
import com.example.billard.net.GameMessage.Chat;
import com.example.billard.net.GameMessage.CueBallMoved;
import com.example.billard.net.GameMessage.CueBallPlaced;
import com.example.billard.net.GameMessage.RematchRequest;
import com.example.billard.net.GameMessage.Settled;
import com.example.billard.net.GameMessage.ShotTaken;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameMessageCodecTest {

    private static final List<String> MALFORMED = List.of(
            "",
            "HELLO",
            "SHOT 1 0.5 0 0 extra",
            "SHOT 1 0.5 0",
            "SHOT 1 1.5 0 0",
            "SHOT 1 -0.1 0 0",
            "SHOT NaN 0.5 0 0",
            "SHOT Infinity 0.5 0 0",
            "MOVE 1e400 3",
            "CHAT INSULT",
            "AIM one 0.5 0 0",
            "SETTLED AIMING ONE 1 - -");

    @Test
    void everyKindOfMessageReadsBackExactlyAsSent() {
        List<GameMessage> messages = List.of(
                new Aim(0.1 + 0.2, 0.7, new TipContact(-0.25, 0.5)),
                new CueBallMoved(new Vec2(123.456, 78.9)),
                new CueBallPlaced(new Vec2(Math.PI, Math.E)),
                new ShotTaken(new Shot(-2.5, 1.0, new TipContact(0, -1))),
                new Settled(tableAfterBreak()),
                new Chat(QuickChat.NICE_SHOT),
                new RematchRequest());

        for (GameMessage message : messages) {
            assertEquals(message, GameMessageCodec.decode(GameMessageCodec.encode(message)));
        }
    }

    @Test
    void aTableStateFitsOnOneLine() {
        String line = GameMessageCodec.encode(new Settled(tableAfterBreak()));

        assertTrue(line.indexOf('\n') < 0 && line.length() < LineChannel.MAX_LINE_BYTES);
    }

    @Test
    void refusesAnythingMalformed() {
        for (String line : MALFORMED) {
            assertThrows(ProtocolException.class, () -> GameMessageCodec.decode(line), line);
        }
    }

    @Test
    void refusesATableStateWhileBallsAreMoving() {
        String settled = GameMessageCodec.encode(new Settled(tableAfterBreak()));
        String moving = settled.replaceFirst("^SETTLED \\w+", "SETTLED BALLS_MOVING");

        assertThrows(ProtocolException.class, () -> GameMessageCodec.decode(moving));
    }

    private static TableState tableAfterBreak() {
        GameSession session = new GameSession(Table.standard());
        session.setPower(1.0);
        session.shoot();
        for (int frame = 0; frame < 60 * 90 && session.phase() == GamePhase.BALLS_MOVING; frame++) {
            session.advance(1.0 / 60);
        }
        return session.capture();
    }
}
