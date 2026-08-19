package edu.nus.cs3227.fencingtournament.domain.rules;

import edu.nus.cs3227.fencingtournament.domain.Seeding;
import edu.nus.cs3227.fencingtournament.domain.pool.BoutScore;
import edu.nus.cs3227.fencingtournament.domain.pool.Pool;
import edu.nus.cs3227.fencingtournament.domain.pool.PoolBout;
import edu.nus.cs3227.fencingtournament.domain.standings.PoolStanding;
import edu.nus.cs3227.fencingtournament.domain.standings.TieBreakCriterion;
import edu.nus.cs3227.fencingtournament.domain.standings.TieBreakPolicy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StandingsCalculatorTest {
    private final StandingsCalculator calculator = new StandingsCalculator();

    @Test
    void undefeatedFencerRanksFirst() {
        List<UUID> fencers = ids(3);
        Pool pool = completePool(fencers, List.of(new BoutScore(5, 1), new BoutScore(5, 3),
                new BoutScore(5, 2)));

        List<PoolStanding> standings = calculate(pool, fencers, allCriteria());

        assertEquals(fencers.get(0), standings.get(0).fencerId());
        assertEquals(2, standings.get(0).victories());
        assertEquals(1.0, standings.get(0).victoryRatio());
        assertEquals(1, standings.get(0).rank());
    }

    @Test
    void multipleVictoriesAreCountedAndRankedByVictoryRatio() {
        List<UUID> fencers = ids(4);
        Pool pool = completePool(fencers, List.of(
                new BoutScore(5, 1), new BoutScore(5, 2), new BoutScore(5, 3),
                new BoutScore(5, 4), new BoutScore(5, 1), new BoutScore(5, 2)));

        List<PoolStanding> standings = calculate(pool, fencers,
                new TieBreakPolicy(List.of(TieBreakCriterion.VICTORY_RATIO)));

        assertEquals(fencers.get(0), standings.get(0).fencerId());
        assertEquals(3, standings.get(0).victories());
        assertEquals(2, standings.get(1).victories());
    }

    @Test
    void equalVictoryCountsUseIndicatorTieBreak() {
        List<UUID> fencers = ids(3);
        Pool pool = completePool(fencers, List.of(
                new BoutScore(5, 1), // fencer 1 beats fencer 2
                new BoutScore(2, 5), // fencer 3 beats fencer 1
                new BoutScore(5, 2))); // fencer 2 beats fencer 3

        List<PoolStanding> standings = calculate(pool, fencers,
                new TieBreakPolicy(List.of(TieBreakCriterion.VICTORY_RATIO,
                        TieBreakCriterion.INDICATOR)));

        assertEquals(1, standings.get(0).victories());
        assertEquals(fencers.get(0), standings.get(0).fencerId());
        assertEquals(1, standings.get(0).indicator());
    }

    @Test
    void touchesScoredTieBreakIsAppliedAfterIndicator() {
        List<UUID> fencers = ids(4);
        List<PoolBout> bouts = List.of(
                bout(fencers.get(0), fencers.get(2), new BoutScore(4, 3)),
                bout(fencers.get(0), fencers.get(3), new BoutScore(5, 4)),
                bout(fencers.get(1), fencers.get(2), new BoutScore(5, 2)),
                bout(fencers.get(1), fencers.get(3), new BoutScore(5, 3)));
        Pool pool = new Pool(UUID.randomUUID(), "Pool A", fencers, bouts);

        List<PoolStanding> standings = calculate(pool, fencers,
                new TieBreakPolicy(List.of(TieBreakCriterion.VICTORY_RATIO,
                        TieBreakCriterion.INDICATOR, TieBreakCriterion.TOUCHES_SCORED)));

        assertEquals(fencers.get(1), standings.get(0).fencerId());
        assertEquals(2, standings.get(0).victories());
        assertEquals(10, standings.get(0).touchesScored());
    }

    @Test
    void seedTieBreakIsAppliedWhenEarlierCriteriaAreEqual() {
        List<UUID> fencers = ids(3);
        Pool pool = new Pool(UUID.randomUUID(), "Pool A", fencers, List.of(
                bout(fencers.get(0), fencers.get(1), new BoutScore(5, 0))));

        List<PoolStanding> standings = calculate(pool, fencers,
                new TieBreakPolicy(List.of(TieBreakCriterion.SEED)));

        assertEquals(fencers.get(0), standings.get(0).fencerId());
        assertEquals(fencers.get(1), standings.get(1).fencerId());
        assertEquals(fencers.get(2), standings.get(2).fencerId());
    }

    @Test
    void incompletePoolIncludesEveryFencerAndUsesOnlyCompletedBouts() {
        List<UUID> fencers = ids(3);
        Pool pool = new Pool(UUID.randomUUID(), "Pool A", fencers, List.of(
                bout(fencers.get(0), fencers.get(1), new BoutScore(5, 2)),
                bout(fencers.get(0), fencers.get(2), null),
                bout(fencers.get(1), fencers.get(2), null)));

        List<PoolStanding> standings = calculate(pool, fencers, allCriteria());

        assertEquals(3, standings.size());
        PoolStanding untouched = standings.stream()
                .filter(standing -> standing.fencerId().equals(fencers.get(2)))
                .findFirst().orElseThrow();
        assertEquals(0, untouched.boutsFenced());
        assertEquals(0, untouched.victories());
        assertEquals(0.0, untouched.victoryRatio());
        assertEquals(2, untouched.rank());
        assertEquals(0, untouched.touchesScored());
    }

    @Test
    void unusualValidScoresContributeToAllTouchStatistics() {
        List<UUID> fencers = ids(2);
        Pool pool = new Pool(UUID.randomUUID(), "Pool A", fencers, List.of(
                bout(fencers.get(0), fencers.get(1), new BoutScore(4, 3))));

        PoolStanding first = calculate(pool, fencers, allCriteria()).get(0);

        assertEquals(fencers.get(0), first.fencerId());
        assertEquals(4, first.touchesScored());
        assertEquals(3, first.touchesReceived());
        assertEquals(1, first.indicator());
    }

    @Test
    void missingSeedForPoolFencerIsRejected() {
        List<UUID> fencers = ids(2);
        Pool pool = new Pool(UUID.randomUUID(), "Pool A", fencers, List.of(
                bout(fencers.get(0), fencers.get(1), new BoutScore(5, 2))));

        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculatePoolStandings(pool,
                        new Seeding(List.of(fencers.get(0))), allCriteria()));
    }

    private List<PoolStanding> calculate(Pool pool, List<UUID> fencers, TieBreakPolicy policy) {
        return calculator.calculatePoolStandings(pool, new Seeding(fencers), policy);
    }

    private static Pool completePool(List<UUID> fencers, List<BoutScore> scores) {
        List<PoolBout> schedule = new PoolGenerator().generate(new Seeding(fencers), 5).get(0).bouts();
        List<PoolBout> completed = new ArrayList<>();
        for (int index = 0; index < schedule.size(); index++) {
            PoolBout bout = schedule.get(index);
            completed.add(bout.withScore(scores.get(index)));
        }
        return new Pool(UUID.randomUUID(), "Pool A", fencers, completed);
    }

    private static PoolBout bout(UUID first, UUID second, BoutScore score) {
        return new PoolBout(UUID.randomUUID(), first, second, score);
    }

    private static List<UUID> ids(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> UUID.nameUUIDFromBytes(("standing-fencer-" + index).getBytes()))
                .toList();
    }

    private static TieBreakPolicy allCriteria() {
        return new TieBreakPolicy(List.of(TieBreakCriterion.VICTORY_RATIO,
                TieBreakCriterion.INDICATOR, TieBreakCriterion.TOUCHES_SCORED,
                TieBreakCriterion.SEED));
    }
}
