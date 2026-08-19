package edu.nus.cs3227.fencingtournament.domain;

import java.util.UUID;

/** A participant in the internal club tournament. Club affiliation is intentionally not stored. */
public record Fencer(UUID id, String name) {
}

