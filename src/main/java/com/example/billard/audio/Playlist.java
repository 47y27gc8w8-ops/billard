package com.example.billard.audio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Shuffle play: every item plays once in random order, then the list is shuffled again. A new round never starts
 * with the item that just finished, so nothing plays twice in a row.
 */
final class Playlist<T> {

    private final List<T> items;
    private final RandomGenerator random;
    private final List<T> upcoming = new ArrayList<>();
    private T previous;

    Playlist(List<T> items, RandomGenerator random) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("A playlist needs at least one item");
        }
        this.items = List.copyOf(items);
        this.random = random;
    }

    T next() {
        if (upcoming.isEmpty()) {
            reshuffle();
        }
        previous = upcoming.removeFirst();
        return previous;
    }

    private void reshuffle() {
        upcoming.addAll(items);
        Collections.shuffle(upcoming, random);
        if (upcoming.size() > 1 && upcoming.getFirst() == previous) {
            Collections.swap(upcoming, 0, upcoming.size() - 1);
        }
    }
}
