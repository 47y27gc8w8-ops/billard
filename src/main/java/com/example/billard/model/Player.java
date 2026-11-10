package com.example.billard.model;

public enum Player {
    ONE("Player 1"),
    TWO("Player 2");

    private final String label;

    Player(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public Player opponent() {
        return this == ONE ? TWO : ONE;
    }
}
