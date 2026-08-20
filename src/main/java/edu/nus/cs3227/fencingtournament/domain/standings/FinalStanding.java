package edu.nus.cs3227.fencingtournament.domain.standings;

import java.util.UUID;

/** Immutable final tournament placement, combining post-pool and DE outcomes. */
public record FinalStanding(UUID fencerId, int place, int poolSeed, int poolVictories,
                            int poolBoutsFenced, int indicator, String directEliminationFinish) {
}
