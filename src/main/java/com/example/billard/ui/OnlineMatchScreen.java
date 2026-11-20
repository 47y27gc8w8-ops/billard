package com.example.billard.ui;

import com.example.billard.model.GameSession;
import com.example.billard.model.Player;
import com.example.billard.model.Table;
import com.example.billard.net.OnlineLink;
import com.example.billard.net.OnlineMatch;
import com.example.billard.net.QuickChat;
import java.util.Optional;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * One match of an online session on screen. Builds the usual match screen with the remote player as the opponent
 * and adds what playing over the network needs: quick chat, a notice while the connection is being restored, the
 * end of the match when the other player leaves, and the rematch handshake.
 */
public final class OnlineMatchScreen implements OnlineMatch.Listener {

    private static final Duration CHAT_SHOWN_FOR = Duration.seconds(4);
    private static final Duration COUNTDOWN_STEP = Duration.millis(500);

    private final OnlineLink link;
    private final OnlineMatch match;
    private final GameScreen screen;
    private final Runnable onNextMatch;
    private final Label chatNotice = new Label();
    private final Label connectionNotice = new Label();
    private final PauseTransition chatTimer = new PauseTransition(CHAT_SHOWN_FOR);
    private final Timeline countdown = new Timeline(new KeyFrame(COUNTDOWN_STEP, event -> showReconnecting()));

    /** The match is wired to {@code this} as its listener; it only calls back from its frame updates, later on. */
    private OnlineMatchScreen(OnlineLink link, int matchNumber, GameAudio audio, Runnable onNextMatch,
                              Runnable onLeave) {
        this.link = link;
        this.onNextMatch = onNextMatch;
        Player localSeat = OnlineMatch.seatFor(link.role(), matchNumber);
        GameSession session = new GameSession(Table.standard());
        this.match = new OnlineMatch(session, localSeat, link, this);
        MatchMode mode = MatchMode.online(localSeat, link.localName(), link.remoteName());
        this.screen = new GameScreen(session, mode, Optional.of(match), audio,
                new GameScreen.Navigation(this::askForRematch, onLeave));
        screen.hideNewMatchButton();
        screen.addShotControl(chatMenu());
        screen.addTableLayer(notices());
        countdown.setCycleCount(Animation.INDEFINITE);
        chatTimer.setOnFinished(event -> chatNotice.setVisible(false));
        link.setStatusListener(this::showConnection);
    }

    /**
     * The screen for match {@code matchNumber} of the session on {@code link}, counting from 0.
     *
     * @param onNextMatch runs once both players agreed to a rematch
     * @param onLeave     runs when the player goes back to the menu; it should close the link
     */
    public static GameScreen open(OnlineLink link, int matchNumber, GameAudio audio, Runnable onNextMatch,
                                  Runnable onLeave) {
        return new OnlineMatchScreen(link, matchNumber, audio, onNextMatch, onLeave).screen;
    }

    @Override
    public void onChat(QuickChat phrase) {
        showChat(link.remoteName() + ": " + phrase.text());
    }

    @Override
    public void onRematchRequested() {
        GameOverOverlay overlay = screen.gameOverOverlay();
        overlay.showNote(link.remoteName() + " wants a rematch.");
        overlay.showPlayAgainState("Accept rematch", true);
    }

    /** Starts the next match after the current frame, since this is called from inside it. */
    @Override
    public void onRematchAgreed() {
        Platform.runLater(onNextMatch);
    }

    private void askForRematch() {
        match.requestRematch();
        if (match.isRematchRequestedHere()) {
            screen.gameOverOverlay().showPlayAgainState("Waiting for " + link.remoteName() + "\u2026", false);
        }
    }

    private MenuButton chatMenu() {
        MenuButton menu = new MenuButton("Chat");
        menu.setAccessibleHelp("Send a quick message to the other player");
        for (QuickChat phrase : QuickChat.values()) {
            MenuItem item = new MenuItem(phrase.text());
            item.setOnAction(event -> {
                match.sendChat(phrase);
                showChat("You: " + phrase.text());
            });
            menu.getItems().add(item);
        }
        return menu;
    }

    private VBox notices() {
        for (Label notice : new Label[]{connectionNotice, chatNotice}) {
            notice.getStyleClass().add("table-notice");
            notice.setVisible(false);
            notice.managedProperty().bind(notice.visibleProperty());
        }
        connectionNotice.getStyleClass().add("warning");
        VBox notices = new VBox(10, connectionNotice, chatNotice);
        notices.setAlignment(Pos.TOP_CENTER);
        notices.setPadding(new Insets(16));
        notices.setPickOnBounds(false);
        notices.setMouseTransparent(true);
        return notices;
    }

    private void showChat(String text) {
        chatNotice.setText(text);
        chatNotice.setVisible(true);
        chatTimer.playFromStart();
    }

    private void showConnection(OnlineLink.State state, String reason) {
        switch (state) {
            case RECONNECTING -> {
                showReconnecting();
                countdown.play();
            }
            case CLOSED -> {
                hideConnectionNotice();
                screen.gameOverOverlay().showEnded("Match ended", reason);
            }
            case CONNECTING, CONNECTED -> hideConnectionNotice();
        }
    }

    private void showReconnecting() {
        connectionNotice.setText("Lost the connection to " + link.remoteName() + ". Reconnecting\u2026 "
                + link.reconnectSecondsLeft() + " s");
        connectionNotice.setVisible(true);
    }

    private void hideConnectionNotice() {
        countdown.stop();
        connectionNotice.setVisible(false);
    }
}
