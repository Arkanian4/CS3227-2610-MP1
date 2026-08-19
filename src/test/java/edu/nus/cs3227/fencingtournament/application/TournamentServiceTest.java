package edu.nus.cs3227.fencingtournament.application;

import edu.nus.cs3227.fencingtournament.domain.Fencer;
import edu.nus.cs3227.fencingtournament.domain.Tournament;
import edu.nus.cs3227.fencingtournament.domain.TournamentPhase;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void serviceRequiresActiveTournamentForRegistrationAndSaving() {
        TournamentService service = new TournamentService(new InMemoryRepository());

        assertThrows(IllegalStateException.class, () -> service.addFencer("Alex Tan"));
        assertThrows(IllegalStateException.class, () -> service.saveTournament(Path.of("unused.json")));
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
    }
}
