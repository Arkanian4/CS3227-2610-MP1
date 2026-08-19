package edu.nus.cs3227.fencingtournament.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DomainSkeletonTest {
    @Test
    void fencerRetainsItsIdentityAndDisplayName() {
        UUID id = UUID.randomUUID();
        Fencer fencer = new Fencer(id, "Alex Tan");

        assertEquals(id, fencer.id());
        assertEquals("Alex Tan", fencer.name());
    }
}

