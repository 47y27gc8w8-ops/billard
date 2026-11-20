package com.example.billard.net;

/**
 * The preset messages players can send each other. Only these travel over the network, by name, so nothing a
 * player types ever reaches the other screen.
 */
public enum QuickChat {
    GOOD_LUCK("Good luck!"),
    NICE_SHOT("Nice shot!"),
    OOPS("Oops!"),
    THINKING("Let me think..."),
    WELL_PLAYED("Well played!"),
    GOOD_GAME("Good game!");

    private final String text;

    QuickChat(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }
}
