package com.example.billard.net;

import java.util.Optional;
import java.util.regex.Pattern;

/** The name a player shows to their opponent: letters, digits, spaces and a little punctuation, at most 16 long. */
public final class PlayerName {

    public static final int MAX_LENGTH = 16;

    private static final Pattern ALLOWED = Pattern.compile("[\\p{L}\\p{N}_.'-]+( [\\p{L}\\p{N}_.'-]+)*");

    private PlayerName() {
    }

    /** {@code raw} trimmed and with runs of spaces closed up, or empty if what is left is not an allowed name. */
    public static Optional<String> clean(String raw) {
        String name = raw.strip().replaceAll(" +", " ");
        return isValid(name) ? Optional.of(name) : Optional.empty();
    }

    public static boolean isValid(String name) {
        return !name.isEmpty() && name.length() <= MAX_LENGTH && ALLOWED.matcher(name).matches();
    }

    /** {@code name} if valid, otherwise a {@link ProtocolException}; for names received over the network. */
    static String requireValid(String name) {
        if (!isValid(name)) {
            throw new ProtocolException("Not an allowed player name");
        }
        return name;
    }
}
