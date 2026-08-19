package edu.nus.cs3227.fencingtournament.domain.pool;

import java.util.List;
import java.util.UUID;

/** A fixed round-robin group and its scheduled bouts. */
public record Pool(UUID id, String name, List<UUID> memberIds, List<PoolBout> bouts) {
    public Pool {
        memberIds = List.copyOf(memberIds);
        bouts = List.copyOf(bouts);
    }
}

