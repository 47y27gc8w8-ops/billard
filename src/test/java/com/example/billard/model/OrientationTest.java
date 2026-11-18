package com.example.billard.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class OrientationTest {

    private static final double RADIUS = 10;
    private static final double TOLERANCE = 1e-9;

    @Test
    void uprightBallShowsItsNumberToTheViewer() {
        double[] m = Orientation.UPRIGHT.matrix();
        assertEquals(1, m[8], TOLERANCE);
    }

    @Test
    void rollingRightCarriesTheTopOfTheBallToTheRight() {
        double quarterTurn = Math.PI / 2 * RADIUS;
        double[] m = Orientation.UPRIGHT.rolled(new Vec2(quarterTurn, 0), RADIUS).matrix();
        // The number face (column 2) now points along +x and no longer up.
        assertEquals(1, m[2], TOLERANCE);
        assertEquals(0, m[5], TOLERANCE);
        assertEquals(0, m[8], TOLERANCE);
    }

    @Test
    void rollingDownTheTableCarriesTheTopDownTheTable() {
        double[] m = Orientation.UPRIGHT.rolled(new Vec2(0, Math.PI / 2 * RADIUS), RADIUS).matrix();
        assertEquals(0, m[2], TOLERANCE);
        assertEquals(1, m[5], TOLERANCE);
    }

    @Test
    void rollingOneCircumferenceInSmallStepsComesBackUpright() {
        Orientation orientation = Orientation.UPRIGHT;
        int steps = 1000;
        Vec2 step = new Vec2(3, 4).normalized().times(2 * Math.PI * RADIUS / steps);
        for (int i = 0; i < steps; i++) {
            orientation = orientation.rolled(step, RADIUS);
        }
        assertEquals(1, orientation.matrix()[8], 1e-6);
        assertEquals(1, Math.abs(orientation.w()), 1e-6);
    }

    @Test
    void spinningClockwiseTurnsTheRightSideDownTheScreen() {
        double[] m = Orientation.UPRIGHT.spun(Math.PI / 2).matrix();
        // The ball's x axis (column 0) now points down the screen, +y; the number still faces up.
        assertEquals(0, m[0], TOLERANCE);
        assertEquals(1, m[3], TOLERANCE);
        assertEquals(1, m[8], TOLERANCE);
    }

    @Test
    void movingBallTurnsInThePhysicsEngine() {
        Table table = Table.standard();
        Ball ball = new Ball(3, new Vec2(table.width() / 2, table.height() / 2));
        ball.setVelocity(new Vec2(200, 0));
        new PhysicsEngine(table, new PhysicsListener() { }).advance(List.of(ball), 0.05);
        double[] m = ball.orientation().matrix();
        assertTrue(m[2] > 0.1, "number face should tip toward the direction of travel, was " + m[2]);
        assertTrue(m[8] < 1 - 1e-3);
    }
}
