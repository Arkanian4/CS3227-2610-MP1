package edu.nus.cs3227.fencingtournament.domain;

import edu.nus.cs3227.fencingtournament.domain.standings.TieBreakPolicy;

/** Configuration required by the agreed MVP tournament rules. */
public record TournamentSettings(
        int targetPoolSize,
        int poolBoutScoreLimit,
        int eliminationBoutScoreLimit,
        int advancingFencerCount,
        TieBreakPolicy tieBreakPolicy) {
}

