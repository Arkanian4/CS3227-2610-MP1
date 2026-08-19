package edu.nus.cs3227.fencingtournament.domain.pool;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoolBoutResultTest {
    @Test
    void incompleteBoutIsDistinguishableAndCanBeCompleted() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        PoolBout bout = new PoolBout(UUID.randomUUID(), first, second, null);
        Pool pool = new Pool(UUID.randomUUID(), "Pool A", List.of(first, second), List.of(bout));

        assertFalse(bout.isComplete());
        Pool completed = pool.recordBoutResult(bout.id(), new BoutScore(5, 3), 5);

        assertTrue(completed.bouts().get(0).isComplete());
        assertEquals(new BoutScore(5, 3), completed.bouts().get(0).score());
        assertTrue(completed.isComplete());
    }

    @Test
    void resultMustUseConfiguredWinningLimit() {
        Pool pool = createPoolWithBout();
        UUID boutId = pool.bouts().get(0).id();

        assertThrows(IllegalArgumentException.class,
                () -> pool.recordBoutResult(boutId, new BoutScore(4, 3), 5));
        assertThrows(IllegalArgumentException.class,
                () -> pool.recordBoutResult(boutId, new BoutScore(6, 2), 5));
    }

    @Test
    void negativeAndTiedScoresAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BoutScore(-1, 5));
        assertThrows(IllegalArgumentException.class, () -> new BoutScore(5, 5));
    }

    @Test
    void completedBoutCannotBeOverwrittenWithoutExplicitReplacement() {
        Pool pool = createPoolWithBout();
        UUID boutId = pool.bouts().get(0).id();
        Pool completed = pool.recordBoutResult(boutId, new BoutScore(5, 2), 5);

        assertThrows(IllegalStateException.class,
                () -> completed.recordBoutResult(boutId, new BoutScore(5, 4), 5));
        Pool corrected = completed.replaceBoutResult(boutId, new BoutScore(5, 4), 5);
        assertEquals(new BoutScore(5, 4), corrected.bouts().get(0).score());
    }

    @Test
    void unknownBoutCannotReceiveAResult() {
        Pool pool = createPoolWithBout();

        assertThrows(IllegalArgumentException.class,
                () -> pool.recordBoutResult(UUID.randomUUID(), new BoutScore(5, 3), 5));
    }

    private static Pool createPoolWithBout() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        PoolBout bout = new PoolBout(UUID.randomUUID(), first, second, null);
        return new Pool(UUID.randomUUID(), "Pool A", List.of(first, second), List.of(bout));
    }
}
