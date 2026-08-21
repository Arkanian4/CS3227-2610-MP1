package edu.nus.cs3227.fencingtournament.domain.pool;

import java.util.List;
import java.util.UUID;

/** A fixed round-robin group and its scheduled bouts. */
public record Pool(UUID id, String name, List<UUID> memberIds, List<PoolBout> bouts) {
    public Pool {
        memberIds = List.copyOf(memberIds);
        bouts = List.copyOf(bouts);
    }

    /** Records a previously incomplete bout, validating the configured winning limit. */
    public Pool recordBoutResult(UUID boutId, BoutScore score, int scoreLimit) {
        return updateBoutResult(boutId, score, scoreLimit, false);
    }

    /** Explicitly replaces an existing result, for correcting an organiser entry. */
    public Pool replaceBoutResult(UUID boutId, BoutScore score, int scoreLimit) {
        return updateBoutResult(boutId, score, scoreLimit, true);
    }

    private Pool updateBoutResult(UUID boutId, BoutScore score, int scoreLimit, boolean allowOverwrite) {
        if (boutId == null || score == null) {
            throw new IllegalArgumentException("Bout ID and score must not be null.");
        }
        if (scoreLimit <= 0) {
            throw new IllegalArgumentException("Bout score limit must be positive.");
        }

        boolean found = false;
        List<PoolBout> updatedBouts = new java.util.ArrayList<>(bouts.size());
        for (PoolBout bout : bouts) {
            if (!bout.id().equals(boutId)) {
                updatedBouts.add(bout);
                continue;
            }
            found = true;
            if (bout.isComplete() && !allowOverwrite) {
                throw new IllegalStateException("Pool bout is already complete; use replacement explicitly.");
            }
            validateScore(score, scoreLimit);
            updatedBouts.add(bout.withScore(score));
        }
        if (!found) {
            throw new IllegalArgumentException("Bout does not belong to this pool.");
        }
        return new Pool(id, name, memberIds, updatedBouts);
    }

    public boolean isComplete() {
        return bouts.stream().allMatch(PoolBout::isComplete);
    }

    private static void validateScore(BoutScore score, int scoreLimit) {
        if (score.firstScore() > scoreLimit || score.secondScore() > scoreLimit) {
            throw new IllegalArgumentException("Pool scores must not exceed " + scoreLimit + ".");
        }
        if (Math.max(score.firstScore(), score.secondScore()) != scoreLimit) {
            throw new IllegalArgumentException("The winning score must be " + scoreLimit + ".");
        }
    }
}

