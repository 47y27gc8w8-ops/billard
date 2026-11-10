package com.example.billard.model;

/** Immutable 2D vector used for positions and velocities in table units. */
public record Vec2(double x, double y) {

    public static final Vec2 ZERO = new Vec2(0, 0);

    public static Vec2 fromAngle(double radians) {
        return new Vec2(Math.cos(radians), Math.sin(radians));
    }

    public Vec2 plus(Vec2 other) {
        return new Vec2(x + other.x, y + other.y);
    }

    public Vec2 minus(Vec2 other) {
        return new Vec2(x - other.x, y - other.y);
    }

    public Vec2 times(double factor) {
        return new Vec2(x * factor, y * factor);
    }

    public double dot(Vec2 other) {
        return x * other.x + y * other.y;
    }

    public double length() {
        return Math.hypot(x, y);
    }

    public double distanceTo(Vec2 other) {
        return minus(other).length();
    }

    public Vec2 normalized() {
        double length = length();
        return length == 0 ? ZERO : times(1 / length);
    }

    public Vec2 perpendicular() {
        return new Vec2(-y, x);
    }

    public double angle() {
        return Math.atan2(y, x);
    }
}
