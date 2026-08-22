package edu.nus.cs3227.fencingtournament.domain.rules;

import edu.nus.cs3227.fencingtournament.domain.Seeding;
import edu.nus.cs3227.fencingtournament.domain.pool.Pool;
import edu.nus.cs3227.fencingtournament.domain.pool.PoolBout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Deterministically distributes seeds and creates one round-robin schedule per pool. */
public final class PoolGenerator {
    /** Generates balanced pools from a complete ordered seeding without exceeding the selected cap. */
    public List<Pool> generate(Seeding seeding, int targetPoolSize) {
        if (seeding == null) {
            throw new IllegalArgumentException("Seeding must not be null.");
        }
        if (targetPoolSize < 5 || targetPoolSize > 8) {
            throw new IllegalArgumentException("Target pool size must be between 5 and 8.");
        }

        List<UUID> seededFencerIds = seeding.fencerIds();
        validateSeededFencers(seededFencerIds);
        int numberOfPools = determinePoolCount(seededFencerIds.size(), targetPoolSize);
        List<List<UUID>> membersByPool = distributeSeeds(seededFencerIds, numberOfPools);

        List<Pool> pools = new ArrayList<>(numberOfPools);
        for (int poolIndex = 0; poolIndex < membersByPool.size(); poolIndex++) {
            List<UUID> members = membersByPool.get(poolIndex);
            List<PoolBout> bouts = new ArrayList<>();
            for (int firstIndex = 0; firstIndex < members.size(); firstIndex++) {
                for (int secondIndex = firstIndex + 1; secondIndex < members.size(); secondIndex++) {
                    bouts.add(new PoolBout(
                            UUID.randomUUID(),
                            members.get(firstIndex),
                            members.get(secondIndex),
                            null));
                }
            }
            pools.add(new Pool(UUID.randomUUID(), poolName(poolIndex), members, bouts));
        }
        return List.copyOf(pools);
    }

    private static int determinePoolCount(int fencerCount, int targetPoolSize) {
        if (fencerCount < 2) {
            throw new IllegalArgumentException("At least two fencers are required to generate pools.");
        }
        return (fencerCount + targetPoolSize - 1) / targetPoolSize;
    }

    private static List<List<UUID>> distributeSeeds(List<UUID> seededFencerIds, int numberOfPools) {
        List<List<UUID>> membersByPool = new ArrayList<>(numberOfPools);
        for (int poolIndex = 0; poolIndex < numberOfPools; poolIndex++) {
            membersByPool.add(new ArrayList<>());
        }

        for (int seedIndex = 0; seedIndex < seededFencerIds.size(); seedIndex++) {
            int row = seedIndex / numberOfPools;
            int column = seedIndex % numberOfPools;
            int poolIndex = row % 2 == 0 ? column : numberOfPools - column - 1;
            membersByPool.get(poolIndex).add(seededFencerIds.get(seedIndex));
        }
        return membersByPool;
    }

    private static void validateSeededFencers(List<UUID> seededFencerIds) {
        Set<UUID> uniqueIds = new HashSet<>();
        for (UUID fencerId : seededFencerIds) {
            if (fencerId == null || !uniqueIds.add(fencerId)) {
                throw new IllegalArgumentException("Seeding must contain unique non-null fencer IDs.");
            }
        }
    }

    private static String poolName(int poolIndex) {
        return "POOL #" + (poolIndex + 1);
    }
}
