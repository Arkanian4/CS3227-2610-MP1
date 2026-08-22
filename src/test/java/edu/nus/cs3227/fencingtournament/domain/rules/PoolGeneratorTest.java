package edu.nus.cs3227.fencingtournament.domain.rules;

import edu.nus.cs3227.fencingtournament.domain.Seeding;
import edu.nus.cs3227.fencingtournament.domain.pool.Pool;
import edu.nus.cs3227.fencingtournament.domain.pool.PoolBout;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoolGeneratorTest {
    private final PoolGenerator generator = new PoolGenerator();

    @Test
    void thirtyFencersUseFiveSixPersonPoolsWithSnakeSeeding() {
        List<UUID> seeds = fencerIds(30);

        List<Pool> pools = generator.generate(new Seeding(seeds), 6);

        assertEquals(5, pools.size());
        assertEquals(List.of(1, 10, 11, 20, 21, 30), seedNumbers(pools.get(0), seeds));
        assertEquals(List.of(2, 9, 12, 19, 22, 29), seedNumbers(pools.get(1), seeds));
        assertEquals(List.of(3, 8, 13, 18, 23, 28), seedNumbers(pools.get(2), seeds));
        assertEquals(List.of(4, 7, 14, 17, 24, 27), seedNumbers(pools.get(3), seeds));
        assertEquals(List.of(5, 6, 15, 16, 25, 26), seedNumbers(pools.get(4), seeds));
    }

    @Test
    void poolSizesAreBalancedForCommonTournamentSizes() {
        assertPoolSizes(5, 5, List.of(5));
        assertPoolSizes(7, 5, List.of(3, 4));
        assertPoolSizes(10, 5, List.of(5, 5));
        assertPoolSizes(11, 5, List.of(3, 4, 4));
        assertPoolSizes(14, 6, List.of(4, 5, 5));
        assertPoolSizes(15, 6, List.of(5, 5, 5));
        assertPoolSizes(20, 6, List.of(5, 5, 5, 5));
        assertPoolSizes(22, 6, List.of(5, 5, 6, 6));
        assertPoolSizes(30, 6, List.of(6, 6, 6, 6, 6));
    }

    @Test
    void eightAndNineFencersUseBalancedSmallExceptionPools() {
        assertPoolSizes(8, 6, List.of(4, 4));
        assertPoolSizes(9, 6, List.of(4, 5));
    }

    @Test
    void sixteenFencersWithMaximumSizeEightProduceTwoCompleteBalancedPools() {
        List<UUID> seeds = fencerIds(16);

        List<Pool> pools = generator.generate(new Seeding(seeds), 8);

        assertEquals(2, pools.size());
        assertEquals(List.of(8, 8), pools.stream().map(pool -> pool.memberIds().size()).sorted().toList());
        assertEquals(List.of(1, 4, 5, 8, 9, 12, 13, 16), seedNumbers(pools.get(0), seeds));
        assertEquals(List.of(2, 3, 6, 7, 10, 11, 14, 15), seedNumbers(pools.get(1), seeds));
        List<UUID> distributed = pools.stream().flatMap(pool -> pool.memberIds().stream()).toList();
        assertEquals(seeds.size(), distributed.size());
        assertEquals(seeds.stream().sorted().toList(), distributed.stream().sorted().toList());
    }

    @Test
    void eachPoolContainsEveryRoundRobinPairExactlyOnce() {
        List<Pool> pools = generator.generate(new Seeding(fencerIds(11)), 6);

        for (Pool pool : pools) {
            int expectedBouts = pool.memberIds().size() * (pool.memberIds().size() - 1) / 2;
            assertEquals(expectedBouts, pool.bouts().size());
            assertTrue(pool.bouts().stream().allMatch(bout -> bout.score() == null));
            assertEquals(pool.bouts().size(), pool.bouts().stream()
                    .map(PoolBout::id)
                    .distinct()
                    .count());
        }
    }

    @Test
    void seedOrderIsPreservedAcrossAllPools() {
        List<UUID> seeds = fencerIds(13);
        List<Pool> pools = generator.generate(new Seeding(seeds), 6);

        List<UUID> distributed = pools.stream()
                .flatMap(pool -> pool.memberIds().stream())
                .toList();

        assertEquals(seeds.size(), distributed.size());
        assertEquals(seeds.stream().distinct().sorted().toList(), distributed.stream().distinct().sorted().toList());
    }

    @Test
    void twoFencersStillProduceOneBout() {
        List<Pool> pools = generator.generate(new Seeding(fencerIds(2)), 5);

        assertEquals(1, pools.size());
        assertEquals(1, pools.get(0).bouts().size());
    }

    @Test
    void selectedMaximumPoolSizeIsAlwaysAHardUpperBound() {
        assertCapacityCases(5, List.of(5, 6, 10, 11, 12, 15, 16));
        assertCapacityCases(6, List.of(6, 7, 12, 13, 16));
        assertCapacityCases(7, List.of(7, 8, 14, 15, 16));
        assertCapacityCases(8, List.of(8, 9, 16, 17));
    }

    @Test
    void invalidInputsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> generator.generate(null, 6));
        assertThrows(IllegalArgumentException.class,
                () -> generator.generate(new Seeding(fencerIds(2)), 4));
        assertThrows(IllegalArgumentException.class,
                () -> generator.generate(new Seeding(fencerIds(2)), 9));
        assertThrows(IllegalArgumentException.class,
                () -> generator.generate(new Seeding(List.of(UUID.randomUUID())), 6));

        UUID duplicate = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,
                () -> generator.generate(new Seeding(List.of(duplicate, duplicate)), 6));
    }

    private void assertPoolSizes(int fencerCount, int targetPoolSize, List<Integer> expectedSizes) {
        List<Pool> pools = generator.generate(new Seeding(fencerIds(fencerCount)), targetPoolSize);

        assertEquals(expectedSizes, pools.stream().map(pool -> pool.memberIds().size()).sorted().toList());
    }

    private void assertCapacityCases(int maximumPoolSize, List<Integer> fencerCounts) {
        for (int fencerCount : fencerCounts) {
            List<UUID> seeds = fencerIds(fencerCount);
            List<Pool> pools = generator.generate(new Seeding(seeds), maximumPoolSize);
            List<UUID> assigned = pools.stream().flatMap(pool -> pool.memberIds().stream()).toList();

            assertEquals((fencerCount + maximumPoolSize - 1) / maximumPoolSize, pools.size());
            assertTrue(pools.stream().allMatch(pool -> pool.memberIds().size() <= maximumPoolSize));
            assertTrue(pools.stream().mapToInt(pool -> pool.memberIds().size()).max().orElseThrow()
                    - pools.stream().mapToInt(pool -> pool.memberIds().size()).min().orElseThrow() <= 1);
            assertEquals(seeds.size(), assigned.size());
            assertEquals(seeds.stream().sorted().toList(), assigned.stream().sorted().toList());
        }
    }

    private static List<UUID> fencerIds(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> UUID.nameUUIDFromBytes(("fencer-" + (index + 1)).getBytes()))
                .toList();
    }

    private static List<Integer> seedNumbers(Pool pool, List<UUID> seedOrder) {
        return pool.memberIds().stream()
                .map(seedOrder::indexOf)
                .map(index -> index + 1)
                .toList();
    }
}
