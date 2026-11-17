package com.example.billard.model;

import java.util.ArrayList;
import java.util.List;

/** Passes every physics event on to each listener, in the order they were added. */
final class PhysicsListenerGroup implements PhysicsListener {

    private final List<PhysicsListener> listeners = new ArrayList<>();

    void add(PhysicsListener listener) {
        listeners.add(listener);
    }

    @Override
    public void onCueStruck(double power) {
        listeners.forEach(listener -> listener.onCueStruck(power));
    }

    @Override
    public void onBallCollision(Ball first, Ball second, double impactSpeed) {
        listeners.forEach(listener -> listener.onBallCollision(first, second, impactSpeed));
    }

    @Override
    public void onCushionHit(Ball ball, double impactSpeed) {
        listeners.forEach(listener -> listener.onCushionHit(ball, impactSpeed));
    }

    @Override
    public void onPocketed(Ball ball, double speed) {
        listeners.forEach(listener -> listener.onPocketed(ball, speed));
    }
}
