package edu.nus.cs3227.fencingtournament.domain;

import java.util.List;
import java.util.UUID;

/** The ordered fencer IDs used to derive one-based tournament seeds. */
public record Seeding(List<UUID> fencerIds) {
    public Seeding {
        fencerIds = List.copyOf(fencerIds);
    }
}

