package com.example.billard.model;

import java.util.List;
import java.util.Optional;

/** Everything the rules need to know about one finished shot. */
public record ShotReport(Optional<BallKind> firstContact, List<BallKind> pocketed, boolean railAfterContact) {

    public ShotReport {
        pocketed = List.copyOf(pocketed);
    }

    public boolean pocketedCue() {
        return pocketed.contains(BallKind.CUE);
    }

    public boolean pocketedEight() {
        return pocketed.contains(BallKind.EIGHT);
    }
}
