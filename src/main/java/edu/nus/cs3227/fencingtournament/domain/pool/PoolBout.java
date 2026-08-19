package edu.nus.cs3227.fencingtournament.domain.pool;

import java.util.UUID;

/** One scheduled pairing in a pool. A null score represents an uncompleted bout in this skeleton. */
public record PoolBout(UUID id, UUID firstFencerId, UUID secondFencerId, BoutScore score) {
    public PoolBout {
        if (id == null || firstFencerId == null || secondFencerId == null) {
            throw new IllegalArgumentException("Pool bout IDs and participants must not be null.");
        }
        if (firstFencerId.equals(secondFencerId)) {
            throw new IllegalArgumentException("A pool bout requires two distinct fencers.");
        }
    }

    public boolean isComplete() {
        return score != null;
    }

    public PoolBout withScore(BoutScore newScore) {
        return new PoolBout(id, firstFencerId, secondFencerId, newScore);
    }
}

