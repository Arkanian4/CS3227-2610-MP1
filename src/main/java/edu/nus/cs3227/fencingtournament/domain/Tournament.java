package edu.nus.cs3227.fencingtournament.domain;

import edu.nus.cs3227.fencingtournament.domain.elimination.EliminationBracket;
import edu.nus.cs3227.fencingtournament.domain.pool.Pool;
import edu.nus.cs3227.fencingtournament.domain.pool.BoutScore;
import edu.nus.cs3227.fencingtournament.domain.rules.PoolGenerator;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;

/** Aggregate root for one locally managed tournament. Behaviour is added incrementally. */
public final class Tournament {
    private final UUID id;
    private final String name;
    private final TournamentSettings settings;
    private final List<Fencer> fencers;
    private Seeding seeding;
    private final List<Pool> pools;
    private EliminationBracket eliminationBracket;

    /** Creates a new tournament in the registration phase with an empty roster. */
    public Tournament(UUID id, String name, TournamentSettings settings) {
        this(id, name, settings, List.of(), null, List.of(), null);
    }

    /** Creates a new tournament with a generated identity. */
    public static Tournament create(String name, TournamentSettings settings) {
        return new Tournament(UUID.randomUUID(), name, settings);
    }

    public Tournament(UUID id, String name, TournamentSettings settings, List<Fencer> fencers,
                      Seeding seeding, List<Pool> pools, EliminationBracket eliminationBracket) {
        this.id = requireId(id);
        this.name = requireName(name, "Tournament name");
        this.settings = Objects.requireNonNull(settings, "Tournament settings must not be null.");
        validateRoster(fencers);
        this.fencers = new ArrayList<>(fencers);
        this.seeding = seeding;
        this.pools = new ArrayList<>(pools);
        validatePoolResults(this.pools, settings.poolBoutScoreLimit());
        this.eliminationBracket = eliminationBracket;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public TournamentSettings settings() {
        return settings;
    }

    public List<Fencer> fencers() {
        return List.copyOf(fencers);
    }

    /** Adds a participant while the tournament is still in registration. */
    public void addFencer(Fencer fencer) {
        requireRegistrationPhase();
        Objects.requireNonNull(fencer, "Fencer must not be null.");
        if (findFencer(fencer.id()).isPresent()) {
            throw new IllegalArgumentException("A fencer with this ID is already registered.");
        }
        if (fencers.stream().anyMatch(existing -> existing.name().equals(fencer.name()))) {
            throw new IllegalArgumentException("A fencer with this name is already registered.");
        }
        fencers.add(fencer);
    }

    /** Removes a participant by ID and reports whether a fencer was removed. */
    public boolean removeFencer(UUID fencerId) {
        requireRegistrationPhase();
        if (fencerId == null) {
            throw new IllegalArgumentException("Fencer ID must not be null.");
        }
        return fencers.removeIf(fencer -> fencer.id().equals(fencerId));
    }

    /** Finds a registered participant by immutable identity. */
    public Optional<Fencer> findFencer(UUID fencerId) {
        if (fencerId == null) {
            throw new IllegalArgumentException("Fencer ID must not be null.");
        }
        return fencers.stream()
                .filter(fencer -> fencer.id().equals(fencerId))
                .findFirst();
    }

    public Seeding seeding() {
        return seeding;
    }

    /** Accepts a complete seed order before pools are generated. */
    public void applySeeding(Seeding newSeeding) {
        if (!pools.isEmpty() || eliminationBracket != null) {
            throw new IllegalStateException("Seeding cannot change after pools are generated.");
        }
        if (newSeeding == null || newSeeding.fencerIds().size() != fencers.size()
                || !new java.util.HashSet<>(newSeeding.fencerIds()).equals(
                fencers.stream().map(Fencer::id).collect(java.util.stream.Collectors.toSet()))) {
            throw new IllegalArgumentException("Seeding must contain every registered fencer exactly once.");
        }
        seeding = newSeeding;
    }

    /** Installs validated generated pools for the current seeding. */
    public void installPools(List<Pool> generatedPools) {
        if (seeding == null) {
            throw new IllegalStateException("Complete seeding is required before generating pools.");
        }
        if (!pools.isEmpty() || eliminationBracket != null) {
            throw new IllegalStateException("Pools have already been generated.");
        }
        if (generatedPools == null || generatedPools.isEmpty()) {
            throw new IllegalArgumentException("Generated pools must not be empty.");
        }

        Set<UUID> rosterIds = fencers.stream().map(Fencer::id).collect(java.util.stream.Collectors.toSet());
        Set<UUID> assignedIds = new HashSet<>();
        for (Pool pool : generatedPools) {
            if (pool == null) {
                throw new IllegalArgumentException("Generated pools must not contain null entries.");
            }
            for (UUID memberId : pool.memberIds()) {
                if (!rosterIds.contains(memberId) || !assignedIds.add(memberId)) {
                    throw new IllegalArgumentException("Generated pools must cover each registered fencer exactly once.");
                }
            }
        }
        if (!assignedIds.equals(rosterIds)) {
            throw new IllegalArgumentException("Generated pools must include every registered fencer.");
        }
        pools.addAll(generatedPools);
    }

    public TournamentPhase phase() {
        if (eliminationBracket != null) {
            return eliminationBracket.isComplete() ? TournamentPhase.COMPLETE : TournamentPhase.ELIMINATION_PHASE;
        }
        if (!pools.isEmpty()) {
            return TournamentPhase.POOL_PHASE;
        }
        return seeding == null ? TournamentPhase.REGISTRATION : TournamentPhase.SEEDING;
    }

    public List<Pool> pools() {
        return List.copyOf(pools);
    }

    /** Records a result for an incomplete scheduled pool bout. */
    public void recordPoolBoutResult(UUID poolId, UUID boutId, BoutScore score) {
        updatePool(poolId, pool -> pool.recordBoutResult(boutId, score,
                settings.poolBoutScoreLimit()));
    }

    /** Explicitly replaces a previously recorded result for correction purposes. */
    public void replacePoolBoutResult(UUID poolId, UUID boutId, BoutScore score) {
        replacePoolBoutResult(poolId, boutId, score, false);
    }

    /** Corrects a pool result and, if explicitly authorised, invalidates the dependent DE bracket. */
    public void replacePoolBoutResult(UUID poolId, UUID boutId, BoutScore score, boolean resetElimination) {
        if (eliminationBracket != null && !resetElimination) {
            throw new IllegalStateException("Changing this pool result will invalidate the Direct Elimination bracket and its results.");
        }
        updatePool(poolId, pool -> pool.replaceBoutResult(boutId, score, settings.poolBoutScoreLimit()), true);
        if (resetElimination) eliminationBracket = null;
    }

    public EliminationBracket eliminationBracket() {
        return eliminationBracket;
    }

    public void installEliminationBracket(EliminationBracket bracket) {
        if (bracket == null || eliminationBracket != null) {
            throw new IllegalArgumentException("A new elimination bracket is required.");
        }
        if (pools.isEmpty() || pools.stream().anyMatch(pool -> !pool.isComplete())) {
            throw new IllegalStateException("All pool bouts must be completed before direct elimination.");
        }
        eliminationBracket = bracket;
    }

    public void recordEliminationBoutResult(UUID matchId, BoutScore score) {
        if (eliminationBracket == null) throw new IllegalStateException("Generate the elimination bracket first.");
        eliminationBracket = eliminationBracket.recordResult(matchId, score, settings.eliminationBoutScoreLimit());
    }

    public boolean eliminationEditNeedsReset(UUID matchId) {
        if (eliminationBracket == null) throw new IllegalStateException("Generate the elimination bracket first.");
        return eliminationBracket.hasCompletedDescendant(matchId);
    }

    public boolean eliminationEditNeedsReset(UUID matchId, BoutScore score) {
        if (eliminationBracket == null) throw new IllegalStateException("Generate the elimination bracket first.");
        return eliminationBracket.changingWinnerWouldInvalidateCompletedDescendant(matchId, score, settings.eliminationBoutScoreLimit());
    }

    public void replaceEliminationBoutResult(UUID matchId, BoutScore score, boolean resetDownstream) {
        if (eliminationBracket == null) throw new IllegalStateException("Generate the elimination bracket first.");
        eliminationBracket = eliminationBracket.replaceResult(matchId, score, settings.eliminationBoutScoreLimit(), resetDownstream);
    }

    private void updatePool(UUID poolId, java.util.function.UnaryOperator<Pool> update) { updatePool(poolId, update, false); }
    private void updatePool(UUID poolId, java.util.function.UnaryOperator<Pool> update, boolean allowEliminationReset) {
        if (pools.isEmpty()) {
            throw new IllegalStateException("Pools must be generated before recording results.");
        }
        if (eliminationBracket != null && !allowEliminationReset) {
            throw new IllegalStateException("Pool results cannot be changed after elimination begins.");
        }
        if (poolId == null) {
            throw new IllegalArgumentException("Pool ID must not be null.");
        }

        for (int index = 0; index < pools.size(); index++) {
            Pool pool = pools.get(index);
            if (pool.id().equals(poolId)) {
                pools.set(index, update.apply(pool));
                return;
            }
        }
        throw new IllegalArgumentException("Pool does not belong to this tournament.");
    }

    private void requireRegistrationPhase() {
        if (seeding != null || !pools.isEmpty() || eliminationBracket != null) {
            throw new IllegalStateException("The tournament is no longer in the registration phase.");
        }
    }

    private static UUID requireId(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Tournament ID must not be null.");
        }
        return id;
    }

    private static String requireName(String name, String fieldName) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return name.trim();
    }

    private static void validateRoster(List<Fencer> fencers) {
        Objects.requireNonNull(fencers, "Fencer roster must not be null.");
        Set<UUID> fencerIds = new HashSet<>();
        Set<String> fencerNames = new HashSet<>();
        for (Fencer fencer : fencers) {
            if (fencer == null) {
                throw new IllegalArgumentException("Fencer roster must not contain null entries.");
            }
            if (!fencerIds.add(fencer.id())) {
                throw new IllegalArgumentException("Fencer IDs must be unique within a tournament.");
            }
            if (!fencerNames.add(fencer.name())) {
                throw new IllegalArgumentException("Fencer names must be unique within a tournament.");
            }
        }
    }

    private static void validatePoolResults(List<Pool> pools, int scoreLimit) {
        for (Pool pool : pools) {
            if (pool == null) {
                throw new IllegalArgumentException("Tournament pools must not contain null entries.");
            }
            pool.bouts().stream()
                    .map(edu.nus.cs3227.fencingtournament.domain.pool.PoolBout::score)
                    .filter(Objects::nonNull)
                    .forEach(score -> {
                        if (score.firstScore() > scoreLimit || score.secondScore() > scoreLimit
                                || Math.max(score.firstScore(), score.secondScore()) != scoreLimit) {
                            throw new IllegalArgumentException(
                                    "Completed pool scores must use the configured winning limit.");
                        }
                    });
        }
    }
}
