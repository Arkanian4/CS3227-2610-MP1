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
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Placeholder for user-workflow orchestration and persistence coordination. */
public final class TournamentService {
    private final TournamentRepository repository;
    private final PoolGenerator poolGenerator = new PoolGenerator();
    private final BracketGenerator bracketGenerator = new BracketGenerator();
    private final StandingsCalculator standingsCalculator = new StandingsCalculator();
    private final FinalStandingsCalculator finalStandingsCalculator = new FinalStandingsCalculator();
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

    public void recordPoolBoutResult(UUID poolId, UUID boutId, BoutScore score) {
        requireActiveTournament().recordPoolBoutResult(poolId, boutId, score);
    }

    public void replacePoolBoutResult(UUID poolId, UUID boutId, BoutScore score) {
        requireActiveTournament().replacePoolBoutResult(poolId, boutId, score);
    }

    public void seedFencers(List<UUID> orderedFencerIds) {
        requireActiveTournament().applySeeding(new Seeding(orderedFencerIds));
    }

    public void generatePools() {
        Tournament tournament = requireActiveTournament();
        tournament.installPools(poolGenerator.generate(
                tournament.seeding(), tournament.settings().targetPoolSize()));
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
                .limit(tournament.settings().advancingFencerCount())
                .map(OverallStanding::fencerId).toList();
        EliminationBracket bracket = bracketGenerator.generate(qualified);
        tournament.installEliminationBracket(bracket);
        return bracket;
    }

    public void recordEliminationBoutResult(UUID matchId, BoutScore score) {
        requireActiveTournament().recordEliminationBoutResult(matchId, score);
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
