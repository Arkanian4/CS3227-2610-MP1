package edu.nus.cs3227.fencingtournament.application;

import edu.nus.cs3227.fencingtournament.domain.Fencer;
import edu.nus.cs3227.fencingtournament.domain.Tournament;
import edu.nus.cs3227.fencingtournament.domain.TournamentPhase;
import edu.nus.cs3227.fencingtournament.domain.elimination.EliminationMatch;
import edu.nus.cs3227.fencingtournament.domain.pool.BoutScore;
import edu.nus.cs3227.fencingtournament.persistence.JsonTournamentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentAutosaveTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void successfulMutationsAutosaveWhileRejectedChangesDoNot() {
        CountingRepository repository = new CountingRepository();
        TournamentService service = new TournamentService(repository, temporaryDirectory);

        service.createTournament("Friday Open");
        service.addFencer("Alice");
        int savesAfterValidChanges = repository.saveCount;

        assertThrows(IllegalArgumentException.class, () -> service.addFencer("  "));
        assertThrows(IllegalArgumentException.class, () -> service.createTournament(" friday open "));

        assertEquals(2, savesAfterValidChanges);
        assertEquals(savesAfterValidChanges, repository.saveCount);
    }

    @Test
    void autosavedTournamentsReloadIndependently() throws IOException {
        Path autosaveDirectory = temporaryDirectory.resolve("tournaments");
        TournamentService service = new TournamentService(new JsonTournamentRepository(), autosaveDirectory);
        Tournament first = service.createTournament("Friday Open");
        service.addFencer("Alice");
        Tournament second = service.createTournament("Saturday Open");
        service.addFencer("Ben");

        TournamentService reloaded = new TournamentService(new JsonTournamentRepository(), autosaveDirectory);
        reloaded.loadAll(autosaveDirectory);

        reloaded.openTournament(first.id());
        assertEquals(List.of("Alice"), reloaded.registeredFencers().stream().map(Fencer::name).toList());
        reloaded.openTournament(second.id());
        assertEquals(List.of("Ben"), reloaded.registeredFencers().stream().map(Fencer::name).toList());
    }

    @Test
    void correctedPoolResultPersistsAndRejectedCorrectionDoesNotOverwriteIt() throws IOException {
        Path autosaveDirectory = temporaryDirectory.resolve("tournaments");
        TournamentService service = poolPhaseTournament(autosaveDirectory);
        var pool = service.pools().getFirst();
        var bout = pool.bouts().getFirst();

        service.recordPoolBoutResult(pool.id(), bout.id(), new BoutScore(5, 1));
        service.replacePoolBoutResult(pool.id(), bout.id(), new BoutScore(5, 3));
        assertThrows(IllegalArgumentException.class,
                () -> service.replacePoolBoutResult(pool.id(), bout.id(), new BoutScore(4, 3)));

        TournamentService reloaded = new TournamentService(new JsonTournamentRepository(), autosaveDirectory);
        reloaded.loadAll(autosaveDirectory);
        reloaded.openTournament(service.currentTournament().orElseThrow().id());
        var reloadedBout = reloaded.pools().getFirst().bouts().stream()
                .filter(candidate -> candidate.id().equals(bout.id())).findFirst().orElseThrow();

        assertEquals(new BoutScore(5, 3), reloadedBout.score());
    }

    @Test
    void editedDeScoreAndDependentResetSurviveReload() throws IOException {
        Path autosaveDirectory = temporaryDirectory.resolve("tournaments");
        TournamentService service = completedFourFencerTournament(autosaveDirectory);
        List<EliminationMatch> semiFinals = service.currentTournament().orElseThrow().eliminationBracket().matches().stream()
                .filter(match -> match.round() == 1).sorted(java.util.Comparator.comparingInt(EliminationMatch::position)).toList();
        EliminationMatch firstSemiFinal = semiFinals.getFirst();
        assertTrue(service.currentTournament().orElseThrow().completedAt().isPresent());

        service.replaceEliminationBoutResult(firstSemiFinal.id(), new BoutScore(6, 15), true);

        TournamentService reloaded = new TournamentService(new JsonTournamentRepository(), autosaveDirectory);
        reloaded.loadAll(autosaveDirectory);
        reloaded.openTournament(service.currentTournament().orElseThrow().id());
        EliminationMatch reloadedFinal = finalMatch(reloaded.currentTournament().orElseThrow());

        assertEquals(new BoutScore(6, 15), match(reloaded.currentTournament().orElseThrow(), firstSemiFinal.id()).score());
        assertFalse(reloadedFinal.isResolved());
        assertEquals(TournamentPhase.ELIMINATION_PHASE, reloaded.currentPhase());
        assertTrue(reloaded.currentTournament().orElseThrow().completedAt().isEmpty());
    }

    @Test
    void completedTournamentAndEditedFinalSurviveReload() throws IOException {
        Path autosaveDirectory = temporaryDirectory.resolve("tournaments");
        TournamentService service = completedFourFencerTournament(autosaveDirectory);
        EliminationMatch finalMatch = finalMatch(service.currentTournament().orElseThrow());
        var completedAt = service.currentTournament().orElseThrow().completedAt().orElseThrow();

        service.replaceEliminationBoutResult(finalMatch.id(), new BoutScore(7, 15), false);

        TournamentService reloaded = new TournamentService(new JsonTournamentRepository(), autosaveDirectory);
        reloaded.loadAll(autosaveDirectory);
        reloaded.openTournament(service.currentTournament().orElseThrow().id());

        assertEquals(TournamentPhase.COMPLETE, reloaded.currentPhase());
        assertEquals(finalMatch.secondSlot().fencerId(), finalMatch(reloaded.currentTournament().orElseThrow()).winnerId());
        assertEquals(finalMatch.secondSlot().fencerId(), reloaded.finalStandings().getFirst().fencerId());
        assertEquals(completedAt, reloaded.currentTournament().orElseThrow().completedAt().orElseThrow());
    }

    @Test
    void autosaveFailureIsReportedAfterTheInMemoryChange() {
        TournamentService service = new TournamentService(new FailingRepository(), temporaryDirectory);

        assertThrows(TournamentPersistenceException.class, () -> service.createTournament("Friday Open"));

        assertTrue(service.currentTournament().isPresent());
    }

    @Test
    void deletingOneTournamentAutosavesWithoutAffectingAnotherTournament() throws IOException {
        Path autosaveDirectory = temporaryDirectory.resolve("tournaments");
        TournamentService service = new TournamentService(new JsonTournamentRepository(), autosaveDirectory);
        Tournament first = service.createTournament("Friday Open");
        service.addFencer("Alice");
        Tournament second = service.createTournament("Saturday Open");
        service.addFencer("Ben");

        service.openTournament(first.id());
        assertTrue(service.deleteTournament(first.id()));
        assertTrue(service.currentTournament().isEmpty());
        assertFalse(Files.exists(autosaveDirectory.resolve("Friday_Open.json")));
        assertEquals(List.of(second.id()), service.listTournaments().stream().map(Tournament::id).toList());

        TournamentService reloaded = new TournamentService(new JsonTournamentRepository(), autosaveDirectory);
        reloaded.loadAll(autosaveDirectory);
        assertEquals(List.of("Saturday Open"), reloaded.listTournaments().stream().map(Tournament::name).toList());
        reloaded.openTournament(second.id());
        assertEquals(List.of("Ben"), reloaded.registeredFencers().stream().map(Fencer::name).toList());
    }

    @Test
    void completedTournamentCanBeDeletedAndDoesNotReappearAfterReload() throws IOException {
        Path autosaveDirectory = temporaryDirectory.resolve("tournaments");
        TournamentService service = completedFourFencerTournament(autosaveDirectory);
        Tournament completed = service.currentTournament().orElseThrow();
        assertEquals(TournamentPhase.COMPLETE, completed.phase());

        assertTrue(service.deleteTournament(completed.id()));

        TournamentService reloaded = new TournamentService(new JsonTournamentRepository(), autosaveDirectory);
        reloaded.loadAll(autosaveDirectory);
        assertTrue(reloaded.listTournaments().isEmpty());
    }

    @Test
    void deletingMissingTournamentDoesNotMutateOrPersist() {
        CountingRepository repository = new CountingRepository();
        TournamentService service = new TournamentService(repository, temporaryDirectory);
        Tournament existing = service.createTournament("Friday Open");
        int savesBeforeDeletionAttempt = repository.saveCount;

        assertFalse(service.deleteTournament(UUID.randomUUID()));

        assertEquals(savesBeforeDeletionAttempt, repository.saveCount);
        assertEquals(0, repository.deleteCount);
        assertEquals(existing.id(), service.currentTournament().orElseThrow().id());
    }

    private TournamentService completedFourFencerTournament(Path autosaveDirectory) {
        TournamentService service = poolPhaseTournament(autosaveDirectory);
        var pool = service.pools().getFirst();
        pool.bouts().forEach(bout -> service.recordPoolBoutResult(pool.id(), bout.id(), new BoutScore(5, 0)));
        service.generateEliminationBracket();
        while (service.currentPhase() != TournamentPhase.COMPLETE) {
            EliminationMatch ready = service.currentTournament().orElseThrow().eliminationBracket().matches().stream()
                    .filter(EliminationMatch::isReady).findFirst().orElseThrow();
            service.recordEliminationBoutResult(ready.id(), new BoutScore(15, 8));
        }
        return service;
    }

    private TournamentService poolPhaseTournament(Path autosaveDirectory) {
        TournamentService service = new TournamentService(new JsonTournamentRepository(), autosaveDirectory);
        service.createTournament("Internal Open");
        List<Fencer> fencers = java.util.stream.IntStream.range(0, 4)
                .mapToObj(index -> service.addFencer("Fencer " + index)).toList();
        service.seedFencers(fencers.stream().map(Fencer::id).toList());
        service.generatePools();
        return service;
    }

    private static EliminationMatch finalMatch(Tournament tournament) {
        return tournament.eliminationBracket().matches().stream().filter(match -> match.nextMatchId() == null).findFirst().orElseThrow();
    }

    private static EliminationMatch match(Tournament tournament, UUID matchId) {
        return tournament.eliminationBracket().matches().stream().filter(match -> match.id().equals(matchId)).findFirst().orElseThrow();
    }

    private static final class CountingRepository implements TournamentRepository {
        private int saveCount;
        private int deleteCount;

        @Override
        public Optional<Tournament> load(Path path) { return Optional.empty(); }

        @Override
        public void save(Tournament tournament, Path path) { saveCount++; }

        @Override
        public void delete(Path path) { deleteCount++; }
    }

    private static final class FailingRepository implements TournamentRepository {
        @Override
        public Optional<Tournament> load(Path path) { return Optional.empty(); }

        @Override
        public void save(Tournament tournament, Path path) throws IOException { throw new IOException("Disk unavailable"); }

        @Override
        public void delete(Path path) throws IOException { throw new IOException("Disk unavailable"); }
    }
}
