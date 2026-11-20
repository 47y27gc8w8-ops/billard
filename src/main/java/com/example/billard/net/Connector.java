package com.example.billard.net;

import java.io.IOException;

/**
 * Opens connections to the other player, the first time and again after a drop: by waiting for them to call in,
 * by calling them, or through a relay server.
 */
public interface Connector extends AutoCloseable {

    /**
     * Waits until a connection to the other player is open. May be called again after the connection drops.
     *
     * @throws ConnectionRefusedException if the other end refused for good, so trying again is pointless
     * @throws IOException                if this attempt failed; a later one may work
     */
    LineChannel connect() throws IOException;

    /** Stops any {@link #connect()} in progress and frees what this connector holds, such as a listening port. */
    @Override
    void close();
}
