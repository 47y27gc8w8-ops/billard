package com.example.billard.audio;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LoungeMusicTest {

    @Test
    void everyTrackIsInTheBuild() {
        for (LoungeMusic.Track track : LoungeMusic.TRACKS) {
            assertDoesNotThrow(track::location, track.title());
        }
    }

    @Test
    void creditNamesEveryTitleTheAuthorAndTheLicence() {
        for (LoungeMusic.Track track : LoungeMusic.TRACKS) {
            assertTrue(LoungeMusic.CREDIT.contains(track.title()), track.title());
        }
        assertTrue(LoungeMusic.CREDIT.contains("Kevin MacLeod (incompetech.com)"));
        assertTrue(LoungeMusic.CREDIT.contains("Creative Commons: By Attribution 4.0"));
    }
}
