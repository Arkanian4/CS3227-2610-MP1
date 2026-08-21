package edu.nus.cs3227.fencingtournament.application;

import edu.nus.cs3227.fencingtournament.domain.Tournament;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/** Persistence boundary for complete local tournament aggregates. */
public interface TournamentRepository {
    Optional<Tournament> load(Path path) throws IOException;

    void save(Tournament tournament, Path path) throws IOException;

    /** Removes one persisted tournament aggregate, if it exists. */
    void delete(Path path) throws IOException;
}
