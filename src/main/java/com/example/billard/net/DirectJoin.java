package com.example.billard.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/** Joins a match hosted directly on another computer, at its address and port. */
public final class DirectJoin implements Connector {

    private static final int CONNECT_TIMEOUT_MILLIS = 4000;

    private final ServerAddress address;
    private volatile Socket attempt;
    private volatile boolean closed;

    public DirectJoin(ServerAddress address) {
        this.address = address;
    }

    @Override
    public LineChannel connect() throws IOException {
        Socket socket = new Socket();
        attempt = socket;
        if (closed) {
            socket.close();
            throw new IOException("Joining was cancelled");
        }
        socket.connect(new InetSocketAddress(address.host(), address.port()), CONNECT_TIMEOUT_MILLIS);
        socket.setTcpNoDelay(true);
        return new LineChannel(socket);
    }

    @Override
    public void close() {
        closed = true;
        Socket socket = attempt;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // The attempt is abandoned either way.
            }
        }
    }
}
