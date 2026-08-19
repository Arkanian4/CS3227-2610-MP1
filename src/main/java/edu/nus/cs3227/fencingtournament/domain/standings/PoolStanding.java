package edu.nus.cs3227.fencingtournament.domain.standings;

import java.util.UUID;

/** Immutable calculated statistics for a fencer within one pool. */
public record PoolStanding(UUID fencerId, int boutsFenced, int victories, double victoryRatio,
                           int touchesScored, int touchesReceived, int indicator, int rank) {
}

