package com.example.billard.ui;

import java.util.prefs.Preferences;

/** What the online lobby remembers between runs, so a player types their name and addresses only once. */
public final class OnlinePreferences {

    private static final String NAME = "online.name";
    private static final String USE_RELAY = "online.useRelay";
    private static final String RELAY_ADDRESS = "online.relayAddress";
    private static final String JOIN_ADDRESS = "online.joinAddress";

    private final Preferences store;

    public OnlinePreferences(Preferences store) {
        this.store = store;
    }

    String name() {
        return store.get(NAME, "");
    }

    void setName(String name) {
        store.put(NAME, name);
    }

    boolean usesRelay() {
        return store.getBoolean(USE_RELAY, false);
    }

    void setUsesRelay(boolean useRelay) {
        store.putBoolean(USE_RELAY, useRelay);
    }

    String relayAddress() {
        return store.get(RELAY_ADDRESS, "");
    }

    void setRelayAddress(String address) {
        store.put(RELAY_ADDRESS, address);
    }

    /** The host address last joined directly; room codes are not kept, since each is used once. */
    String joinAddress() {
        return store.get(JOIN_ADDRESS, "");
    }

    void setJoinAddress(String address) {
        store.put(JOIN_ADDRESS, address);
    }
}
