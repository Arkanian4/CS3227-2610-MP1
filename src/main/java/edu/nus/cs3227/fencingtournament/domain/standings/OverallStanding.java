package edu.nus.cs3227.fencingtournament.domain.standings;

import java.util.UUID;

/** Immutable calculated tournament-wide ranking used for qualification. */
public record OverallStanding(UUID fencerId, int boutsFenced, int victories, double victoryRatio,
                              int touchesScored, int touchesReceived, int indicator, int seed, int rank) {
}

