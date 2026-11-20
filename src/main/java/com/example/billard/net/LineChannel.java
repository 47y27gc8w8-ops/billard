package com.example.billard.net;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * A TCP connection that carries UTF-8 text, one message per line. Lines are capped at {@link #MAX_LINE_BYTES} in
 * both directions, so a misbehaving peer can never make this side buffer without limit.
 *
 * <p>A handshake can be read line by line with {@link #readLine()} first; {@link #start} then hands every further
 * line to a callback on a reader thread of its own. Sending is safe from any thread. The messages are small and a
 * dead peer is noticed within seconds by the keep-alive, so sending writes straight to the socket.
 */
public final class LineChannel implements Closeable {

    public static final int MAX_LINE_BYTES = 4096;

    private final Socket socket;
    private final InputStream input;
    private final OutputStream output;
    private final Object writeLock = new Object();

    public LineChannel(Socket socket) throws IOException {
        this.socket = socket;
        this.input = new BufferedInputStream(socket.getInputStream());
        this.output = socket.getOutputStream();
    }

    /**
     * Waits for the next line.
     *
     * @throws EOFException      if the other side closed the connection
     * @throws ProtocolException if the line is longer than {@link #MAX_LINE_BYTES}
     */
    public String readLine() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int next = input.read(); next != '\n'; next = input.read()) {
            if (next == -1) {
                throw new EOFException("The connection was closed");
            }
            if (bytes.size() >= MAX_LINE_BYTES) {
                throw new ProtocolException("Received a line longer than " + MAX_LINE_BYTES + " bytes");
            }
            bytes.write(next);
        }
        String line = bytes.toString(StandardCharsets.UTF_8);
        return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
    }

    /**
     * Reads lines on a new thread, passing each to {@code onLine}, until the connection ends or breaks the rules;
     * then closes it and runs {@code onClosed} once.
     */
    public void start(Consumer<String> onLine, Runnable onClosed) {
        Thread.ofVirtual().name("billard-net-reader").start(() -> {
            try {
                while (true) {
                    onLine.accept(readLine());
                }
            } catch (IOException | ProtocolException ended) {
                close();
                onClosed.run();
            }
        });
    }

    /**
     * Sends {@code line}. Fails quietly once the connection is gone: the reader notices too and reports the end.
     *
     * @throws IllegalArgumentException if the line contains a line break or is too long to be received
     */
    public void send(String line) {
        byte[] bytes = (line + "\n").getBytes(StandardCharsets.UTF_8);
        if (line.indexOf('\n') >= 0 || line.indexOf('\r') >= 0 || bytes.length > MAX_LINE_BYTES) {
            throw new IllegalArgumentException("Cannot send a line with breaks or over " + MAX_LINE_BYTES + " bytes");
        }
        synchronized (writeLock) {
            try {
                output.write(bytes);
                output.flush();
            } catch (IOException gone) {
                close();
            }
        }
    }

    /** How long a read may wait for data before the connection counts as broken; 0 waits for ever. */
    public void setReadTimeout(int milliseconds) throws IOException {
        socket.setSoTimeout(milliseconds);
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
            // Closing is best effort; the socket is unusable either way.
        }
    }
}
