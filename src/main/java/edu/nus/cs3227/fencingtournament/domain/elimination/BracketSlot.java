package edu.nus.cs3227.fencingtournament.domain.elimination;

import java.util.UUID;

/** A resolved participant slot in a direct-elimination match; null is unresolved in the skeleton. */
public record BracketSlot(UUID fencerId) {
}

