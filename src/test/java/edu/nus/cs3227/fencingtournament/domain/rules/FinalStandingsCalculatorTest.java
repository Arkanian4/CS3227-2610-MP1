package edu.nus.cs3227.fencingtournament.domain.rules;

import edu.nus.cs3227.fencingtournament.domain.elimination.EliminationBracket;
import edu.nus.cs3227.fencingtournament.domain.elimination.EliminationMatch;
import edu.nus.cs3227.fencingtournament.domain.pool.BoutScore;
import edu.nus.cs3227.fencingtournament.domain.standings.FinalStanding;
import edu.nus.cs3227.fencingtournament.domain.standings.OverallStanding;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FinalStandingsCalculatorTest {
    private final BracketGenerator bracketGenerator = new BracketGenerator();
    private final FinalStandingsCalculator calculator = new FinalStandingsCalculator();

    @Test
    void ranksChampionFinalistSemiFinalistsAndNonQualifiersDeterministically() {
        List<UUID> fencers = java.util.stream.IntStream.range(0, 10).mapToObj(ignored -> UUID.randomUUID()).toList();
        EliminationBracket completed = completeWithFirstFencerWinning(bracketGenerator.generate(fencers.subList(0, 8)));

        List<FinalStanding> results = calculator.calculate(postPoolStandings(fencers), completed);

        assertEquals(fencers, results.stream().map(FinalStanding::fencerId).toList());
        assertEquals(List.of("Champion", "Runner-up", "Semi-finalist", "Semi-finalist", "Quarter-finalist", "Quarter-finalist", "Quarter-finalist", "Quarter-finalist", "Did not qualify for DE", "Did not qualify for DE"),
                results.stream().map(FinalStanding::directEliminationFinish).toList());
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), results.stream().map(FinalStanding::place).toList());
    }

    @Test
    void rejectsAnIncompleteBracket() {
        List<UUID> fencers = java.util.stream.IntStream.range(0, 2).mapToObj(ignored -> UUID.randomUUID()).toList();

        assertThrows(IllegalStateException.class, () -> calculator.calculate(postPoolStandings(fencers), bracketGenerator.generate(fencers)));
    }

    @Test
    void lowerPostPoolSeedAdvancesPastAndFinishesAboveDefeatedHigherSeed() {
        List<UUID> fencers = java.util.stream.IntStream.range(0, 4).mapToObj(ignored -> UUID.randomUUID()).toList();
        EliminationBracket bracket = bracketGenerator.generate(fencers);
        List<EliminationMatch> opening = bracket.matches().stream().filter(match -> match.round() == 1)
                .sorted(java.util.Comparator.comparingInt(EliminationMatch::position)).toList();
        bracket = bracket.recordResult(opening.getFirst().id(), new BoutScore(0, 15), 15);
        bracket = bracket.recordResult(opening.get(1).id(), new BoutScore(15, 0), 15);
        EliminationMatch finalMatch = bracket.matches().stream().filter(EliminationMatch::isReady).findFirst().orElseThrow();
        bracket = bracket.recordResult(finalMatch.id(), new BoutScore(15, 0), 15);

        List<FinalStanding> results = calculator.calculate(postPoolStandings(fencers), bracket);

        assertEquals(fencers.get(3), results.getFirst().fencerId());
        assertEquals(fencers.get(1), results.get(1).fencerId());
        assertEquals(fencers.get(0), results.get(2).fencerId());
    }

    private static EliminationBracket completeWithFirstFencerWinning(EliminationBracket bracket) {
        EliminationBracket current = bracket;
        while (!current.isComplete()) {
            EliminationMatch ready = current.matches().stream().filter(EliminationMatch::isReady).findFirst().orElseThrow();
            current = current.recordResult(ready.id(), new BoutScore(15, 0), 15);
        }
        return current;
    }

    private static List<OverallStanding> postPoolStandings(List<UUID> fencers) {
        return java.util.stream.IntStream.range(0, fencers.size())
                .mapToObj(index -> new OverallStanding(fencers.get(index), 4, 4, 1.0, 20, 4, 16, index + 1, index + 1)).toList();
    }
}
