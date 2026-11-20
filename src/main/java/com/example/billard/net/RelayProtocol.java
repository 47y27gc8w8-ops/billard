package com.example.billard.net;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The few lines a game and the relay server exchange before the relay starts passing lines between two players.
 *
 * <pre>
 * game  -&gt; relay   HOST                    open a room
 * relay -&gt; game    ROOM code secret        the room's code to share, and the host's secret for rejoining
 * game  -&gt; relay   JOIN code               take the free seat in a room
 * relay -&gt; game    JOINED secret           the guest's secret for rejoining
 * game  -&gt; relay   REJOIN code secret      come back to a room after the connection dropped
 * relay -&gt; game    REJOINED
 * relay -&gt; game    PAIRED                  both players are here; from now on every line is passed through
 * relay -&gt; game    ERROR reason            refused; the relay closes the connection
 * </pre>
 */
public final class RelayProtocol {

    public static final int DEFAULT_PORT = 47475;
    /** Room codes use letters and digits that cannot be mistaken for each other when read out. */
    public static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    public static final int CODE_LENGTH = 4;

    static final String HOST = "HOST";
    static final String ROOM = "ROOM";
    static final String JOIN = "JOIN";
    static final String JOINED = "JOINED";
    static final String REJOIN = "REJOIN";
    static final String REJOINED = "REJOINED";
    static final String PAIRED = "PAIRED";
    static final String ERROR = "ERROR";

    private static final Pattern CODE = Pattern.compile("[" + CODE_ALPHABET + "]{" + CODE_LENGTH + "}");
    private static final Pattern SECRET = Pattern.compile("[0-9a-f]{32}");

    private RelayProtocol() {
    }

    /** {@code text} as a room code, upper-cased, or empty if it is not one. */
    public static Optional<String> roomCode(String text) {
        String code = text.strip().toUpperCase(Locale.ROOT);
        return isRoomCode(code) ? Optional.of(code) : Optional.empty();
    }

    static boolean isRoomCode(String code) {
        return CODE.matcher(code).matches();
    }

    static boolean isSecret(String secret) {
        return SECRET.matcher(secret).matches();
    }
}
