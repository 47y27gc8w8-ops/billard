package com.example.billard.net;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Reads the separated fields of one received line, front to back, checking each one's type and range. Everything a
 * remote player sends passes through here, so a bad field always ends in a {@link ProtocolException}.
 */
final class Fields {

    static final String NONE = "-";

    private final String[] values;
    private final String context;
    private int next;

    private Fields(String[] values, String context) {
        this.values = values;
        this.context = context;
    }

    /** The fields of {@code text} split at single spaces, to be read in order. */
    static Fields of(String text) {
        return new Fields(text.split(" ", -1), text);
    }

    /** The fields of {@code text} split at {@code separator}, a plain character, not a pattern. */
    static Fields of(String text, char separator) {
        return new Fields(text.split(Pattern.quote(String.valueOf(separator)), -1), text);
    }

    String nextText() {
        if (next >= values.length) {
            throw new ProtocolException("Too few fields in: " + abbreviated());
        }
        return values[next++];
    }

    /** Everything from the next field to the end, spaces included, for a free-text last field such as a name. */
    String restOfLine() {
        if (next >= values.length) {
            throw new ProtocolException("Too few fields in: " + abbreviated());
        }
        String rest = String.join(" ", Arrays.copyOfRange(values, next, values.length));
        next = values.length;
        return rest;
    }

    /** A finite number; NaN and the infinities are refused so they can never reach the physics. */
    double nextDouble() {
        String text = nextText();
        try {
            double value = Double.parseDouble(text);
            if (!Double.isFinite(value)) {
                throw new ProtocolException("Not a finite number: " + text);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new ProtocolException("Not a number: " + abbreviate(text), e);
        }
    }

    long nextLong() {
        String text = nextText();
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new ProtocolException("Not a whole number: " + abbreviate(text), e);
        }
    }

    boolean nextFlag() {
        return switch (nextText()) {
            case "0" -> false;
            case "1" -> true;
            default -> throw new ProtocolException("Expected 0 or 1 in: " + abbreviated());
        };
    }

    /** One of {@code type}'s constants, named exactly. */
    <E extends Enum<E>> E nextEnum(Class<E> type) {
        String text = nextText();
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equals(text)) {
                return constant;
            }
        }
        throw new ProtocolException("Unknown " + type.getSimpleName() + ": " + abbreviate(text));
    }

    /** Like {@link #nextEnum}, or empty when the field is {@value #NONE}. */
    <E extends Enum<E>> Optional<E> nextOptionalEnum(Class<E> type) {
        if (NONE.equals(peek())) {
            next++;
            return Optional.empty();
        }
        return Optional.of(nextEnum(type));
    }

    /** Fails unless every field has been read. */
    void requireEnd() {
        if (next != values.length) {
            throw new ProtocolException("Unexpected extra fields in: " + abbreviated());
        }
    }

    static String flag(boolean value) {
        return value ? "1" : "0";
    }

    static String optional(Optional<? extends Enum<?>> value) {
        return value.map(Enum::name).orElse(NONE);
    }

    private String peek() {
        return next < values.length ? values[next] : "";
    }

    private String abbreviated() {
        return abbreviate(context);
    }

    /** Received text is shown in errors only in part, so a hostile line cannot flood the log. */
    static String abbreviate(String text) {
        int limit = 60;
        return text.length() <= limit ? text : text.substring(0, limit) + "...";
    }
}
