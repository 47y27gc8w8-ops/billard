package com.example.billard.audio;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.AbstractPreferences;

/** A preferences store that lives only in memory, so tests never touch the real user settings. */
final class InMemoryPreferences extends AbstractPreferences {

    private final Map<String, String> values = new HashMap<>();

    InMemoryPreferences() {
        super(null, "");
    }

    @Override
    protected void putSpi(String key, String value) {
        values.put(key, value);
    }

    @Override
    protected String getSpi(String key) {
        return values.get(key);
    }

    @Override
    protected void removeSpi(String key) {
        values.remove(key);
    }

    @Override
    protected void removeNodeSpi() {
        values.clear();
    }

    @Override
    protected String[] keysSpi() {
        return values.keySet().toArray(String[]::new);
    }

    @Override
    protected String[] childrenNamesSpi() {
        return new String[0];
    }

    @Override
    protected AbstractPreferences childSpi(String name) {
        throw new UnsupportedOperationException("Not needed by the tests");
    }

    @Override
    protected void syncSpi() {
    }

    @Override
    protected void flushSpi() {
    }
}
