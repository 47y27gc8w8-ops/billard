package com.example.billard.model;

import java.util.Optional;

/**
 * What the bot does on its turn.
 *
 * @param cueSpot where it puts the cue ball first, when it has ball in hand and wants it moved
 * @param shot    the stroke it plays
 */
public record BotMove(Optional<Vec2> cueSpot, Shot shot) {

    static BotMove from(Shot shot) {
        return new BotMove(Optional.empty(), shot);
    }

    static BotMove placedAt(Vec2 cueSpot, Shot shot) {
        return new BotMove(Optional.of(cueSpot), shot);
    }

    BotMove withShot(Shot newShot) {
        return new BotMove(cueSpot, newShot);
    }
}
