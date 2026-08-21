package edu.nus.cs3227.fencingtournament.persistence;

import edu.nus.cs3227.fencingtournament.domain.Fencer;
import edu.nus.cs3227.fencingtournament.domain.Seeding;
import edu.nus.cs3227.fencingtournament.domain.Tournament;
import edu.nus.cs3227.fencingtournament.domain.TournamentSettings;
import edu.nus.cs3227.fencingtournament.domain.standings.TieBreakCriterion;
import edu.nus.cs3227.fencingtournament.domain.standings.TieBreakPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonTournamentRepositoryTest {
    @TempDir
    Path temporaryDirectory;

    private final JsonTournamentRepository repository = new JsonTournamentRepository();

    @Test
    void saveThenLoadPreservesTournamentState() throws IOException {
        UUID tournamentId = UUID.randomUUID();
        Fencer first = new Fencer(UUID.randomUUID(), "Alex Tan");
        Fencer second = new Fencer(UUID.randomUUID(), "Jamie Lim");
        TournamentSettings settings = testSettings();
        Tournament original = new Tournament(tournamentId, "Internal Open", settings,
                List.of(first, second), new Seeding(List.of(second.id(), first.id())), List.of(), null);
        Path file = temporaryDirectory.resolve("tournament.json");

        repository.save(original, file);
        Tournament loaded = repository.load(file).orElseThrow();

        assertEquals(original.id(), loaded.id());
        assertEquals(original.name(), loaded.name());
        assertEquals(original.settings(), loaded.settings());
        assertEquals(original.fencers(), loaded.fencers());
        assertEquals(original.seeding(), loaded.seeding());
        assertEquals(original.pools(), loaded.pools());
        assertEquals(original.eliminationBracket(), loaded.eliminationBracket());
    }

    @Test
    void multipleFencersAndTheirUuidsSurviveRoundTrip() throws IOException {
        Fencer first = Fencer.create("Alex Tan");
        Fencer second = Fencer.create("Alex Tan (2)");
        Fencer third = Fencer.create("Jamie Lim");
        Tournament original = Tournament.create("Internal Open", testSettings());
        original.addFencer(first);
        original.addFencer(second);
        original.addFencer(third);
        Path file = temporaryDirectory.resolve("roster.json");

        repository.save(original, file);
        Tournament loaded = repository.load(file).orElseThrow();

        assertEquals(List.of(first.id(), second.id(), third.id()),
                loaded.fencers().stream().map(Fencer::id).toList());
        assertEquals(List.of("Alex Tan", "Alex Tan (2)", "Jamie Lim"),
                loaded.fencers().stream().map(Fencer::name).toList());
    }

    @Test
    void legacySetupSaveWithoutSeedingLoadsWithRegistrationOrderAsItsSeedOrder() throws IOException {
        Fencer first = Fencer.create("Alex Tan");
        Fencer second = Fencer.create("Jamie Lim");
        Path file = temporaryDirectory.resolve("legacy-setup.json");
        Files.writeString(file, """
                {
                  "id": "%s",
                  "name": "Internal Open",
                  "settings": {
                    "targetPoolSize": 5,
                    "poolBoutScoreLimit": 5,
                    "eliminationBoutScoreLimit": 15,
                    "advancingFencerCount": 8,
                    "tieBreakPolicy": { "criteria": [] }
                  },
                  "fencers": [
                    { "id": "%s", "name": "Alex Tan" },
                    { "id": "%s", "name": "Jamie Lim" }
                  ],
                  "seeding": null,
                  "pools": [],
                  "eliminationBracket": null
                }
                """.formatted(UUID.randomUUID(), first.id(), second.id()));

        Tournament loaded = repository.load(file).orElseThrow();

        assertEquals(List.of(first.id(), second.id()), loaded.seeding().fencerIds());
    }

    @Test
    void settingsSurviveRoundTrip() throws IOException {
        TournamentSettings settings = testSettings();
        Tournament original = Tournament.create("Internal Open", settings);
        Path file = temporaryDirectory.resolve("settings.json");

        repository.save(original, file);

        assertEquals(settings, repository.load(file).orElseThrow().settings());
    }

    @Test
    void missingFileReturnsEmptyOptional() throws IOException {
        Path missingFile = temporaryDirectory.resolve("missing.json");

        Optional<Tournament> loaded = repository.load(missingFile);

        assertTrue(loaded.isEmpty());
    }

    @Test
    void malformedJsonRaisesIOException() throws IOException {
        Path malformedFile = temporaryDirectory.resolve("malformed.json");
        Files.writeString(malformedFile, "{ this is not valid JSON }");

        assertThrows(IOException.class, () -> repository.load(malformedFile));
    }

    @Test
    void invalidPersistedRosterRaisesIOException() throws IOException {
        UUID duplicateId = UUID.randomUUID();
        Path invalidFile = temporaryDirectory.resolve("invalid-roster.json");
        Files.writeString(invalidFile, """
                {
                  "id": "%s",
                  "name": "Internal Open",
                  "settings": {
                    "targetPoolSize": 5,
                    "poolBoutScoreLimit": 5,
                    "eliminationBoutScoreLimit": 15,
                    "advancingFencerCount": 8,
                    "tieBreakPolicy": { "criteria": [] }
                  },
                  "fencers": [
                    { "id": "%s", "name": "Alex Tan" },
                    { "id": "%s", "name": "Alex T." }
                  ],
                  "seeding": null,
                  "pools": [],
                  "eliminationBracket": null
                }
                """.formatted(UUID.randomUUID(), duplicateId, duplicateId));

        assertThrows(IOException.class, () -> repository.load(invalidFile));
    }

    private static TournamentSettings testSettings() {
        return new TournamentSettings(5, 5, 15, 8,
                new TieBreakPolicy(List.of(TieBreakCriterion.VICTORY_RATIO,
                        TieBreakCriterion.INDICATOR, TieBreakCriterion.SEED)));
    }
}
