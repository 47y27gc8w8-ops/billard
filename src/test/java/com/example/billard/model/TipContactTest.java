package com.example.billard.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TipContactTest {

    private static final Vec2 EAST = new Vec2(1, 0);

    @Test
    void aPointOutsideTheBallIsPulledOntoItsRim() {
        TipContact contact = new TipContact(3, 4);

        assertEquals(1.0, Math.hypot(contact.right(), contact.up()), 1e-9);
        assertEquals(0.6, contact.right(), 1e-9);
    }

    @Test
    void aPointInsideTheBallIsKept() {
        TipContact contact = new TipContact(0.2, -0.3);

        assertEquals(0.2, contact.right());
        assertEquals(-0.3, contact.up());
    }

    @Test
    void centreHitCarriesNoSpin() {
        assertTrue(TipContact.CENTER.isCentered());
        assertEquals(Vec2.ZERO, TipContact.CENTER.rollSpin(EAST, 1000));
        assertEquals(0.0, TipContact.CENTER.sideSpin(1000), 1e-9);
    }

    @Test
    void highHitSpinsWithTheShotAndLowHitAgainstIt() {
        assertTrue(new TipContact(0, 0.5).rollSpin(EAST, 1000).x() > 0);
        assertTrue(new TipContact(0, -0.5).rollSpin(EAST, 1000).x() < 0);
    }

    @Test
    void sideSpinIsOppositeForLeftAndRightHits() {
        assertTrue(new TipContact(0.5, 0).sideSpin(1000) < 0);
        assertTrue(new TipContact(-0.5, 0).sideSpin(1000) > 0);
        assertFalse(new TipContact(0.5, 0).isCentered());
    }
}
