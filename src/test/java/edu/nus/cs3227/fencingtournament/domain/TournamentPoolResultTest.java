package edu.nus.cs3227.fencingtournament.domain;

import edu.nus.cs3227.fencingtournament.domain.pool.BoutScore;
import edu.nus.cs3227.fencingtournament.domain.pool.Pool;
import edu.nus.cs3227.fencingtournament.domain.rules.PoolGenerator;
import edu.nus.cs3227.fencingtournament.domain.standings.TieBreakPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TournamentPoolResultTest {
    @Test
    void recordingAResultUpdatesOnlyTheScheduledPoolBout() {
        Fencer first = Fencer.create("Alex Tan");
        Fencer second = Fencer.create("Jamie Lim");
        TournamentSettings settings = new TournamentSettings(5, 5, 15, 2,
                new TieBreakPolicy(List.of()));
        Pool pool = new PoolGenerator().generate(new Seeding(List.of(first.id(), second.id())), 5).get(0);
        Tournament tournament = new Tournament(UUID.randomUUID(), "Open", settings,
                List.of(first, second), new Seeding(List.of(first.id(), second.id())), List.of(pool), null);

        tournament.recordPoolBoutResult(pool.id(), pool.bouts().get(0).id(), new BoutScore(5, 1));

        assertEquals(new BoutScore(5, 1), tournament.pools().get(0).bouts().get(0).score());
    }

    @Test
    void resultCannotBeRecordedWithoutGeneratedPoolsOrForAnUnknownPool() {
        TournamentSettings settings = new TournamentSettings(5, 5, 15, 2,
                new TieBreakPolicy(List.of()));
        Tournament empty = Tournament.create("Open", settings);

        assertThrows(IllegalStateException.class,
                () -> empty.recordPoolBoutResult(UUID.randomUUID(), UUID.randomUUID(), new BoutScore(5, 1)));
    }

    @Test
    void tournamentRejectsPersistedOrConstructedPoolScoreAboveConfiguredLimit() {
        Fencer first = Fencer.create("Alex Tan");
        Fencer second = Fencer.create("Jamie Lim");
        TournamentSettings settings = new TournamentSettings(5, 5, 15, 2,
                new edu.nus.cs3227.fencingtournament.domain.standings.TieBreakPolicy(List.of()));
        Pool generated = new PoolGenerator().generate(
                new Seeding(List.of(first.id(), second.id())), 5).get(0);
        Pool invalid = generated.recordBoutResult(generated.bouts().get(0).id(), new BoutScore(6, 2), 6);

        assertThrows(IllegalArgumentException.class,
                () -> new Tournament(UUID.randomUUID(), "Open", settings,
                        List.of(first, second), new Seeding(List.of(first.id(), second.id())),
                        List.of(invalid), null));
    }
}
