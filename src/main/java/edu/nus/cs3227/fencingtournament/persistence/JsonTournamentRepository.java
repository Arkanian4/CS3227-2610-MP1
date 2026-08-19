package edu.nus.cs3227.fencingtournament.persistence;

import edu.nus.cs3227.fencingtournament.application.TournamentRepository;
import edu.nus.cs3227.fencingtournament.domain.Tournament;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * JSON persistence boundary placeholder. Serialization is intentionally deferred until the
 * domain's authoritative mutation and rehydration rules are implemented.
 */
public final class JsonTournamentRepository implements TournamentRepository {
    @Override
    public Optional<Tournament> load(Path path) throws IOException {
        throw new UnsupportedOperationException("JSON loading is not implemented yet.");
    }

    @Override
    public void save(Tournament tournament, Path path) throws IOException {
        throw new UnsupportedOperationException("JSON saving is not implemented yet.");
    }
}

