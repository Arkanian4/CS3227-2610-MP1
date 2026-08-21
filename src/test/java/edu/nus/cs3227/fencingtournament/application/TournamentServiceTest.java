package edu.nus.cs3227.fencingtournament.application;

import edu.nus.cs3227.fencingtournament.domain.Fencer;
import edu.nus.cs3227.fencingtournament.domain.Tournament;
import edu.nus.cs3227.fencingtournament.domain.TournamentPhase;
import edu.nus.cs3227.fencingtournament.domain.pool.BoutScore;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentServiceTest {
    @Test
    void serviceCreatesAndManagesActiveTournament() {
        TournamentService service = new TournamentService(new InMemoryRepository());

        service.createTournament("Internal Open");
        Fencer added = service.addFencer("Alex Tan");

        assertEquals(List.of(added), service.registeredFencers());
        assertTrue(service.removeFencer(added.id()));
        assertTrue(service.registeredFencers().isEmpty());
    }

    @Test
    void multipleTournamentsCanBeSelectedWithoutSharingRosterState() {
        TournamentService service = new TournamentService(new InMemoryRepository());

        Tournament first = service.createTournament("Club Open");
        service.addFencer("Alice");
        Tournament second = service.createTournament("Training Night");
        service.addFencer("Bob");

        service.openTournament(first.id());
        assertEquals(List.of("Alice"), service.registeredFencers().stream().map(Fencer::name).toList());
        service.openTournament(second.id());
        assertEquals(List.of("Bob"), service.registeredFencers().stream().map(Fencer::name).toList());
        assertEquals(2, service.listTournaments().size());
    }

    @Test
    void tournamentNamesAreUniqueIgnoringCaseAndWhitespace() {
        TournamentService service = new TournamentService(new InMemoryRepository());
        Tournament original = service.createTournament("  Club Open  ");

        assertThrows(IllegalArgumentException.class, () -> service.createTournament("club open"));
        assertEquals(original.id(), service.currentTournament().orElseThrow().id());
    }

    @Test
    void serviceRequiresActiveTournamentForRegistration() {
        TournamentService service = new TournamentService(new InMemoryRepository());

        assertThrows(IllegalStateException.class, () -> service.addFencer("Alex Tan"));
    }

    @Test
    void missingLoadDoesNotReplaceActiveTournament() throws IOException {
        InMemoryRepository repository = new InMemoryRepository();
        TournamentService service = new TournamentService(repository);
        Tournament created = service.createTournament("Internal Open");

        assertTrue(service.loadTournament(Path.of("missing.json")).isEmpty());
        assertEquals(created, service.currentTournament().orElseThrow());
    }

    @Test
    void serviceCoordinatesSeedingPoolGenerationAndReadModels() {
        TournamentService service = new TournamentService(new InMemoryRepository());
        service.createTournament("Internal Open");
        List<Fencer> fencers = java.util.stream.IntStream.range(0, 5)
                .mapToObj(index -> service.addFencer("Fencer " + (index + 1)))
                .toList();

        service.seedFencers(fencers.stream().map(Fencer::id).toList());
        assertEquals(TournamentPhase.SEEDING, service.currentPhase());

        service.generatePools();

        assertEquals(TournamentPhase.POOL_PHASE, service.currentPhase());
        assertEquals(1, service.pools().size());
        assertEquals(0, service.poolProgress().completedBouts());
        assertEquals(10, service.poolProgress().totalBouts());
        assertEquals(5, service.standingsForPool(service.pools().get(0).id()).size());
    }

    @Test
    void seededFencersCanBeMovedByInsertionBeforePoolGeneration() {
        TournamentService service = new TournamentService(new InMemoryRepository());
        service.createTournament("Internal Open");
        List<Fencer> fencers = java.util.stream.IntStream.range(0, 4)
                .mapToObj(index -> service.addFencer("Fencer " + (index + 1))).toList();
        service.seedFencers(fencers.stream().map(Fencer::id).toList());

        assertTrue(service.moveSeedFencer(fencers.getFirst().id(), 3));
        assertEquals(List.of(fencers.get(1).id(), fencers.get(2).id(), fencers.get(3).id(), fencers.getFirst().id()),
                service.currentTournament().orElseThrow().seeding().fencerIds());
        assertTrue(service.moveSeedFencer(fencers.getFirst().id(), 0));
        assertTrue(service.moveSeedFencer(fencers.get(3).id(), 1));
        assertEquals(List.of(fencers.getFirst().id(), fencers.get(3).id(), fencers.get(1).id(), fencers.get(2).id()),
                service.currentTournament().orElseThrow().seeding().fencerIds());
        assertFalse(service.moveSeedFencer(UUID.randomUUID(), 0));
        assertFalse(service.moveSeedFencer(fencers.getFirst().id(), -1));
        assertFalse(service.moveSeedFencer(fencers.getFirst().id(), 4));
        assertEquals(4, new java.util.HashSet<>(service.currentTournament().orElseThrow().seeding().fencerIds()).size());

        service.generatePools(5);
        assertEquals(TournamentPhase.POOL_PHASE, service.currentPhase());
        assertFalse(service.moveSeedFencer(fencers.getFirst().id(), 1));
    }

    @Test
    void serviceGeneratesEliminationAfterEveryPoolBoutIsComplete() {
        TournamentService service = new TournamentService(new InMemoryRepository());
        service.createTournament("Internal Open");
        List<Fencer> fencers = java.util.stream.IntStream.range(0, 4)
                .mapToObj(index -> service.addFencer("Fencer " + (index + 1))).toList();
        service.seedFencers(fencers.stream().map(Fencer::id).toList());
        service.generatePools();
        var pool = service.pools().getFirst();
        assertThrows(IllegalStateException.class, service::generateEliminationBracket);
        for (var bout : pool.bouts()) service.recordPoolBoutResult(pool.id(), bout.id(), new BoutScore(5, 0));

        var bracket = service.generateEliminationBracket();

        assertEquals(4, bracket.size());
        assertEquals(TournamentPhase.ELIMINATION_PHASE, service.currentPhase());
    }

    @Test
    void completingTheFinalProducesFinalStandingsAndRetainsTournamentData() {
        TournamentService service = new TournamentService(new InMemoryRepository());
        service.createTournament("Internal Open");
        List<Fencer> fencers = java.util.stream.IntStream.range(0, 4)
                .mapToObj(index -> service.addFencer("Fencer " + (index + 1))).toList();
        service.seedFencers(fencers.stream().map(Fencer::id).toList());
        service.generatePools();
        var pool = service.pools().getFirst();
        pool.bouts().forEach(bout -> service.recordPoolBoutResult(pool.id(), bout.id(), new BoutScore(5, 0)));
        service.generateEliminationBracket();

        while (service.currentPhase() != TournamentPhase.COMPLETE) {
            var ready = service.currentTournament().orElseThrow().eliminationBracket().matches().stream()
                    .filter(match -> match.isReady()).findFirst().orElseThrow();
            service.recordEliminationBoutResult(ready.id(), new BoutScore(15, 0));
        }

        var tournament = service.currentTournament().orElseThrow();
        var finalMatch = tournament.eliminationBracket().matches().stream()
                .filter(match -> match.nextMatchId() == null).findFirst().orElseThrow();
        var finalResults = service.finalStandings();
        UUID runnerUp = finalMatch.score().firstFencerWon() ? finalMatch.secondSlot().fencerId() : finalMatch.firstSlot().fencerId();

        assertEquals(TournamentPhase.COMPLETE, service.currentPhase());
        assertEquals(finalMatch.winnerId(), finalResults.getFirst().fencerId());
        assertEquals(runnerUp, finalResults.get(1).fencerId());
        assertEquals("Champion", finalResults.getFirst().directEliminationFinish());
        assertEquals("Runner-up", finalResults.get(1).directEliminationFinish());
        assertNotNull(tournament.eliminationBracket());
        assertEquals(1, tournament.pools().size());
        assertThrows(IllegalStateException.class, () -> service.recordEliminationBoutResult(finalMatch.id(), new BoutScore(15, 0)));
    }

    private static final class InMemoryRepository implements TournamentRepository {
        private Tournament stored;

        @Override
        public Optional<Tournament> load(Path path) {
            return Optional.ofNullable(stored);
        }

        @Override
        public void save(Tournament tournament, Path path) {
            stored = tournament;
        }

        @Override
        public void delete(Path path) {
            stored = null;
        }
    }
}
