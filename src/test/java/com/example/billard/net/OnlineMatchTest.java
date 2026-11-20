package com.example.billard.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.billard.model.GamePhase;
import com.example.billard.model.GameSession;
import com.example.billard.model.Player;
import com.example.billard.model.Table;
import com.example.billard.model.TableState;
import com.example.billard.model.TableState.BallState;
import com.example.billard.model.Vec2;
import com.example.billard.net.GameMessage.Settled;
import com.example.billard.net.OnlineLink.Role;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OnlineMatchTest {

    private static final double FRAME = 1.0 / 60;
    private static final int MAX_FRAMES = 60 * 120;

    private final InMemoryChannel[] ends = InMemoryChannel.pair();
    private final GameSession hostTable = new GameSession(Table.standard());
    private final GameSession guestTable = new GameSession(Table.standard());
    private final RecordingListener hostHears = new RecordingListener();
    private final RecordingListener guestHears = new RecordingListener();
    private final OnlineMatch hostMatch = new OnlineMatch(hostTable, Player.ONE, ends[0], hostHears);
    private final OnlineMatch guestMatch = new OnlineMatch(guestTable, Player.TWO, ends[1], guestHears);

    @Test
    void theHostBreaksFirstAndTheBreakAlternatesWithEveryRematch() {
        assertEquals(Player.ONE, OnlineMatch.seatFor(Role.HOST, 0));
        assertEquals(Player.TWO, OnlineMatch.seatFor(Role.GUEST, 0));
        assertEquals(Player.TWO, OnlineMatch.seatFor(Role.HOST, 1));
        assertEquals(Player.ONE, OnlineMatch.seatFor(Role.GUEST, 1));
    }

    @Test
    void onlyThePlayerWhoseTurnItIsHasTheTable() {
        assertFalse(hostMatch.hasTheTable());
        assertTrue(guestMatch.hasTheTable());
    }

    @Test
    void theOtherPlayerSeesTheShooterLineUpLive() {
        hostTable.setAimAngle(1.25);
        hostTable.setPower(0.8);

        playFrames(2);

        assertEquals(1.25, guestTable.aimAngle());
        assertEquals(0.8, guestTable.power());
    }

    @Test
    void aShotEndsTheSameOnBothTables() {
        hostTable.setAimAngle(0.02);
        hostTable.setPower(1.0);
        hostTable.shoot();

        playUntilQuiet();

        assertEquals(hostTable.capture(), guestTable.capture());
    }

    @Test
    void theShootersTableWinsWhenTheReplayEndsDifferently() {
        nudgeBallOnGuestTable(15);
        hostTable.setPower(1.0);
        hostTable.shoot();

        playUntilQuiet();

        assertEquals(hostTable.capture(), guestTable.capture());
    }

    @Test
    void theOtherPlayerWaitsUntilTheShootersResultArrives() {
        hostTable.setPower(1.0);
        hostTable.shoot();
        playFrames(1);

        assertTrue(guestMatch.hasTheTable());
        assertFalse(hostMatch.hasTheTable());
    }

    @Test
    void aTableStateThatFollowsNoShotIsIgnored() {
        TableState before = guestTable.capture();
        TableState forged = withBallMoved(before, 15, guestTable.table().headSpot().plus(new Vec2(0, 60)));

        ends[1].receive(new Settled(forged));
        playFrames(1);

        assertEquals(before, guestTable.capture());
    }

    @Test
    void aCueBallPlacedAfterAFoulAppearsOnTheOtherTable() {
        TableState guestHasBallInHand = inPhase(hostTable.capture(), GamePhase.PLACING_CUE, Player.TWO);
        hostTable.restore(guestHasBallInHand);
        guestTable.restore(guestHasBallInHand);
        Vec2 spot = guestTable.table().headSpot().plus(new Vec2(0, 40));

        guestTable.previewCueBall(spot);
        guestTable.confirmPlacement();
        playFrames(1);

        assertEquals(GamePhase.AIMING, hostTable.phase());
        assertEquals(guestTable.cueBall().position(), hostTable.cueBall().position());
    }

    @Test
    void aRematchStartsOnlyOnceBothPlayersAskForIt() {
        TableState decided = inPhase(hostTable.capture(), GamePhase.GAME_OVER, Player.ONE);
        hostTable.restore(decided);
        guestTable.restore(decided);

        hostMatch.requestRematch();
        playFrames(1);
        assertEquals(1, guestHears.rematchRequests);
        assertEquals(0, hostHears.rematchesAgreed + guestHears.rematchesAgreed);

        guestMatch.requestRematch();
        playFrames(1);
        assertEquals(1, hostHears.rematchesAgreed);
        assertEquals(1, guestHears.rematchesAgreed);
    }

    @Test
    void aRematchCannotBeRequestedWhileTheMatchIsOn() {
        hostMatch.requestRematch();

        assertFalse(hostMatch.isRematchRequestedHere());
    }

    @Test
    void quickChatReachesTheOtherPlayer() {
        guestMatch.sendChat(QuickChat.GOOD_LUCK);
        playFrames(1);

        assertEquals(List.of(QuickChat.GOOD_LUCK), hostHears.chat);
    }

    private void playFrames(int frames) {
        for (int frame = 0; frame < frames; frame++) {
            playFrame();
        }
    }

    private void playUntilQuiet() {
        for (int frame = 0; frame < MAX_FRAMES && !isQuiet(); frame++) {
            playFrame();
        }
        assertTrue(isQuiet(), "the shot never settled on both tables");
    }

    private boolean isQuiet() {
        return hostTable.phase() != GamePhase.BALLS_MOVING && guestTable.phase() != GamePhase.BALLS_MOVING
                && !ends[0].hasWaiting() && !ends[1].hasWaiting();
    }

    private void playFrame() {
        hostTable.advance(FRAME);
        hostMatch.update(FRAME);
        guestTable.advance(FRAME);
        guestMatch.update(FRAME);
    }

    private void nudgeBallOnGuestTable(int number) {
        TableState state = guestTable.capture();
        Vec2 position = state.balls().get(number).position();
        guestTable.restore(withBallMoved(state, number, position.plus(new Vec2(0, 0.5))));
        assertNotEquals(hostTable.capture(), guestTable.capture());
    }

    private static TableState withBallMoved(TableState state, int number, Vec2 position) {
        List<BallState> balls = new ArrayList<>(state.balls());
        balls.set(number, new BallState(number, position, false));
        return new TableState(state.phase(), state.currentPlayer(), state.breakShot(), state.playerOneGroup(),
                state.lastOutcome(), balls);
    }

    private static TableState inPhase(TableState state, GamePhase phase, Player current) {
        return new TableState(phase, current, false, state.playerOneGroup(), state.lastOutcome(), state.balls());
    }

    private static final class RecordingListener implements OnlineMatch.Listener {
        final List<QuickChat> chat = new ArrayList<>();
        int rematchRequests;
        int rematchesAgreed;

        @Override
        public void onChat(QuickChat phrase) {
            chat.add(phrase);
        }

        @Override
        public void onRematchRequested() {
            rematchRequests++;
        }

        @Override
        public void onRematchAgreed() {
            rematchesAgreed++;
        }
    }
}
