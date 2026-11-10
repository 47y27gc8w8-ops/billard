package com.example.billard.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AimPredictorTest {

    private final Table table = Table.standard();

    @Test
    void aimingStraightAtABallStopsTheGhostOneDiameterShort() {
        Ball cue = new Ball(0, new Vec2(200, 250));
        Ball target = new Ball(1, new Vec2(500, 250));

        AimPrediction prediction = AimPredictor.predict(table, List.of(cue, target), cue, 0);

        assertTrue(prediction.target().isPresent());
        assertEquals(500 - 2 * table.ballRadius(), prediction.ghostCenter().x(), 0.01);
    }

    @Test
    void aimingAtEmptyTableStopsAtTheCushion() {
        Ball cue = new Ball(0, new Vec2(200, 250));

        AimPrediction prediction = AimPredictor.predict(table, List.of(cue), cue, 0);

        assertTrue(prediction.target().isEmpty());
        assertEquals(table.width() - table.ballRadius(), prediction.ghostCenter().x(), 0.01);
    }

    @Test
    void aTargetBehindTheCueBallIsIgnored() {
        Ball cue = new Ball(0, new Vec2(500, 250));
        Ball behind = new Ball(1, new Vec2(200, 250));

        assertTrue(AimPredictor.predict(table, List.of(cue, behind), cue, 0).target().isEmpty());
    }

    @Test
    void targetDirectionPointsFromGhostThroughTheTargetCentre() {
        Ball cue = new Ball(0, new Vec2(200, 250));
        Ball target = new Ball(1, new Vec2(500, 250 + table.ballRadius()));

        Vec2 direction = AimPredictor.predict(table, List.of(cue, target), cue, 0).targetDirection().orElseThrow();

        assertTrue(direction.y() > 0, "a hit left of centre sends the target downward");
        assertEquals(1.0, direction.length(), 1e-9);
    }
}
