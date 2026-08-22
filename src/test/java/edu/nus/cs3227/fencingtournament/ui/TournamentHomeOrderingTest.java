package edu.nus.cs3227.fencingtournament.ui;

import edu.nus.cs3227.fencingtournament.domain.Tournament;
import edu.nus.cs3227.fencingtournament.domain.TournamentSettings;
import edu.nus.cs3227.fencingtournament.domain.Fencer;
import edu.nus.cs3227.fencingtournament.domain.Seeding;
import edu.nus.cs3227.fencingtournament.domain.pool.BoutScore;
import edu.nus.cs3227.fencingtournament.domain.rules.BracketGenerator;
import edu.nus.cs3227.fencingtournament.domain.standings.TieBreakPolicy;
import edu.nus.cs3227.fencingtournament.domain.standings.TieBreakCriterion;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TournamentHomeOrderingTest {
    @Test
    void sortsMostRecentlyModifiedFirstAndUsesNameForEqualTimestamps() {
        Instant shared = Instant.parse("2026-08-20T10:00:00Z");
        Tournament alpha = tournament("Alpha", shared);
        Tournament bravo = tournament("Bravo", shared);
        Tournament newest = tournament("Newest", shared.plusSeconds(60));

        assertEquals(List.of(newest, alpha, bravo),
                TournamentView.orderByMostRecentlyModified(List.of(bravo, newest, alpha)));
    }

    @Test
    void formatsModificationTimesForHomeMetadata() {
        Instant now = Instant.parse("2026-08-22T15:00:00Z");
        assertEquals("Updated just now", TournamentView.formatLastModified(now.minusSeconds(30), now, ZoneOffset.UTC));
        assertEquals("Updated 12 min ago", TournamentView.formatLastModified(now.minusSeconds(12 * 60), now, ZoneOffset.UTC));
        assertEquals("Updated 2h ago", TournamentView.formatLastModified(now.minusSeconds(2 * 60 * 60), now, ZoneOffset.UTC));
        assertEquals("Updated yesterday", TournamentView.formatLastModified(now.minusSeconds(26 * 60 * 60), now, ZoneOffset.UTC));
        assertEquals("Updated 18 Aug", TournamentView.formatLastModified(Instant.parse("2026-08-18T10:00:00Z"), now, ZoneOffset.UTC));
        assertEquals("Updated 18 Aug 2025", TournamentView.formatLastModified(Instant.parse("2025-08-18T10:00:00Z"), now, ZoneOffset.UTC));
        assertEquals("Completed 21 Aug", TournamentView.formatCompletedAt(Instant.parse("2026-08-21T10:00:00Z"), now, ZoneOffset.UTC));
        assertEquals("Completed 21 Aug 2025", TournamentView.formatCompletedAt(Instant.parse("2025-08-21T10:00:00Z"), now, ZoneOffset.UTC));
    }

    @Test
    void sortsCompletedTournamentsByCompletionTimeThenName() {
        Instant shared = Instant.parse("2026-08-20T10:00:00Z");
        Tournament alpha = completedTournament("Alpha", shared, shared);
        Tournament bravo = completedTournament("Bravo", shared, shared);
        Tournament newest = completedTournament("Newest", shared.minusSeconds(60), shared.plusSeconds(60));

        assertEquals(List.of(newest, alpha, bravo),
                TournamentView.orderByCompletionTime(List.of(bravo, newest, alpha)));
    }

    private static Tournament tournament(String name, Instant lastModified) {
        return new Tournament(UUID.randomUUID(), name,
                new TournamentSettings(5, 5, 15, 8,
                        new TieBreakPolicy(List.of(TieBreakCriterion.VICTORY_RATIO))),
                List.of(), null, List.of(), null, lastModified);
    }

    private static Tournament completedTournament(String name, Instant lastModified, Instant completedAt) {
        Fencer first = Fencer.create(name + " A");
        Fencer second = Fencer.create(name + " B");
        var bracket = new BracketGenerator().generate(List.of(first.id(), second.id()));
        var finalMatch = bracket.matches().getFirst();
        bracket = bracket.recordResult(finalMatch.id(), new BoutScore(15, 10), 15);
        return new Tournament(UUID.randomUUID(), name,
                new TournamentSettings(5, 5, 15, 8,
                        new TieBreakPolicy(List.of(TieBreakCriterion.VICTORY_RATIO))),
                List.of(first, second), new Seeding(List.of(first.id(), second.id())), List.of(), bracket,
                lastModified, completedAt);
    }
}
