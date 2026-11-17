package com.example.billard.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class PlaylistTest {

    private static final List<String> TRACKS = List.of("a", "b", "c", "d", "e");

    @Test
    void everyTrackPlaysOnceBeforeAnyRepeats() {
        Playlist<String> playlist = new Playlist<>(TRACKS, new Random(1));

        for (int round = 0; round < 20; round++) {
            List<String> played = new ArrayList<>();
            for (int i = 0; i < TRACKS.size(); i++) {
                played.add(playlist.next());
            }
            assertEquals(new HashSet<>(TRACKS), new HashSet<>(played), "round " + round);
        }
    }

    @Test
    void noTrackPlaysTwiceInARowEvenAcrossRounds() {
        for (int seed = 0; seed < 200; seed++) {
            Playlist<String> playlist = new Playlist<>(TRACKS, new Random(seed));
            String previous = playlist.next();
            for (int i = 0; i < 4 * TRACKS.size(); i++) {
                String next = playlist.next();
                assertNotEquals(previous, next, "seed " + seed);
                previous = next;
            }
        }
    }

    @Test
    void aSingleTrackSimplyRepeats() {
        Playlist<String> playlist = new Playlist<>(List.of("only"), new Random(1));

        assertEquals("only", playlist.next());
        assertEquals("only", playlist.next());
    }

    @Test
    void anEmptyPlaylistIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Playlist<>(List.of(), new Random(1)));
    }
}
