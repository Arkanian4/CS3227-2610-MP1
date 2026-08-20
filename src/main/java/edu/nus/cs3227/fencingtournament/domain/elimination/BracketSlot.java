package edu.nus.cs3227.fencingtournament.domain.elimination;

import java.util.UUID;

/** A bracket position; resolved slots may contain a fencer or a first-round bye. */
public record BracketSlot(UUID fencerId, boolean resolved) {
    public static BracketSlot initial(UUID fencerId) { return new BracketSlot(fencerId, true); }
    public static BracketSlot pending() { return new BracketSlot(null, false); }
}
