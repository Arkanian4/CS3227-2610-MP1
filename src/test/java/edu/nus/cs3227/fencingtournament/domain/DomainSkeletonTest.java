package edu.nus.cs3227.fencingtournament.domain;

import edu.nus.cs3227.fencingtournament.domain.standings.TieBreakPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainSkeletonTest {
    @Test
    void fencerRetainsItsIdentityAndDisplayName() {
        UUID id = UUID.randomUUID();
        Fencer fencer = new Fencer(id, "  Alex Tan  ");

        assertEquals(id, fencer.id());
        assertEquals("Alex Tan", fencer.name());
    }

    @Test
    void fencerRejectsNullOrBlankName() {
        UUID id = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> new Fencer(id, null));
        assertThrows(IllegalArgumentException.class, () -> new Fencer(id, "   "));
    }

    @Test
    void duplicateFencerNamesAreRejectedWhenIdsDiffer() {
        Fencer first = Fencer.create("Alex Tan");
        Fencer second = Fencer.create("Alex Tan");
        Tournament tournament = Tournament.create("Internal Open", testSettings());

        assertDoesNotThrow(() -> tournament.addFencer(first));
        assertThrows(IllegalArgumentException.class, () -> tournament.addFencer(second));
        assertEquals(1, tournament.fencers().size());
    }

    @Test
    void tournamentCreationRejectsInvalidIdentityNameAndSettings() {
        TournamentSettings settings = testSettings();

        assertThrows(IllegalArgumentException.class,
                () -> new Tournament(null, "Tournament", settings));
        assertThrows(IllegalArgumentException.class,
                () -> new Tournament(UUID.randomUUID(), "   ", settings));
        assertThrows(NullPointerException.class,
                () -> new Tournament(UUID.randomUUID(), "Tournament", null));
    }

    @Test
    void tournamentStartsWithAnEmptyRoster() {
        Tournament tournament = Tournament.create("  Internal Open  ", testSettings());

        assertEquals("Internal Open", tournament.name());
        assertTrue(tournament.fencers().isEmpty());
    }

    @Test
    void tournamentRejectsDuplicateFencerIdsDuringCreation() {
        UUID id = UUID.randomUUID();
        Fencer first = new Fencer(id, "Alex Tan");
        Fencer duplicate = new Fencer(id, "Alex T.");

        assertThrows(IllegalArgumentException.class,
                () -> new Tournament(UUID.randomUUID(), "Tournament", testSettings(),
                        List.of(first, duplicate), null, List.of(), null));
    }

    @Test
    void tournamentRejectsDuplicateFencerIdsWhenAdding() {
        UUID id = UUID.randomUUID();
        Tournament tournament = Tournament.create("Tournament", testSettings());
        tournament.addFencer(new Fencer(id, "Alex Tan"));

        assertThrows(IllegalArgumentException.class,
                () -> tournament.addFencer(new Fencer(id, "Alex T.")));
        assertEquals(1, tournament.fencers().size());
    }

    @Test
    void tournamentCanFindListAndRemoveFencersById() {
        Fencer first = Fencer.create("Alex Tan");
        Fencer second = Fencer.create("Jamie Lim");
        Tournament tournament = Tournament.create("Tournament", testSettings());
        tournament.addFencer(first);
        tournament.addFencer(second);

        assertEquals(first, tournament.findFencer(first.id()).orElseThrow());
        assertTrue(tournament.removeFencer(first.id()));
        assertTrue(tournament.findFencer(first.id()).isEmpty());
        assertEquals(List.of(second), tournament.fencers());
        assertFalse(tournament.removeFencer(first.id()));
    }

    @Test
    void rosterViewCannotBeMutatedExternally() {
        Tournament tournament = Tournament.create("Tournament", testSettings());
        tournament.addFencer(Fencer.create("Alex Tan"));

        assertThrows(UnsupportedOperationException.class,
                () -> tournament.fencers().clear());
    }

    @Test
    void rosterCannotBeChangedAfterRegistrationStateHasEnded() {
        Fencer fencer = Fencer.create("Alex Tan");
        Tournament tournament = new Tournament(UUID.randomUUID(), "Tournament", testSettings(),
                List.of(fencer), new Seeding(List.of(fencer.id())), List.of(), null);

        assertThrows(IllegalStateException.class, () -> tournament.addFencer(Fencer.create("Jamie Lim")));
        assertThrows(IllegalStateException.class, () -> tournament.removeFencer(fencer.id()));
    }

    private static TournamentSettings testSettings() {
        return new TournamentSettings(5, 5, 15, 8, new TieBreakPolicy(List.of()));
    }
}
