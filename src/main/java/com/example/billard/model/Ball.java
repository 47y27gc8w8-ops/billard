package com.example.billard.model;

/** A ball on the table. Number 0 is the cue ball. */
public final class Ball {

    private final int number;
    private final BallKind kind;
    private Vec2 position;
    private Vec2 velocity = Vec2.ZERO;
    private Vec2 rollSpin = Vec2.ZERO;
    private double sideSpin;
    private Orientation orientation = Orientation.UPRIGHT;
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

    /** Follow or draw waiting to be released at the next ball-to-ball hit, as a velocity change. */
    public Vec2 rollSpin() {
        return rollSpin;
    }

    /** Sideways spin in speed units; positive turns clockwise on screen. */
    public double sideSpin() {
        return sideSpin;
    }

    public void setRollSpin(Vec2 rollSpin) {
        this.rollSpin = rollSpin;
    }

    public void setSideSpin(double sideSpin) {
        this.sideSpin = sideSpin;
    }

    /** How the ball is turned, which decides where its number and stripe show. */
    public Orientation orientation() {
        return orientation;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
    }

    /** Brings the ball to a halt and takes all spin off it. */
    public void stop() {
        velocity = Vec2.ZERO;
        rollSpin = Vec2.ZERO;
        sideSpin = 0;
    }

    public void pocket() {
        pocketed = true;
        stop();
    }

    /** Puts the ball back on the table, at rest. */
    public void putOnTable(Vec2 spot) {
        pocketed = false;
        stop();
        position = spot;
    }
}
