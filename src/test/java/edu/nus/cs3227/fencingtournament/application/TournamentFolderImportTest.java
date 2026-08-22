package edu.nus.cs3227.fencingtournament.application;

import edu.nus.cs3227.fencingtournament.domain.Fencer;
import edu.nus.cs3227.fencingtournament.domain.Seeding;
import edu.nus.cs3227.fencingtournament.domain.Tournament;
import edu.nus.cs3227.fencingtournament.domain.TournamentSettings;
import edu.nus.cs3227.fencingtournament.domain.standings.TieBreakPolicy;
import edu.nus.cs3227.fencingtournament.persistence.JsonTournamentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentFolderImportTest {
    @TempDir Path temporaryDirectory;

    @Test
    void importsValidFilesIndependentlyAndPreservesTheirTimestamps() throws IOException {
        Path source = Files.createDirectory(temporaryDirectory.resolve("source"));
        Path autosave = Files.createDirectory(temporaryDirectory.resolve("autosave"));
        Tournament first = tournament("First", Instant.parse("2026-08-01T10:15:30Z"));
        Tournament second = tournament("Second", Instant.parse("2026-08-02T10:15:30Z"));
        JsonTournamentRepository repository = new JsonTournamentRepository();
        repository.save(first, source.resolve("first.json"));
        repository.save(second, source.resolve("second.json"));
        Files.writeString(source.resolve("broken.json"), "{ broken");
        Files.writeString(source.resolve("empty.json"), "");
        Files.writeString(source.resolve("ignore.txt"), "not json");

        TournamentService service = new TournamentService(repository, autosave);
        TournamentFolderImportResult result = service.importTournamentsFromFolder(source);

        assertEquals(2, result.imported().size());
        assertEquals(2, result.rejected().size());
        assertEquals(List.of("Second", "First"), service.listTournaments().stream().map(Tournament::name).toList());
        assertEquals(first.lastModified(), service.openTournament(first.id()).lastModified());
        assertTrue(Files.isRegularFile(autosave.resolve("First.json")));
        assertTrue(Files.isRegularFile(autosave.resolve("Second.json")));
    }

    @Test
    void skipsDuplicateNamesWithoutChangingExistingTournament() throws IOException {
        Path source = Files.createDirectory(temporaryDirectory.resolve("source"));
        JsonTournamentRepository repository = new JsonTournamentRepository();
        Tournament imported = tournament("Club Open", Instant.parse("2026-08-01T10:15:30Z"));
        repository.save(imported, source.resolve("club-open.json"));

        TournamentService service = new TournamentService(repository);
        Tournament existing = service.createTournament(" club open ");
        TournamentFolderImportResult result = service.importTournamentsFromFolder(source);

        assertEquals(0, result.imported().size());
        assertEquals(1, result.skipped().size());
        assertEquals(existing.id(), service.listTournaments().getFirst().id());
    }

    @Test
    void singleFileImportUsesTheSameResultCategoriesAndActivatesTheImportedTournament() throws IOException {
        Path source = temporaryDirectory.resolve("single.json");
        JsonTournamentRepository repository = new JsonTournamentRepository();
        Tournament original = tournament("Single", Instant.parse("2026-08-04T10:15:30Z"));
        repository.save(original, source);

        TournamentService service = new TournamentService(repository);
        TournamentFolderImportResult result = service.importTournamentFile(source);

        assertEquals(1, result.imported().size());
        assertEquals(original.id(), service.currentTournament().orElseThrow().id());
        assertTrue(result.skipped().isEmpty());
        assertTrue(result.rejected().isEmpty());
    }

    @Test
    void rejectsInconsistentPoolAssignmentWithoutPartiallyImportingIt() throws IOException {
        Path source = Files.createDirectory(temporaryDirectory.resolve("source"));
        Path file = source.resolve("inconsistent.json");
        UUID tournamentId = UUID.randomUUID();
        UUID alex = UUID.randomUUID();
        UUID ben = UUID.randomUUID();
        UUID poolOne = UUID.randomUUID();
        UUID poolTwo = UUID.randomUUID();
        UUID boutOne = UUID.randomUUID();
        UUID boutTwo = UUID.randomUUID();
        Files.writeString(file, """
                {
                  "id":"%s", "name":"Broken pools",
                  "settings":{"targetPoolSize":5,"poolBoutScoreLimit":5,"eliminationBoutScoreLimit":15,"advancingFencerCount":16,"tieBreakPolicy":{"criteria":[]}},
                  "fencers":[{"id":"%s","name":"Alex"},{"id":"%s","name":"Ben"}],
                  "seeding":{"fencerIds":["%s","%s"]},
                  "pools":[
                    {"id":"%s","name":"POOL #1","memberIds":["%s","%s"],"bouts":[{"id":"%s","firstFencerId":"%s","secondFencerId":"%s","score":null}]},
                    {"id":"%s","name":"POOL #2","memberIds":["%s","%s"],"bouts":[{"id":"%s","firstFencerId":"%s","secondFencerId":"%s","score":null}]}
                  ], "eliminationBracket":null
                }
                """.formatted(tournamentId, alex, ben, alex, ben, poolOne, alex, ben, boutOne, alex, ben,
                poolTwo, alex, ben, boutTwo, alex, ben));

        TournamentFolderImportResult result = new TournamentService(new JsonTournamentRepository())
                .importTournamentsFromFolder(source);

        assertTrue(result.imported().isEmpty());
        assertEquals(1, result.rejected().size());
        assertTrue(result.rejected().getFirst().detail().contains("Pool assignments"));
    }

    @Test
    void legacyFileWithoutNewTimestampsStillImports() throws IOException {
        Path source = Files.createDirectory(temporaryDirectory.resolve("source"));
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Files.writeString(source.resolve("legacy.json"), """
                {"id":"%s","name":"Legacy","settings":{"targetPoolSize":5,"poolBoutScoreLimit":5,"eliminationBoutScoreLimit":15,"advancingFencerCount":8,"tieBreakPolicy":{"criteria":[]}},
                "fencers":[{"id":"%s","name":"Alex"},{"id":"%s","name":"Ben"}],"seeding":null,"pools":[],"eliminationBracket":null}
                """.formatted(UUID.randomUUID(), first, second));

        TournamentFolderImportResult result = new TournamentService(new JsonTournamentRepository())
                .importTournamentsFromFolder(source);

        assertEquals(1, result.imported().size());
        assertFalse(result.imported().getFirst().tournamentName().isBlank());
    }

    @Test
    void rejectsMissingRequiredDataAndUnknownPersistedEnumsWhileKeepingValidNeighbours() throws IOException {
        Path source = Files.createDirectory(temporaryDirectory.resolve("source"));
        JsonTournamentRepository repository = new JsonTournamentRepository();
        repository.save(tournament("Valid", Instant.parse("2026-08-03T10:15:30Z")), source.resolve("valid.json"));
        Files.writeString(source.resolve("missing-name.json"), "{\"id\":\"" + UUID.randomUUID() + "\"}");
        Files.writeString(source.resolve("unknown-enum.json"), """
                {"id":"%s","name":"Bad enum","settings":{"targetPoolSize":5,"poolBoutScoreLimit":5,"eliminationBoutScoreLimit":15,"advancingFencerCount":8,"tieBreakPolicy":{"criteria":["NOT_A_CRITERION"]}},"fencers":[],"seeding":null,"pools":[],"eliminationBracket":null}
                """.formatted(UUID.randomUUID()));

        TournamentFolderImportResult result = new TournamentService(repository).importTournamentsFromFolder(source);

        assertEquals(1, result.imported().size());
        assertEquals(2, result.rejected().size());
    }

    private static Tournament tournament(String name, Instant timestamp) {
        Fencer alex = Fencer.create("Alex " + name);
        Fencer ben = Fencer.create("Ben " + name);
        return new Tournament(UUID.randomUUID(), name, new TournamentSettings(5, 5, 15, 16,
                new TieBreakPolicy(List.of())), List.of(alex, ben), new Seeding(List.of(alex.id(), ben.id())),
                List.of(), null, timestamp, null);
    }
}
