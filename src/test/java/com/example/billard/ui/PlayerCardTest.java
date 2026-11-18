package com.example.billard.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.billard.model.BallKind;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PlayerCardTest {

    @BeforeAll
    static void startJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // Another test class started the toolkit first.
        }
    }

    @Test
    void cardStopsShowingTheTurnOnceItPassesEvenAfterChangesDuringTheTurn() throws Exception {
        boolean stillShooting = onFxThread(() -> {
            PlayerCard card = new PlayerCard("Player 1");
            card.update(new PlayerCard.View(true, "Open table", null, List.of()));
            card.update(new PlayerCard.View(true, "Solids", BallKind.SOLID, List.of(3)));
            card.update(new PlayerCard.View(true, "Solids", BallKind.SOLID, List.of(3, 5)));
            card.update(new PlayerCard.View(false, "Solids", BallKind.SOLID, List.of(3, 5)));
            return card.getPseudoClassStates().contains(PlayerCard.SHOOTING);
        });
        assertFalse(stillShooting);
    }

    @Test
    void cardShowsTheTurnWhileItIsThePlayersShot() throws Exception {
        boolean shooting = onFxThread(() -> {
            PlayerCard card = new PlayerCard("Player 2");
            card.update(new PlayerCard.View(false, "Open table", null, List.of()));
            card.update(new PlayerCard.View(true, "Open table", null, List.of()));
            return card.getPseudoClassStates().contains(PlayerCard.SHOOTING);
        });
        assertTrue(shooting);
    }

    @Test
    void stylesheetLightsTheCardThroughTheSameStateTheCardSets() throws Exception {
        String css;
        try (var stylesheet = PlayerCard.class.getResourceAsStream("style.css")) {
            css = new String(stylesheet.readAllBytes(), java.nio.charset.StandardCharsets.US_ASCII);
        }
        assertTrue(css.contains(".player-card:" + PlayerCard.SHOOTING.getPseudoClassName() + " {"));
        assertTrue(css.contains(".player-card:" + PlayerCard.SHOOTING.getPseudoClassName() + " .turn-marker {"));
    }

    private static <T> T onFxThread(Supplier<T> work) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(work.get());
            } catch (RuntimeException | Error failure) {
                result.completeExceptionally(failure);
            }
        });
        return result.get(10, TimeUnit.SECONDS);
    }
}
