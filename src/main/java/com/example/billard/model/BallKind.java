package com.example.billard.model;

/** The four kinds of ball in eight-ball. SOLID and STRIPE double as a player's group. */
public enum BallKind {
    CUE,
    SOLID,
    EIGHT,
    STRIPE;

    private static final int HIGHEST_BALL_NUMBER = 15;
    private static final int EIGHT_BALL_NUMBER = 8;

    public static BallKind fromNumber(int number) {
        if (number < 0 || number > HIGHEST_BALL_NUMBER) {
            throw new IllegalArgumentException("Ball number must be 0.." + HIGHEST_BALL_NUMBER + " but was " + number);
        }
        if (number == 0) {
            return CUE;
        }
        if (number == EIGHT_BALL_NUMBER) {
            return EIGHT;
        }
        return number < EIGHT_BALL_NUMBER ? SOLID : STRIPE;
    }

    public boolean isGroup() {
        return this == SOLID || this == STRIPE;
    }

    public BallKind opposite() {
        if (!isGroup()) {
            throw new IllegalStateException(this + " is not a group and has no opposite");
        }
        return this == SOLID ? STRIPE : SOLID;
    }

    public String groupName() {
        return switch (this) {
            case SOLID -> "Solids";
            case STRIPE -> "Stripes";
            default -> throw new IllegalStateException(this + " is not a group");
        };
    }
}
