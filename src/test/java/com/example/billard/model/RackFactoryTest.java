package com.example.billard.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class RackFactoryTest {

    private final Table table = Table.standard();
    private final List<Ball> balls = RackFactory.standardRack(table);

    @Test
    void containsEveryBallNumberExactlyOnce() {
        Set<Integer> numbers = balls.stream().map(Ball::number).collect(Collectors.toSet());

        assertEquals(16, balls.size());
        assertEquals(16, numbers.size());
    }

    @Test
    void noTwoBallsOverlapAtTheStart() {
        for (int i = 0; i < balls.size(); i++) {
            for (int j = i + 1; j < balls.size(); j++) {
                double distance = balls.get(i).position().distanceTo(balls.get(j).position());
                assertTrue(distance >= 2 * table.ballRadius(), "balls " + i + " and " + j + " overlap");
            }
        }
    }

    @Test
    void theEightBallSitsInTheMiddleOfTheThirdRow() {
        Ball eight = balls.stream().filter(ball -> ball.number() == 8).findFirst().orElseThrow();

        assertEquals(table.height() / 2, eight.position().y(), 1e-9);
    }

    @Test
    void theCueBallStartsOnTheHeadSpot() {
        assertEquals(table.headSpot(), balls.get(0).position());
    }
}
