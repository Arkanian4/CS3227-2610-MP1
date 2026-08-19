package edu.nus.cs3227.fencingtournament.application;

import edu.nus.cs3227.fencingtournament.domain.Fencer;
import edu.nus.cs3227.fencingtournament.domain.Tournament;
import edu.nus.cs3227.fencingtournament.domain.TournamentSettings;
import edu.nus.cs3227.fencingtournament.domain.standings.TieBreakCriterion;
import edu.nus.cs3227.fencingtournament.domain.standings.TieBreakPolicy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Placeholder for user-workflow orchestration and persistence coordination. */
public final class TournamentService {
    private final TournamentRepository repository;
    private Tournament activeTournament;

    public TournamentService(TournamentRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("Tournament repository must not be null.");
        }
        this.repository = repository;
    }

    public Tournament createTournament(String name) {
        activeTournament = Tournament.create(name, defaultSettings());
        return activeTournament;
    }

    public Optional<Tournament> loadTournament(Path path) throws IOException {
        Optional<Tournament> loaded = repository.load(path);
        loaded.ifPresent(tournament -> activeTournament = tournament);
        return loaded;
    }

    public void saveTournament(Path path) throws IOException {
        repository.save(requireActiveTournament(), path);
    }

    public Fencer addFencer(String name) {
        Fencer fencer = Fencer.create(name);
        requireActiveTournament().addFencer(fencer);
        return fencer;
    }

    public boolean removeFencer(UUID fencerId) {
        return requireActiveTournament().removeFencer(fencerId);
    }

    public Optional<Tournament> currentTournament() {
        return Optional.ofNullable(activeTournament);
    }

    public List<Fencer> registeredFencers() {
        return currentTournament().map(Tournament::fencers).orElseGet(List::of);
    }

    private Tournament requireActiveTournament() {
        if (activeTournament == null) {
            throw new IllegalStateException("Create or load a tournament first.");
        }
        return activeTournament;
    }

    private static TournamentSettings defaultSettings() {
        return new TournamentSettings(
                5,
                5,
                15,
                8,
                new TieBreakPolicy(List.of(
                        TieBreakCriterion.VICTORY_RATIO,
                        TieBreakCriterion.INDICATOR,
                        TieBreakCriterion.TOUCHES_SCORED,
                        TieBreakCriterion.SEED)));
    }
}
