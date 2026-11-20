package com.example.billard.net;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/** One end of a connection that lives in memory: whatever one end sends is waiting at the other at once. */
final class InMemoryChannel implements MatchChannel {

    private final Deque<GameMessage> incoming = new ArrayDeque<>();
    private InMemoryChannel other;

    /** Two ends joined to each other. */
    static InMemoryChannel[] pair() {
        InMemoryChannel first = new InMemoryChannel();
        InMemoryChannel second = new InMemoryChannel();
        first.other = second;
        second.other = first;
        return new InMemoryChannel[]{first, second};
    }

    /** Puts {@code message} in this end's queue as if the other end had sent it. */
    void receive(GameMessage message) {
        incoming.addLast(message);
    }

    boolean hasWaiting() {
        return !incoming.isEmpty();
    }

    @Override
    public void send(GameMessage message) {
        other.receive(message);
    }

    @Override
    public Optional<GameMessage> nextIncoming() {
        return Optional.ofNullable(incoming.peekFirst());
    }

    @Override
    public void consumeIncoming() {
        incoming.pollFirst();
    }

    @Override
    public boolean isConnected() {
        return true;
    }
}
