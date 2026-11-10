package com.example.billard.model;

/** A ball on the table. Number 0 is the cue ball. */
public final class Ball {

    private final int number;
    private final BallKind kind;
    private Vec2 position;
    private Vec2 velocity = Vec2.ZERO;
    private boolean pocketed;

    public Ball(int number, Vec2 position) {
        this.number = number;
        this.kind = BallKind.fromNumber(number);
        this.position = position;
    }

    public int number() {
        return number;
    }

    public BallKind kind() {
        return kind;
    }

    public Vec2 position() {
        return position;
    }

    public Vec2 velocity() {
        return velocity;
    }

    public boolean isPocketed() {
        return pocketed;
    }

    public boolean isMoving() {
        return !pocketed && velocity.dot(velocity) > 0;
    }

    public void setPosition(Vec2 position) {
        this.position = position;
    }

    public void setVelocity(Vec2 velocity) {
        this.velocity = velocity;
    }

    public void pocket() {
        pocketed = true;
        velocity = Vec2.ZERO;
    }

    /** Puts the ball back on the table, at rest. */
    public void putOnTable(Vec2 spot) {
        pocketed = false;
        velocity = Vec2.ZERO;
        position = spot;
    }
}
