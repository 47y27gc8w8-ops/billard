package com.example.billard.net;

/** The other side sent something this version of the game does not accept: a malformed, oversized or unknown line. */
public final class ProtocolException extends RuntimeException {

    public ProtocolException(String message) {
        super(message);
    }

    public ProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
