package edu.nus.cs3227.fencingtournament.domain.elimination;

import edu.nus.cs3227.fencingtournament.domain.pool.BoutScore;

import java.util.UUID;

/** One match in the fixed direct-elimination bracket topology. */
public record EliminationMatch(UUID id, int round, int position, BracketSlot firstSlot,
                               BracketSlot secondSlot, BoutScore score, UUID nextMatchId,
                               Integer nextMatchSlot) {
}

