package com.example.billard.net;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A host name or IP address with a port, as a player types it: {@code 192.168.1.20}, {@code example.org:47474} or
 * {@code [::1]:47474}. A missing port means the default one.
 */
public record ServerAddress(String host, int port) {

    private static final int MAX_PORT = 65_535;
    private static final Pattern BRACKETED = Pattern.compile("\\[([0-9A-Fa-f:.]+)](?::(\\d{1,5}))?");
    private static final Pattern PLAIN = Pattern.compile("([A-Za-z0-9.-]+)(?::(\\d{1,5}))?");

    public ServerAddress {
        if (port < 1 || port > MAX_PORT) {
            throw new IllegalArgumentException("Port must be 1.." + MAX_PORT + " but was " + port);
        }
    }

    /** {@code text} read as an address, or empty if it is not one. */
    public static Optional<ServerAddress> parse(String text, int defaultPort) {
        String trimmed = text.strip();
        for (Pattern pattern : new Pattern[]{BRACKETED, PLAIN}) {
            Matcher match = pattern.matcher(trimmed);
            if (match.matches()) {
                int port = match.group(2) == null ? defaultPort : Integer.parseInt(match.group(2));
                return port >= 1 && port <= MAX_PORT
                        ? Optional.of(new ServerAddress(match.group(1), port))
                        : Optional.empty();
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return (host.contains(":") ? "[" + host + "]" : host) + ":" + port;
    }
}
