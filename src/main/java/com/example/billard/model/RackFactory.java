package com.example.billard.model;

import java.util.ArrayList;
import java.util.List;

/** Builds the opening arrangement: cue ball on the head spot, fifteen balls racked on the foot spot. */
public final class RackFactory {

    /** Ball numbers by rack row, apex first. The eight sits in the middle, a solid and a stripe in the back corners. */
    private static final int[][] ROWS = {
            {1},
            {9, 2},
            {3, 8, 10},
            {11, 4, 5, 12},
            {6, 13, 14, 7, 15}
    };
    private static final double GAP_BETWEEN_BALLS = 0.6;

    private RackFactory() {
    }

    public static List<Ball> standardRack(Table table) {
        List<Ball> balls = new ArrayList<>(16);
        balls.add(new Ball(0, table.headSpot()));
        double spacing = 2 * table.ballRadius() + GAP_BETWEEN_BALLS;
        double rowDepth = spacing * Math.sqrt(3) / 2;
        Vec2 apex = table.footSpot();
        for (int row = 0; row < ROWS.length; row++) {
            addRow(balls, ROWS[row], apex.plus(new Vec2(row * rowDepth, -row * spacing / 2)), spacing);
        }
        return balls;
    }

    private static void addRow(List<Ball> balls, int[] numbers, Vec2 rowStart, double spacing) {
        for (int slot = 0; slot < numbers.length; slot++) {
            balls.add(new Ball(numbers[slot], rowStart.plus(new Vec2(0, slot * spacing))));
        }
    }
}
