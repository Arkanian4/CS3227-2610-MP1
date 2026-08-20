package edu.nus.cs3227.fencingtournament.domain.rules;

import edu.nus.cs3227.fencingtournament.domain.elimination.EliminationBracket;
import edu.nus.cs3227.fencingtournament.domain.elimination.EliminationMatch;
import edu.nus.cs3227.fencingtournament.domain.standings.FinalStanding;
import edu.nus.cs3227.fencingtournament.domain.standings.OverallStanding;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Derives final places from a completed DE bracket.
 * Champion and final loser occupy first and second; other DE losers are ordered by the round
 * in which they were eliminated, then by post-pool rank. Non-qualifiers follow by post-pool rank.
 */
public final class FinalStandingsCalculator {
    public List<FinalStanding> calculate(List<OverallStanding> postPoolStandings, EliminationBracket bracket) {
        if (postPoolStandings == null || bracket == null || !bracket.isComplete()) {
            throw new IllegalStateException("Final standings require a completed elimination bracket.");
        }
        Map<UUID, OverallStanding> byFencer = new HashMap<>();
        for (OverallStanding standing : postPoolStandings) byFencer.put(standing.fencerId(), standing);
        EliminationMatch finalMatch = bracket.matches().stream().filter(match -> match.nextMatchId() == null)
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Elimination bracket has no final."));
        if (finalMatch.score() == null) throw new IllegalStateException("The final must be fenced to calculate final standings.");

        UUID champion = finalMatch.winnerId();
        UUID runnerUp = finalMatch.score().firstFencerWon() ? finalMatch.secondSlot().fencerId() : finalMatch.firstSlot().fencerId();
        int finalRound = finalMatch.round();
        Set<UUID> qualified = new HashSet<>();
        Map<UUID, Integer> eliminationRound = new HashMap<>();
        for (EliminationMatch match : bracket.matches()) {
            if (match.round() == 1) {
                if (match.firstSlot().fencerId() != null) qualified.add(match.firstSlot().fencerId());
                if (match.secondSlot().fencerId() != null) qualified.add(match.secondSlot().fencerId());
            }
            if (match.score() != null) {
                UUID loser = match.score().firstFencerWon() ? match.secondSlot().fencerId() : match.firstSlot().fencerId();
                eliminationRound.put(loser, match.round());
            }
        }
        requireStanding(byFencer, champion); requireStanding(byFencer, runnerUp);
        List<UUID> ordered = new ArrayList<>(List.of(champion, runnerUp));
        eliminationRound.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(champion) && !entry.getKey().equals(runnerUp))
                .sorted(Comparator.<Map.Entry<UUID, Integer>>comparingInt(Map.Entry::getValue).reversed()
                        .thenComparingInt(entry -> requireStanding(byFencer, entry.getKey()).rank()))
                .map(Map.Entry::getKey).forEach(ordered::add);
        postPoolStandings.stream().map(OverallStanding::fencerId).filter(id -> !qualified.contains(id))
                .sorted(Comparator.comparingInt(id -> requireStanding(byFencer, id).rank())).forEach(ordered::add);

        List<FinalStanding> results = new ArrayList<>(ordered.size());
        for (int index = 0; index < ordered.size(); index++) {
            UUID fencerId = ordered.get(index); OverallStanding pool = requireStanding(byFencer, fencerId);
            results.add(new FinalStanding(fencerId, index + 1, pool.rank(), pool.victories(), pool.boutsFenced(),
                    pool.indicator(), finish(fencerId, champion, runnerUp, eliminationRound.get(fencerId), finalRound)));
        }
        return List.copyOf(results);
    }

    private static OverallStanding requireStanding(Map<UUID, OverallStanding> standings, UUID fencerId) {
        OverallStanding standing = standings.get(fencerId);
        if (standing == null) throw new IllegalArgumentException("Every DE fencer must have a post-pool standing.");
        return standing;
    }

    private static String finish(UUID fencerId, UUID champion, UUID runnerUp, Integer eliminatedRound, int finalRound) {
        if (fencerId.equals(champion)) return "Champion";
        if (fencerId.equals(runnerUp)) return "Runner-up";
        if (eliminatedRound == null) return "Did not qualify for DE";
        int roundsBeforeFinal = finalRound - eliminatedRound;
        if (roundsBeforeFinal == 1) return "Semi-finalist";
        if (roundsBeforeFinal == 2) return "Quarter-finalist";
        return "Eliminated in Round of " + (1 << (roundsBeforeFinal + 1));
    }
}
