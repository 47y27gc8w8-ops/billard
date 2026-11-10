package com.example.billard.model;

public enum Foul {
    NONE(""),
    CUE_POCKETED("the cue ball was pocketed"),
    NO_CONTACT("the cue ball hit nothing"),
    WRONG_BALL_FIRST("the wrong ball was hit first"),
    NO_RAIL("no ball reached a cushion");

    private final String description;

    Foul(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }

    public boolean isFoul() {
        return this != NONE;
    }
}
