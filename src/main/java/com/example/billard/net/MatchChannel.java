package com.example.billard.net;

import java.util.Optional;

/**
 * The game's view of the connection to the other player: send a message, look at the oldest one received, and
 * take it off the queue once it has been dealt with. Messages arrive in the order they were sent.
 */
public interface MatchChannel {

    /** Sends {@code message}; one that must arrive waits through a dropped connection and is sent on reconnect. */
    void send(GameMessage message);

    /** The oldest received message not yet consumed, left in place. */
    Optional<GameMessage> nextIncoming();

    /** Removes the message {@link #nextIncoming()} returned. Does nothing if there is none. */
    void consumeIncoming();

    /** True while the other player can be reached right now. */
    boolean isConnected();
}
