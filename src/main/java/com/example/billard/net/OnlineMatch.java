package com.example.billard.net;

import com.example.billard.model.GamePhase;
import com.example.billard.model.GameSession;
import com.example.billard.model.Opponent;
import com.example.billard.model.Player;
import com.example.billard.model.Shot;
import com.example.billard.model.TurnListener;
import com.example.billard.model.Vec2;
import com.example.billard.net.GameMessage.Aim;
import com.example.billard.net.GameMessage.Chat;
import com.example.billard.net.GameMessage.CueBallMoved;
import com.example.billard.net.GameMessage.CueBallPlaced;
import com.example.billard.net.GameMessage.RematchRequest;
import com.example.billard.net.GameMessage.Settled;
import com.example.billard.net.GameMessage.ShotTaken;
import com.example.billard.net.OnlineLink.Role;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * One match of an online session as played on this computer. Passes on what the local player does, and replays
 * what the remote player does on the local table: their live aim, where they put the cue ball, and their shots.
 *
 * <p>The shooter's computer decides how its own shot ends. Both computers roll the same shot, so they nearly always
 * agree, but floating-point maths may differ slightly between machines; once the shooter's balls stop, its table is
 * sent over and replaces the replayed one. Until then the shot counts as still in play here, so the local player
 * cannot act on a result that might change. A remote table is only accepted right after a remote shot.
 *
 * <p>All methods must be called on the thread that runs the game, normally the JavaFX application thread.
 */
public final class OnlineMatch implements Opponent, TurnListener {

    /** Hears what the other player says, and when both want to play again. */
    public interface Listener {

        void onChat(QuickChat phrase);

        /** The other player asked for a rematch; this side has not asked yet. */
        void onRematchRequested();

        /** Both players asked for a rematch: this match is over and the next one should start. */
        void onRematchAgreed();
    }

    /** Live aim goes out about 30 times a second at most, and only when it changed. */
    static final double LIVE_INTERVAL_SECONDS = 1.0 / 30;
    /** How long the replay of a remote shot may run on after the shooter's result arrived before it is cut short. */
    static final double SETTLE_GRACE_SECONDS = 2;

    private static final Logger LOG = Logger.getLogger(OnlineMatch.class.getName());

    private final GameSession session;
    private final Player localSeat;
    private final MatchChannel channel;
    private final Listener listener;
    private boolean replaying;
    private boolean localShotRolling;
    private boolean awaitingRemoteResult;
    private double resultWaitSeconds;
    private double liveSeconds;
    private Optional<GameMessage> lastLiveSent = Optional.empty();
    private boolean rematchAskedHere;
    private boolean rematchAskedThere;
    private boolean over;

    /** Plays {@code session} with the local player in {@code localSeat}, and starts listening to the session. */
    public OnlineMatch(GameSession session, Player localSeat, MatchChannel channel, Listener listener) {
        this.session = session;
        this.localSeat = localSeat;
        this.channel = channel;
        this.listener = listener;
        session.addTurnListener(this);
    }

    /** The host breaks in the first match; after that the break alternates with every rematch. */
    public static Player seatFor(Role role, int matchNumber) {
        boolean hostBreaks = matchNumber % 2 == 0;
        return (role == Role.HOST) == hostBreaks ? Player.ONE : Player.TWO;
    }

    public Player localSeat() {
        return localSeat;
    }

    @Override
    public void update(double elapsedSeconds) {
        if (over) {
            return;
        }
        reportFinishedLocalShot();
        handleIncoming(elapsedSeconds);
        streamLiveAim(elapsedSeconds);
    }

    @Override
    public boolean hasTheTable() {
        if (awaitingRemoteResult) {
            return true;
        }
        return session.phase() != GamePhase.GAME_OVER && session.currentPlayer() != localSeat;
    }

    @Override
    public void stop() {
        over = true;
    }

    public void sendChat(QuickChat phrase) {
        channel.send(new Chat(phrase));
    }

    /** Asks for another match once this one is decided. When the other player already asked, it starts. */
    public void requestRematch() {
        if (over || rematchAskedHere || session.phase() != GamePhase.GAME_OVER || hasTheTable()) {
            return;
        }
        rematchAskedHere = true;
        channel.send(new RematchRequest());
        if (rematchAskedThere) {
            agreeRematch();
        }
    }

    public boolean isRematchRequestedHere() {
        return rematchAskedHere;
    }

    @Override
    public void onShot(Shot shot) {
        if (!replaying) {
            localShotRolling = true;
            channel.send(new ShotTaken(shot));
        }
    }

    @Override
    public void onCueBallPlaced(Vec2 spot) {
        if (!replaying) {
            channel.send(new CueBallPlaced(spot));
        }
    }

    private void reportFinishedLocalShot() {
        if (localShotRolling && session.phase() != GamePhase.BALLS_MOVING) {
            localShotRolling = false;
            channel.send(new Settled(session.capture()));
        }
    }

    /** Handles received messages in order, stopping at one that has to wait or once the match is over. */
    private void handleIncoming(double elapsedSeconds) {
        Optional<GameMessage> next = channel.nextIncoming();
        while (next.isPresent() && !over && handle(next.get(), elapsedSeconds)) {
            channel.consumeIncoming();
            next = channel.nextIncoming();
        }
    }

    /** @return false if {@code message} has to wait for a later frame */
    private boolean handle(GameMessage message, double elapsedSeconds) {
        switch (message) {
            case Aim aim -> replayAim(aim);
            case CueBallMoved moved -> replayCueBallMove(moved.spot());
            case CueBallPlaced placed -> replayPlacement(placed.spot());
            case ShotTaken taken -> replayShot(taken.shot());
            case Settled settled -> {
                return applyRemoteResult(settled, elapsedSeconds);
            }
            case Chat chat -> listener.onChat(chat.phrase());
            case RematchRequest request -> receiveRematchRequest();
        }
        return true;
    }

    private void replayAim(Aim aim) {
        if (isRemoteAt(GamePhase.AIMING)) {
            replay(() -> {
                session.setAimAngle(aim.angle());
                session.setPower(aim.power());
                session.setTipContact(aim.tip());
            });
        }
    }

    private void replayCueBallMove(Vec2 spot) {
        if (isRemoteAt(GamePhase.PLACING_CUE)) {
            session.previewCueBall(spot);
        }
    }

    private void replayPlacement(Vec2 spot) {
        if (!isRemoteAt(GamePhase.PLACING_CUE)) {
            LOG.warning("Ignored a cue ball placement that arrived out of turn");
            return;
        }
        replay(() -> {
            session.previewCueBall(spot);
            session.confirmPlacement();
        });
        if (session.phase() == GamePhase.PLACING_CUE) {
            LOG.log(Level.WARNING, "The other player''s cue ball spot {0} is not free here", spot);
        }
    }

    private void replayShot(Shot shot) {
        if (!isRemoteAt(GamePhase.AIMING)) {
            LOG.warning("Ignored a shot that arrived out of turn");
            return;
        }
        replay(() -> {
            session.setAimAngle(shot.angle());
            session.setPower(shot.power());
            session.setTipContact(shot.tip());
            session.shoot();
        });
        awaitingRemoteResult = true;
        resultWaitSeconds = 0;
    }

    /**
     * Takes over the shooter's table once the replay here has finished, or cuts the replay short when it runs on
     * for too long. A table sent at any other time is refused, so the other side cannot rearrange the balls.
     */
    private boolean applyRemoteResult(Settled settled, double elapsedSeconds) {
        if (!awaitingRemoteResult) {
            LOG.warning("Ignored a table state that did not follow a shot by the other player");
            return true;
        }
        if (session.phase() == GamePhase.BALLS_MOVING && resultWaitSeconds < SETTLE_GRACE_SECONDS) {
            resultWaitSeconds += elapsedSeconds;
            return false;
        }
        try {
            session.restore(settled.state());
        } catch (IllegalArgumentException invalid) {
            LOG.log(Level.WARNING, "Ignored an impossible table state: {0}", invalid.getMessage());
        }
        awaitingRemoteResult = false;
        return true;
    }

    private void receiveRematchRequest() {
        rematchAskedThere = true;
        if (rematchAskedHere) {
            agreeRematch();
        } else {
            listener.onRematchRequested();
        }
    }

    private void agreeRematch() {
        over = true;
        listener.onRematchAgreed();
    }

    /** Sends where the local player aims, or holds the cue ball, while it is their turn and it has changed. */
    private void streamLiveAim(double elapsedSeconds) {
        liveSeconds += elapsedSeconds;
        if (liveSeconds < LIVE_INTERVAL_SECONDS) {
            return;
        }
        liveSeconds = 0;
        Optional<GameMessage> current = currentLiveMessage();
        if (!current.equals(lastLiveSent)) {
            current.ifPresent(channel::send);
            lastLiveSent = current;
        }
    }

    private Optional<GameMessage> currentLiveMessage() {
        if (session.currentPlayer() != localSeat || awaitingRemoteResult) {
            return Optional.empty();
        }
        return switch (session.phase()) {
            case AIMING -> Optional.of(new Aim(session.aimAngle(), session.power(), session.tipContact()));
            case PLACING_CUE -> Optional.of(new CueBallMoved(session.cueBall().position()));
            default -> Optional.empty();
        };
    }

    private boolean isRemoteAt(GamePhase phase) {
        return session.currentPlayer() != localSeat && session.phase() == phase && !awaitingRemoteResult;
    }

    /** Runs {@code action} on the session without passing on what it triggers, since it came from the other side. */
    private void replay(Runnable action) {
        replaying = true;
        try {
            action.run();
        } finally {
            replaying = false;
        }
    }
}
