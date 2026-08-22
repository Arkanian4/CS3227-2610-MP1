package edu.nus.cs3227.fencingtournament.persistence;

import edu.nus.cs3227.fencingtournament.application.TournamentRepository;
import edu.nus.cs3227.fencingtournament.domain.Fencer;
import edu.nus.cs3227.fencingtournament.domain.Seeding;
import edu.nus.cs3227.fencingtournament.domain.Tournament;
import edu.nus.cs3227.fencingtournament.domain.TournamentSettings;
import edu.nus.cs3227.fencingtournament.domain.elimination.EliminationBracket;
import edu.nus.cs3227.fencingtournament.domain.pool.Pool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JSON persistence for a complete local tournament aggregate. */
public final class JsonTournamentRepository implements TournamentRepository {
    private final ObjectMapper objectMapper;

    public JsonTournamentRepository() {
        objectMapper = new ObjectMapper()
                .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }

    @Override
    public Optional<Tournament> load(Path path) throws IOException {
        requirePath(path);
        if (Files.notExists(path)) {
            return Optional.empty();
        }

        try {
            PersistedTournament persisted = objectMapper.readValue(path.toFile(), PersistedTournament.class);
            if (persisted == null) {
                throw new IOException("Tournament JSON must contain an object.");
            }
            Instant lastModified = parseLastModified(persisted.lastModified(), path);
            return Optional.of(new Tournament(
                    persisted.id(),
                    persisted.name(),
                    persisted.settings(),
                    persisted.fencers(),
                    persisted.seeding(),
                    persisted.pools(),
                    persisted.eliminationBracket(),
                    lastModified,
                    parseCompletedAt(persisted.completedAt(), path, persisted.eliminationBracket(), lastModified)));
        } catch (JsonProcessingException exception) {
            throw exception;
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IOException("Persisted tournament violates domain invariants.", exception);
        }
    }

    @Override
    public void save(Tournament tournament, Path path) throws IOException {
        if (tournament == null) {
            throw new IllegalArgumentException("Tournament must not be null.");
        }
        requirePath(path);

        if (Files.exists(path)) {
            Optional<Tournament> existing = load(path);
            if (existing.isPresent() && !existing.get().id().equals(tournament.id())) {
                throw new IOException("A different tournament already exists at this path.");
            }
        }

        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        PersistedTournament persisted = new PersistedTournament(
                tournament.id(),
                tournament.name(),
                tournament.settings(),
                tournament.fencers(),
                tournament.seeding(),
                tournament.pools(),
                tournament.eliminationBracket(),
                tournament.lastModified().toString(),
                tournament.completedAt().map(Instant::toString).orElse(null));
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(persisted);
        Path temporary = Files.createTempFile(parent, path.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temporary, json, StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public void delete(Path path) throws IOException {
        requirePath(path);
        Files.deleteIfExists(path);
    }

    private static void requirePath(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Persistence path must not be null.");
        }
    }

    private static Instant parseLastModified(String persistedTimestamp, Path sourcePath) throws IOException {
        if (persistedTimestamp == null || persistedTimestamp.isBlank()) return fileModificationTime(sourcePath);
        try {
            return Instant.parse(persistedTimestamp);
        } catch (DateTimeParseException exception) {
            throw new IOException("Tournament last-modified timestamp is invalid.", exception);
        }
    }

    private static Instant fileModificationTime(Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant();
        } catch (IOException exception) {
            return Instant.EPOCH;
        }
    }

    private static Instant parseCompletedAt(String persistedTimestamp, Path sourcePath,
                                            EliminationBracket bracket, Instant lastModified) throws IOException {
        if (bracket == null || !bracket.isComplete()) return null;
        if (persistedTimestamp == null || persistedTimestamp.isBlank()) return fileModificationTime(sourcePath);
        try {
            return Instant.parse(persistedTimestamp);
        } catch (DateTimeParseException exception) {
            throw new IOException("Tournament completion timestamp is invalid.", exception);
        }
    }

    /** Persistence-only representation; domain reconstruction happens through Tournament. */
    private record PersistedTournament(UUID id, String name, TournamentSettings settings,
                                       List<Fencer> fencers, Seeding seeding, List<Pool> pools,
                                       EliminationBracket eliminationBracket, String lastModified,
                                       String completedAt) {
    }
}
