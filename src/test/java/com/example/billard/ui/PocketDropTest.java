package com.example.billard.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.billard.model.Orientation;
import com.example.billard.model.Vec2;
import org.junit.jupiter.api.Test;

class PocketDropTest {

    private static final Vec2 POCKET = new Vec2(0, 0);
    private static final double HOLE = 34;
    private static final double BALL = 14;
    private static final Vec2 ENTRY = new Vec2(14, 30);

    private static PocketDrop drop(double speed) {
        return new PocketDrop(5, ENTRY, POCKET, HOLE, BALL, speed, Orientation.UPRIGHT);
    }

    @Test
    void startsExactlyWhereThePocketCaughtTheBallAndUncut() {
        PocketDrop.Frame frame = drop(400).frameAt(0);
        assertEquals(ENTRY.x(), frame.centre().x(), 1e-9);
        assertEquals(ENTRY.y(), frame.centre().y(), 1e-9);
        assertEquals(BALL, frame.radius(), 1e-9);
        assertEquals(0, frame.darkness(), 1e-9);
        assertTrue(ENTRY.distanceTo(POCKET) + BALL <= frame.clipRadius(), "the lip must not cut the ball at first");
    }

    @Test
    void endsSmallDarkAndInsideTheHole() {
        PocketDrop drop = drop(400);
        PocketDrop.Frame end = drop.frameAt(drop.duration());
        assertTrue(end.radius() < BALL);
        assertEquals(1, end.darkness(), 1e-9);
        assertEquals(HOLE, end.clipRadius(), 1e-9);
        assertTrue(end.centre().distanceTo(POCKET) + end.radius() <= HOLE + 1e-9, "ball should finish within the hole");
    }

    @Test
    void movesSteadilyTowardThePocketAndGetsDarker() {
        PocketDrop drop = drop(400);
        double previousDistance = Double.MAX_VALUE;
        double previousDarkness = -1;
        for (double t = 0; t <= drop.duration(); t += 0.02) {
            PocketDrop.Frame frame = drop.frameAt(t);
            double fromEntry = frame.centre().distanceTo(ENTRY);
            assertTrue(fromEntry >= -previousDistance, "never moves back");
            assertTrue(frame.darkness() >= previousDarkness, "never gets lighter");
            previousDistance = -fromEntry;
            previousDarkness = frame.darkness();
        }
    }

    @Test
    void fasterBallsRunFurtherAcrossTheHole() {
        double slow = drop(150).frameAt(1).centre().distanceTo(ENTRY);
        double fast = drop(900).frameAt(1).centre().distanceTo(ENTRY);
        assertTrue(fast > slow);
    }

    @Test
    void finishesWithinHalfASecond() {
        for (double speed : new double[]{0, 50, 400, 2000}) {
            PocketDrop drop = drop(speed);
            assertTrue(drop.duration() < 0.55, "drop at speed " + speed + " takes " + drop.duration());
            assertFalse(drop.isFinished(0));
            assertTrue(drop.isFinished(drop.duration()));
        }
    }
}
