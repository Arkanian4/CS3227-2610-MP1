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

/** Placeholder for user-workflow orchestration and persistence coordination. */
public final class TournamentService {
    private final TournamentRepository repository;
    private final PoolGenerator poolGenerator = new PoolGenerator();
    private final BracketGenerator bracketGenerator = new BracketGenerator();
    private final StandingsCalculator standingsCalculator = new StandingsCalculator();
    private final FinalStandingsCalculator finalStandingsCalculator = new FinalStandingsCalculator();
    private final Map<UUID, Tournament> tournaments = new LinkedHashMap<>();
    private final Map<UUID, Path> tournamentFiles = new LinkedHashMap<>();
    private final Set<String> tournamentNameKeys = new HashSet<>();
    private Tournament activeTournament;

    public TournamentService(TournamentRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("Tournament repository must not be null.");
        }
        this.repository = repository;
    }

    public Tournament createTournament(String name) {
        String normalized = normalizeName(name);
        if (tournamentNameKeys.contains(nameKey(normalized))) {
            throw new IllegalArgumentException("A tournament with this name already exists. Choose a different name.");
        }
        activeTournament = Tournament.create(normalized, defaultSettings());
        tournaments.put(activeTournament.id(), activeTournament);
        tournamentNameKeys.add(nameKey(activeTournament.name()));
        return activeTournament;
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
            tournamentFiles.put(tournament.id(), path);
            tournamentNameKeys.add(key);
        });
        return loaded;
    }

    public void saveTournament(Path path) throws IOException {
        Tournament tournament = requireActiveTournament();
        repository.save(tournament, path);
        tournamentFiles.put(tournament.id(), path);
    }

    public List<Tournament> listTournaments() {
        return tournaments.values().stream().sorted(Comparator.comparing(Tournament::name, String.CASE_INSENSITIVE_ORDER)).toList();
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
                    tournamentFiles.put(tournament.id(), file);
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
            tournamentFiles.put(tournament.id(), file);
        }
    }

    public Fencer addFencer(String name) {
        Fencer fencer = Fencer.create(name);
        requireActiveTournament().addFencer(fencer);
        return fencer;
    }

    public boolean removeFencer(UUID fencerId) {
        return requireActiveTournament().removeFencer(fencerId);
    }

    public void recordPoolBoutResult(UUID poolId, UUID boutId, BoutScore score) {
        requireActiveTournament().recordPoolBoutResult(poolId, boutId, score);
    }

    public void replacePoolBoutResult(UUID poolId, UUID boutId, BoutScore score) {
        replacePoolBoutResult(poolId, boutId, score, false);
    }

    public boolean poolEditNeedsReset() { return requireActiveTournament().eliminationBracket() != null; }

    public void replacePoolBoutResult(UUID poolId, UUID boutId, BoutScore score, boolean resetElimination) {
        requireActiveTournament().replacePoolBoutResult(poolId, boutId, score, resetElimination);
    }

    public void seedFencers(List<UUID> orderedFencerIds) {
        requireActiveTournament().applySeeding(new Seeding(orderedFencerIds));
    }

    public void generatePools() {
        generatePools(requireActiveTournament().settings().targetPoolSize());
    }

    public void generatePools(int maximumPoolSize) {
        Tournament tournament = requireActiveTournament();
        tournament.installPools(poolGenerator.generate(
                tournament.seeding(), maximumPoolSize));
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
        List<UUID> qualified = overallStandings().stream()
                .sorted(java.util.Comparator.comparingInt(OverallStanding::rank))
                .limit(16)
                .map(OverallStanding::fencerId).toList();
        EliminationBracket bracket = bracketGenerator.generate(qualified);
        tournament.installEliminationBracket(bracket);
        return bracket;
    }

    public void recordEliminationBoutResult(UUID matchId, BoutScore score) {
        requireActiveTournament().recordEliminationBoutResult(matchId, score);
    }

    public boolean eliminationEditNeedsReset(UUID matchId) { return requireActiveTournament().eliminationEditNeedsReset(matchId); }

    public void replaceEliminationBoutResult(UUID matchId, BoutScore score, boolean resetDownstream) {
        requireActiveTournament().replaceEliminationBoutResult(matchId, score, resetDownstream);
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
