package com.example.billard.net;

import com.example.billard.model.Shot;
import com.example.billard.model.TableState;
import com.example.billard.model.TipContact;
import com.example.billard.model.Vec2;

/**
 * What one copy of an online match tells the other. Most messages must arrive, in order, even across a reconnect;
 * the live ones only show what the shooter is doing right now and are simply replaced by the next.
 */
public sealed interface GameMessage {

    /** True for messages that may be lost, because the next one of the same kind makes them obsolete. */
    default boolean isLive() {
        return false;
    }

    /** Where the shooter is pointing the cue, how hard and with which spin, while lining up. */
    record Aim(double angle, double power, TipContact tip) implements GameMessage {
        @Override
        public boolean isLive() {
            return true;
        }
    }

    /** Where the shooter is holding the cue ball while they have ball in hand. */
    record CueBallMoved(Vec2 spot) implements GameMessage {
        @Override
        public boolean isLive() {
            return true;
        }
    }

    /** The shooter put the cue ball down on {@code spot} and is now aiming. */
    record CueBallPlaced(Vec2 spot) implements GameMessage {
    }

    /** The shooter struck the cue ball. */
    record ShotTaken(Shot shot) implements GameMessage {
    }

    /** The shooter's balls have stopped, and this is how the table and the match stand now. */
    record Settled(TableState state) implements GameMessage {
    }

    record Chat(QuickChat phrase) implements GameMessage {
    }

    /** The sender would like to play again once this match is over. */
    record RematchRequest() implements GameMessage {
    }
}
