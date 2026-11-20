package com.example.billard.net;

import java.io.IOException;

/** The other end answered but said no, for example to an unknown room code; trying again would not help. */
public final class ConnectionRefusedException extends IOException {

    public ConnectionRefusedException(String reason) {
        super(reason);
    }
}
