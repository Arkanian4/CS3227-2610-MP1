package edu.nus.cs3227.fencingtournament.domain.pool;

import java.util.UUID;

/** One scheduled pairing in a pool. A null score represents an uncompleted bout in this skeleton. */
public record PoolBout(UUID id, UUID firstFencerId, UUID secondFencerId, BoutScore score) {
}

