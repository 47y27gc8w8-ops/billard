package com.example.billard.audio;

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

/** The background music shipped with the game, and the credit its licence requires us to show. */
public final class LoungeMusic {

    /** One piece of music, stored as an MP3 in the game's resources. */
    public record Track(String title, String fileName) {

        /** Where the MP3 is inside the running game; fails loudly if the build left the file out. */
        public URL location() {
            URL location = LoungeMusic.class.getResource(FOLDER + fileName);
            if (location == null) {
                throw new IllegalStateException("Music file missing from the build: " + FOLDER + fileName);
            }
            return location;
        }
    }

    public static final List<Track> TRACKS = List.of(
            new Track("Airport Lounge", "airport_lounge.mp3"),
            new Track("Backbay Lounge", "backbay_lounge.mp3"),
            new Track("Bossa Antigua", "bossa_antigua.mp3"),
            new Track("Cool Vibes", "cool_vibes.mp3"),
            new Track("Night in Venice", "night_in_venice.mp3"));

    /** CC BY 4.0 credit: titles, author, source and licence. Must stay visible in every build that ships the music. */
    public static final String CREDIT = TRACKS.stream()
            .map(track -> "\u201C" + track.title() + "\u201D")
            .collect(Collectors.joining(", ", "Music: ", " by Kevin MacLeod (incompetech.com). "))
            + "Licensed under Creative Commons: By Attribution 4.0 (creativecommons.org/licenses/by/4.0).";

    private static final String FOLDER = "/com/example/billard/music/";

    private LoungeMusic() {
    }
}
