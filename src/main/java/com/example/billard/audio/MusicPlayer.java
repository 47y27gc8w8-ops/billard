package com.example.billard.audio;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.random.RandomGenerator;
import javafx.beans.value.ObservableDoubleValue;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;

/**
 * Plays background music one track after another in shuffled order, at a volume that follows a setting. Must be
 * used on the JavaFX thread. If a track cannot be played it is skipped; if none can, the music stays silent.
 */
public final class MusicPlayer implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(MusicPlayer.class.getName());

    private final Playlist<LoungeMusic.Track> playlist;
    private final ObservableDoubleValue volume;
    private final int trackCount;
    private MediaPlayer current;
    private int failuresInARow;

    public MusicPlayer(List<LoungeMusic.Track> tracks, ObservableDoubleValue volume, RandomGenerator random) {
        this.playlist = new Playlist<>(tracks, random);
        this.volume = volume;
        this.trackCount = tracks.size();
    }

    public void start() {
        if (current == null) {
            playNext();
        }
    }

    @Override
    public void close() {
        if (current != null) {
            current.dispose();
            current = null;
        }
    }

    private void playNext() {
        close();
        LoungeMusic.Track track = playlist.next();
        try {
            current = playerFor(track);
            current.play();
        } catch (MediaException | IllegalStateException unplayable) {
            skip(track, unplayable);
        }
    }

    private MediaPlayer playerFor(LoungeMusic.Track track) {
        MediaPlayer player = new MediaPlayer(new Media(track.location().toExternalForm()));
        player.volumeProperty().bind(volume);
        player.setOnPlaying(() -> failuresInARow = 0);
        player.setOnEndOfMedia(this::playNext);
        player.setOnError(() -> skip(track, player.getError()));
        return player;
    }

    private void skip(LoungeMusic.Track track, Exception reason) {
        LOG.log(Level.WARNING, "Cannot play music track " + track.title(), reason);
        failuresInARow++;
        if (failuresInARow < trackCount) {
            playNext();
        } else {
            LOG.warning("No music track could be played; music stays off");
            close();
        }
    }
}
