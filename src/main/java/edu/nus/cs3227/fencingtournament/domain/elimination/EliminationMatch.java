package edu.nus.cs3227.fencingtournament.domain.elimination;

import edu.nus.cs3227.fencingtournament.domain.pool.BoutScore;
import java.util.UUID;

/** One fixed DE match; a null score with a winner denotes an automatic bye advance. */
public record EliminationMatch(UUID id, int round, int position, BracketSlot firstSlot,
                               BracketSlot secondSlot, BoutScore score, UUID winnerId,
                               UUID nextMatchId, Integer nextMatchSlot) {
    public EliminationMatch {
        if (id == null || round < 1 || position < 0 || firstSlot == null || secondSlot == null) {
            throw new IllegalArgumentException("Elimination match structure is invalid.");
        }
    }
    public boolean isResolved() { return winnerId != null; }
    public boolean isReady() { return !isResolved() && firstSlot.resolved() && secondSlot.resolved()
            && firstSlot.fencerId() != null && secondSlot.fencerId() != null; }
    public boolean isBye() { return isResolved() && score == null; }
    public EliminationMatch withSlot(int slot, UUID fencerId) {
        return slot == 0 ? new EliminationMatch(id, round, position, BracketSlot.initial(fencerId), secondSlot,
                score, winnerId, nextMatchId, nextMatchSlot)
                : new EliminationMatch(id, round, position, firstSlot, BracketSlot.initial(fencerId),
                score, winnerId, nextMatchId, nextMatchSlot);
    }
    public EliminationMatch resolve(UUID winner, BoutScore result) {
        return new EliminationMatch(id, round, position, firstSlot, secondSlot, result, winner,
                nextMatchId, nextMatchSlot);
    }
}
