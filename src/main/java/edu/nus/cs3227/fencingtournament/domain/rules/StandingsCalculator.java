package edu.nus.cs3227.fencingtournament.domain.rules;

import edu.nus.cs3227.fencingtournament.domain.Seeding;
import edu.nus.cs3227.fencingtournament.domain.pool.BoutScore;
import edu.nus.cs3227.fencingtournament.domain.pool.Pool;
import edu.nus.cs3227.fencingtournament.domain.pool.PoolBout;
import edu.nus.cs3227.fencingtournament.domain.standings.PoolStanding;
import edu.nus.cs3227.fencingtournament.domain.standings.OverallStanding;
import edu.nus.cs3227.fencingtournament.domain.standings.TieBreakCriterion;
import edu.nus.cs3227.fencingtournament.domain.standings.TieBreakPolicy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Calculates provisional or final standings from the recorded bouts of one pool. */
public final class StandingsCalculator {
    /** Calculates one tournament-wide ranking from all completed pool bouts. */
    public List<OverallStanding> calculateOverallStandings(List<Pool> pools, Seeding seeding,
                                                            TieBreakPolicy tieBreakPolicy) {
        if (pools == null || pools.isEmpty() || seeding == null || tieBreakPolicy == null) {
            throw new IllegalArgumentException("Pools, seeding, and tie-break policy are required.");
        }
        Map<UUID, Integer> seeds = seedMap(seeding);
        Map<UUID, Statistics> statistics = new LinkedHashMap<>();
        for (UUID fencerId : seeding.fencerIds()) statistics.put(fencerId, new Statistics(fencerId, seeds.get(fencerId)));
        for (Pool pool : pools) {
            if (pool == null) throw new IllegalArgumentException("Pools must not contain null entries.");
            for (PoolBout bout : pool.bouts()) {
                Statistics first = statistics.get(bout.firstFencerId());
                Statistics second = statistics.get(bout.secondFencerId());
                if (first == null || second == null) throw new IllegalArgumentException("Pool fencer is missing from seeding.");
                if (bout.score() == null) throw new IllegalStateException("All pool bouts must be completed before pool seeding is calculated.");
                BoutScore score = bout.score();
                first.boutsFenced++; second.boutsFenced++;
                first.touchesScored += score.firstScore(); first.touchesReceived += score.secondScore();
                second.touchesScored += score.secondScore(); second.touchesReceived += score.firstScore();
                if (score.firstFencerWon()) first.victories++; else second.victories++;
            }
        }
        validateCriteria(tieBreakPolicy);
        List<Statistics> ordered = new ArrayList<>(statistics.values());
        ordered.sort(comparator(tieBreakPolicy.criteria()));
        List<OverallStanding> result = new ArrayList<>(ordered.size());
        for (int index = 0; index < ordered.size(); index++) {
            Statistics statistic = ordered.get(index);
            result.add(new OverallStanding(statistic.fencerId, statistic.boutsFenced, statistic.victories,
                    statistic.victoryRatio(), statistic.touchesScored, statistic.touchesReceived,
                    statistic.indicator(), statistic.seed, index + 1));
        }
        return List.copyOf(result);
    }
    /**
     * Calculates standings using only completed bouts. A fencer with no completed bouts remains
     * in the result with zero statistics. If the pool is incomplete, the returned ranks are
     * provisional; callers can use {@link Pool#isComplete()} to distinguish them from final ranks.
     */
    public List<PoolStanding> calculatePoolStandings(Pool pool, Seeding seeding,
                                                       TieBreakPolicy tieBreakPolicy) {
        if (pool == null || seeding == null || tieBreakPolicy == null) {
            throw new IllegalArgumentException("Pool, seeding, and tie-break policy are required.");
        }

        Map<UUID, Integer> seeds = seedMap(seeding);
        Map<UUID, Statistics> statistics = new LinkedHashMap<>();
        for (UUID fencerId : pool.memberIds()) {
            Integer seed = seeds.get(fencerId);
            if (seed == null) {
                throw new IllegalArgumentException("Every pool fencer must have a seed.");
            }
            statistics.put(fencerId, new Statistics(fencerId, seed));
        }

        for (PoolBout bout : pool.bouts()) {
            if (!statistics.containsKey(bout.firstFencerId())
                    || !statistics.containsKey(bout.secondFencerId())) {
                throw new IllegalArgumentException("Pool bout participants must belong to the pool.");
            }
            BoutScore score = bout.score();
            if (score == null) {
                continue;
            }
            Statistics first = statistics.get(bout.firstFencerId());
            Statistics second = statistics.get(bout.secondFencerId());
            first.boutsFenced++;
            second.boutsFenced++;
            first.touchesScored += score.firstScore();
            first.touchesReceived += score.secondScore();
            second.touchesScored += score.secondScore();
            second.touchesReceived += score.firstScore();
            if (score.firstFencerWon()) {
                first.victories++;
            } else {
                second.victories++;
            }
        }

        validateCriteria(tieBreakPolicy);
        List<TieBreakCriterion> criteria = tieBreakPolicy.criteria();
        List<Statistics> ordered = new ArrayList<>(statistics.values());
        ordered.sort(comparator(criteria));

        List<PoolStanding> standings = new ArrayList<>(ordered.size());
        for (int index = 0; index < ordered.size(); index++) {
            Statistics statistic = ordered.get(index);
            standings.add(new PoolStanding(
                    statistic.fencerId,
                    statistic.boutsFenced,
                    statistic.victories,
                    statistic.victoryRatio(),
                    statistic.touchesScored,
                    statistic.touchesReceived,
                    statistic.indicator(),
                    index + 1));
        }
        return List.copyOf(standings);
    }

    private static void validateCriteria(TieBreakPolicy tieBreakPolicy) {
        List<TieBreakCriterion> criteria = tieBreakPolicy.criteria();
        if (criteria == null || criteria.stream().anyMatch(criterion -> criterion == null)) {
            throw new IllegalArgumentException("Tie-break criteria must not contain null values.");
        }
    }

    private static Comparator<Statistics> comparator(List<TieBreakCriterion> criteria) {
        return (left, right) -> {
            for (TieBreakCriterion criterion : criteria) {
                int comparison = compareByCriterion(left, right, criterion);
                if (comparison != 0) {
                    return comparison;
                }
            }
            // Seed is the deterministic final fallback, even if omitted from the policy.
            return Integer.compare(left.seed, right.seed);
        };
    }

    private static int compareByCriterion(Statistics left, Statistics right,
                                          TieBreakCriterion criterion) {
        return switch (criterion) {
            case VICTORY_RATIO -> Double.compare(right.victoryRatio(), left.victoryRatio());
            case INDICATOR -> Integer.compare(right.indicator(), left.indicator());
            case TOUCHES_SCORED -> Integer.compare(right.touchesScored, left.touchesScored);
            case SEED -> Integer.compare(left.seed, right.seed);
        };
    }

    private static Map<UUID, Integer> seedMap(Seeding seeding) {
        Map<UUID, Integer> seeds = new HashMap<>();
        List<UUID> fencerIds = seeding.fencerIds();
        for (int index = 0; index < fencerIds.size(); index++) {
            UUID fencerId = fencerIds.get(index);
            if (fencerId == null || seeds.put(fencerId, index + 1) != null) {
                throw new IllegalArgumentException("Seeding must contain unique non-null fencer IDs.");
            }
        }
        return seeds;
    }

    private static final class Statistics {
        private final UUID fencerId;
        private final int seed;
        private int boutsFenced;
        private int victories;
        private int touchesScored;
        private int touchesReceived;

        private Statistics(UUID fencerId, int seed) {
            this.fencerId = fencerId;
            this.seed = seed;
        }

        private double victoryRatio() {
            return boutsFenced == 0 ? 0.0 : (double) victories / boutsFenced;
        }

        private int indicator() {
            return touchesScored - touchesReceived;
        }
    }
}
