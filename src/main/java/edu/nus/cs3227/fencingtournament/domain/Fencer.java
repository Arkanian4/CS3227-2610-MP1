package edu.nus.cs3227.fencingtournament.domain;

import java.util.UUID;

/** A participant in the internal club tournament. Club affiliation is intentionally not stored. */
public record Fencer(UUID id, String name) {
    public Fencer {
        if (id == null) {
            throw new IllegalArgumentException("Fencer ID must not be null.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Fencer name must not be blank.");
        }
        name = name.trim();
    }

    public static Fencer create(String name) {
        return new Fencer(UUID.randomUUID(), name);
    }
}
