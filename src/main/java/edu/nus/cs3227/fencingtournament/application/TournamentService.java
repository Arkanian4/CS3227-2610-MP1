package edu.nus.cs3227.fencingtournament.application;

import edu.nus.cs3227.fencingtournament.domain.Fencer;
import edu.nus.cs3227.fencingtournament.domain.Tournament;
import edu.nus.cs3227.fencingtournament.domain.TournamentSettings;
import edu.nus.cs3227.fencingtournament.domain.Seeding;
import edu.nus.cs3227.fencingtournament.domain.TournamentPhase;
import edu.nus.cs3227.fencingtournament.domain.pool.Pool;
import edu.nus.cs3227.fencingtournament.domain.rules.PoolGenerator;
import edu.nus.cs3227.fencingtournament.domain.rules.BracketGenerator;
import edu.nus.cs3227.fencingtournament.domain.rules.StandingsCalculator;
import edu.nus.cs3227.fencingtournament.domain.rules.FinalStandingsCalculator;
import edu.nus.cs3227.fencingtournament.domain.standings.PoolStanding;
import edu.nus.cs3227.fencingtournament.domain.standings.OverallStanding;
import edu.nus.cs3227.fencingtournament.domain.standings.FinalStanding;
import edu.nus.cs3227.fencingtournament.domain.pool.BoutScore;
import edu.nus.cs3227.fencingtournament.domain.elimination.EliminationBracket;
import edu.nus.cs3227.fencingtournament.domain.standings.TieBreakCriterion;
import edu.nus.cs3227.fencingtournament.domain.standings.TieBreakPolicy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/** Coordinates tournament workflows, derived read models, and optional autosave. */
public final class TournamentService {
    private final TournamentRepository repository;
    private final PoolGenerator poolGenerator = new PoolGenerator();
    private final BracketGenerator bracketGenerator = new BracketGenerator();
    private final StandingsCalculator standingsCalculator = new StandingsCalculator();
    private final FinalStandingsCalculator finalStandingsCalculator = new FinalStandingsCalculator();
    private final Map<UUID, Tournament> tournaments = new LinkedHashMap<>();
    private final Set<String> tournamentNameKeys = new HashSet<>();
    private final Path autoSaveDirectory;
    private Tournament activeTournament;

    public TournamentService(TournamentRepository repository) {
        this(repository, null);
    }

    /** Creates a service that autosaves the complete local tournament collection after each mutation. */
    public TournamentService(TournamentRepository repository, Path autoSaveDirectory) {
        if (repository == null) {
            throw new IllegalArgumentException("Tournament repository must not be null.");
        }
        this.repository = repository;
        this.autoSaveDirectory = autoSaveDirectory;
    }

    public Tournament createTournament(String name) {
        String normalized = normalizeName(name);
        if (tournamentNameKeys.contains(nameKey(normalized))) {
            throw new IllegalArgumentException("A tournament with this name already exists. Choose a different name.");
        }
        return mutate(() -> {
            activeTournament = Tournament.create(normalized, defaultSettings());
            tournaments.put(activeTournament.id(), activeTournament);
            tournamentNameKeys.add(nameKey(activeTournament.name()));
            return activeTournament;
        });
    }

    public Optional<Tournament> loadTournament(Path path) throws IOException {
        Optional<Tournament> loaded = repository.load(path);
        loaded.ifPresent(tournament -> {
            String key = nameKey(tournament.name());
            Tournament existing = tournaments.values().stream().filter(item -> nameKey(item.name()).equals(key)).findFirst().orElse(null);
            if (existing != null && !existing.id().equals(tournament.id())) {
                throw new IllegalArgumentException("A tournament with this name is already open. Choose a different tournament.");
            }
            activeTournament = tournament;
            tournaments.put(tournament.id(), tournament);
            tournamentNameKeys.add(key);
        });
        if (loaded.isPresent() && autoSaveDirectory != null) {
            try {
                saveAll(autoSaveDirectory);
            } catch (IOException exception) {
                throw new TournamentPersistenceException("Tournament loaded, but automatic saving failed.", exception);
            }
        }
        return loaded;
    }

    public List<Tournament> listTournaments() {
        return tournaments.values().stream()
                .sorted(Comparator.comparing(Tournament::lastModified).reversed()
                        .thenComparing(Tournament::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public Tournament openTournament(UUID tournamentId) {
        Tournament selected = tournaments.get(tournamentId);
        if (selected == null) throw new IllegalArgumentException("Tournament does not exist.");
        activeTournament = selected;
        return selected;
    }

    public Tournament openTournament(String name) {
        String key = nameKey(normalizeName(name));
        return tournaments.values().stream().filter(item -> nameKey(item.name()).equals(key)).findFirst()
                .map(item -> openTournament(item.id())).orElseThrow(() -> new IllegalArgumentException("Tournament does not exist."));
    }

    /**
     * Removes one tournament from the local collection and its autosave file.
     * Returns false without persisting when the identifier is not present.
     */
    public boolean deleteTournament(UUID tournamentId) {
        Tournament tournament = tournaments.get(tournamentId);
        if (tournament == null) return false;

        if (autoSaveDirectory != null) {
            Path file = autoSaveDirectory.resolve(safeFileName(tournament.name()) + ".json");
            try {
                repository.delete(file);
            } catch (IOException exception) {
                throw new TournamentPersistenceException("Tournament was not deleted because local storage could not be updated.", exception);
            }
        }

        tournaments.remove(tournamentId);
        tournamentNameKeys.remove(nameKey(tournament.name()));
        if (tournament == activeTournament) activeTournament = null;

        if (autoSaveDirectory != null) {
            try {
                saveAll(autoSaveDirectory);
            } catch (IOException exception) {
                throw new TournamentPersistenceException("Tournament was deleted, but automatic saving failed.", exception);
            }
        }
        return true;
    }

    public void returnToTournamentHome() { activeTournament = null; }

    public List<Tournament> loadAll(Path directory) throws IOException {
        if (directory == null) throw new IllegalArgumentException("Tournament directory must not be null.");
        if (Files.notExists(directory)) return List.of();
        List<Tournament> loaded = new ArrayList<>();
        try (var stream = Files.list(directory)) {
            for (Path file : stream.filter(path -> path.toString().toLowerCase().endsWith(".json")).toList()) {
                Optional<Tournament> value = repository.load(file);
                if (value.isEmpty()) continue;
                Tournament tournament = value.get();
                if (tournamentNameKeys.add(nameKey(tournament.name()))) {
                    tournaments.put(tournament.id(), tournament);
                    loaded.add(tournament);
                }
            }
        }
        return List.copyOf(loaded);
    }

    public void saveAll(Path directory) throws IOException {
        if (directory == null) throw new IllegalArgumentException("Tournament directory must not be null.");
        Files.createDirectories(directory);
        for (Tournament tournament : tournaments.values()) {
            Path file = directory.resolve(safeFileName(tournament.name()) + ".json");
            repository.save(tournament, file);
        }
    }

    public Fencer addFencer(String name) {
        Fencer fencer = Fencer.create(name);
        return mutate(() -> {
            requireActiveTournament().addFencer(fencer);
            return fencer;
        });
    }

    public boolean removeFencer(UUID fencerId) {
        boolean removed = requireActiveTournament().removeFencer(fencerId);
        if (removed) persistSuccessfulMutation();
        return removed;
    }

    public void recordPoolBoutResult(UUID poolId, UUID boutId, BoutScore score) {
        mutate(() -> { requireActiveTournament().recordPoolBoutResult(poolId, boutId, score); return null; });
    }

    public void replacePoolBoutResult(UUID poolId, UUID boutId, BoutScore score) {
        replacePoolBoutResult(poolId, boutId, score, false);
    }

    public boolean poolEditNeedsReset() { return requireActiveTournament().eliminationBracket() != null; }

    public void replacePoolBoutResult(UUID poolId, UUID boutId, BoutScore score, boolean resetElimination) {
        mutate(() -> { requireActiveTournament().replacePoolBoutResult(poolId, boutId, score, resetElimination); return null; });
    }

    public void seedFencers(List<UUID> orderedFencerIds) {
        mutate(() -> { requireActiveTournament().applySeeding(new Seeding(orderedFencerIds)); return null; });
    }

    public void generatePools() {
        generatePools(requireActiveTournament().settings().targetPoolSize());
    }

    public void generatePools(int maximumPoolSize) {
        mutate(() -> {
            Tournament tournament = requireActiveTournament();
            tournament.installPools(poolGenerator.generate(tournament.seeding(), maximumPoolSize));
            return null;
        });
    }

    /** Moves one setup fencer to a zero-based target seed position before pools are generated. */
    public boolean moveSeedFencer(UUID fencerId, int targetSeedIndex) {
        Tournament tournament = requireActiveTournament();
        if (!tournament.pools().isEmpty() || tournament.eliminationBracket() != null || tournament.seeding() == null) {
            return false;
        }
        List<UUID> orderedIds = new ArrayList<>(tournament.seeding().fencerIds());
        int sourceIndex = orderedIds.indexOf(fencerId);
        if (sourceIndex < 0 || targetSeedIndex < 0 || targetSeedIndex >= orderedIds.size()
                || sourceIndex == targetSeedIndex) return false;
        return mutate(() -> {
            UUID moved = orderedIds.remove(sourceIndex);
            orderedIds.add(targetSeedIndex, moved);
            tournament.applySeeding(new Seeding(orderedIds));
            return true;
        });
    }

    public List<Pool> pools() {
        return requireActiveTournament().pools();
    }

    public Optional<Pool> findPool(UUID poolId) {
        return pools().stream().filter(pool -> pool.id().equals(poolId)).findFirst();
    }

    public List<PoolStanding> standingsForPool(UUID poolId) {
        Tournament tournament = requireActiveTournament();
        Pool pool = findPool(poolId).orElseThrow(() ->
                new IllegalArgumentException("Pool does not belong to this tournament."));
        return standingsCalculator.calculatePoolStandings(
                pool, tournament.seeding(), tournament.settings().tieBreakPolicy());
    }

    public List<OverallStanding> overallStandings() {
        Tournament tournament = requireActiveTournament();
        List<Pool> tournamentPools = tournament.pools();
        if (tournamentPools.isEmpty() || tournamentPools.stream().anyMatch(pool -> !pool.isComplete())) {
            throw new IllegalStateException("Pool Result is available after all pool bouts are complete.");
        }
        return standingsCalculator.calculateOverallStandings(
                tournamentPools, tournament.seeding(), tournament.settings().tieBreakPolicy());
    }

    public EliminationBracket generateEliminationBracket() {
        Tournament tournament = requireActiveTournament();
        if (tournament.eliminationBracket() != null) return tournament.eliminationBracket();
        return mutate(() -> {
            List<UUID> qualified = overallStandings().stream()
                    .sorted(java.util.Comparator.comparingInt(OverallStanding::rank))
                    .limit(16)
                    .map(OverallStanding::fencerId).toList();
            EliminationBracket bracket = bracketGenerator.generate(qualified);
            tournament.installEliminationBracket(bracket);
            return bracket;
        });
    }

    public void recordEliminationBoutResult(UUID matchId, BoutScore score) {
        mutate(() -> { requireActiveTournament().recordEliminationBoutResult(matchId, score); return null; });
    }

    public boolean eliminationEditNeedsReset(UUID matchId) { return requireActiveTournament().eliminationEditNeedsReset(matchId); }

    public boolean eliminationEditNeedsReset(UUID matchId, BoutScore score) {
        return requireActiveTournament().eliminationEditNeedsReset(matchId, score);
    }

    public void replaceEliminationBoutResult(UUID matchId, BoutScore score, boolean resetDownstream) {
        mutate(() -> { requireActiveTournament().replaceEliminationBoutResult(matchId, score, resetDownstream); return null; });
    }

    /** Returns final placements only after the completed DE bracket determines a champion. */
    public List<FinalStanding> finalStandings() {
        Tournament tournament = requireActiveTournament();
        return finalStandingsCalculator.calculate(overallStandings(), tournament.eliminationBracket());
    }

    public PoolProgress poolProgress() {
        int total = 0;
        int completed = 0;
        for (Pool pool : pools()) {
            total += pool.bouts().size();
            completed += (int) pool.bouts().stream().filter(bout -> bout.score() != null).count();
        }
        return new PoolProgress(completed, total);
    }

    public TournamentPhase currentPhase() {
        return requireActiveTournament().phase();
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

    private <T> T mutate(Supplier<T> operation) {
        T result = operation.get();
        persistSuccessfulMutation();
        return result;
    }

    /** Touches the active aggregate only after a domain mutation has succeeded. */
    private void persistSuccessfulMutation() {
        requireActiveTournament().markModified();
        if (autoSaveDirectory == null) return;
        try {
            saveAll(autoSaveDirectory);
        } catch (IOException exception) {
            throw new TournamentPersistenceException("Change applied, but automatic saving failed.", exception);
        }
    }

    private static String normalizeName(String name) {
        if (name == null || name.trim().isBlank()) throw new IllegalArgumentException("Tournament name must not be blank.");
        return name.trim();
    }

    private static String nameKey(String name) {
        return name.toLowerCase(java.util.Locale.ROOT);
    }

    private static String safeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]+", "_");
    }

    private static TournamentSettings defaultSettings() {
        return new TournamentSettings(
                5,
                5,
                15,
                16,
                new TieBreakPolicy(List.of(
                        TieBreakCriterion.VICTORY_RATIO,
                        TieBreakCriterion.INDICATOR,
                        TieBreakCriterion.TOUCHES_SCORED,
                        TieBreakCriterion.SEED)));
    }
}
