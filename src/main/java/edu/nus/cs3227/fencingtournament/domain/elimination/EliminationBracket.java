package edu.nus.cs3227.fencingtournament.domain.elimination;

import edu.nus.cs3227.fencingtournament.domain.pool.BoutScore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Fixed direct-elimination topology and progressive results. */
public record EliminationBracket(UUID id, int size, List<EliminationMatch> matches) {
    public EliminationBracket { matches = List.copyOf(matches); }
    public boolean isComplete() { return matches.stream().filter(match -> match.nextMatchId() == null).allMatch(EliminationMatch::isResolved); }
    public EliminationBracket recordResult(UUID matchId, BoutScore score, int scoreLimit) {
        if (matchId == null || score == null || score.firstScore() > scoreLimit || score.secondScore() > scoreLimit
                || Math.max(score.firstScore(), score.secondScore()) != scoreLimit) throw new IllegalArgumentException("DE result must use the configured winning score.");
        List<EliminationMatch> updated = new ArrayList<>(matches); int index = indexOf(updated, matchId); EliminationMatch match = updated.get(index);
        if (!match.isReady()) throw new IllegalStateException("This DE bout is not ready for a result.");
        UUID winner = score.firstFencerWon() ? match.firstSlot().fencerId() : match.secondSlot().fencerId();
        EliminationMatch resolved = match.resolve(winner, score); updated.set(index, resolved); propagate(updated, resolved, winner); resolveByes(updated);
        return new EliminationBracket(id, size, updated);
    }
    public EliminationBracket resolveByes() { List<EliminationMatch> updated = new ArrayList<>(matches); resolveByes(updated); return new EliminationBracket(id, size, updated); }
    private static void resolveByes(List<EliminationMatch> matches) {
        boolean changed;
        do { changed = false;
            for (int index = 0; index < matches.size(); index++) {
                EliminationMatch match = matches.get(index);
                if (match.isResolved() || !match.firstSlot().resolved() || !match.secondSlot().resolved()) continue;
                UUID first = match.firstSlot().fencerId(); UUID second = match.secondSlot().fencerId();
                if ((first == null) == (second == null)) continue;
                UUID winner = first == null ? second : first; EliminationMatch resolved = match.resolve(winner, null);
                matches.set(index, resolved); propagate(matches, resolved, winner); changed = true;
            }
        } while (changed);
    }
    private static void propagate(List<EliminationMatch> matches, EliminationMatch match, UUID winner) {
        if (match.nextMatchId() == null) return; int nextIndex = indexOf(matches, match.nextMatchId());
        matches.set(nextIndex, matches.get(nextIndex).withSlot(match.nextMatchSlot(), winner));
    }
    private static int indexOf(List<EliminationMatch> matches, UUID id) {
        for (int index = 0; index < matches.size(); index++) if (matches.get(index).id().equals(id)) return index;
        throw new IllegalArgumentException("Match does not belong to this bracket.");
    }
}
