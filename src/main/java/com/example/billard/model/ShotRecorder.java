package com.example.billard.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Listens to the physics during a shot and condenses it into a {@link ShotReport}. */
public final class ShotRecorder implements PhysicsListener {

    private final List<BallKind> pocketed = new ArrayList<>();
    private Optional<BallKind> firstContact = Optional.empty();
    private boolean railAfterContact;

    public void reset() {
        pocketed.clear();
        firstContact = Optional.empty();
        railAfterContact = false;
    }

    public ShotReport report() {
        return new ShotReport(firstContact, pocketed, railAfterContact);
    }

    @Override
    public void onBallCollision(Ball first, Ball second) {
        if (firstContact.isPresent()) {
            return;
        }
        if (first.kind() == BallKind.CUE) {
            firstContact = Optional.of(second.kind());
        } else if (second.kind() == BallKind.CUE) {
            firstContact = Optional.of(first.kind());
        }
    }

    @Override
    public void onCushionHit(Ball ball) {
        if (firstContact.isPresent()) {
            railAfterContact = true;
        }
    }

    @Override
    public void onPocketed(Ball ball) {
        pocketed.add(ball.kind());
    }
}
