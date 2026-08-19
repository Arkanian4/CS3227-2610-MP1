package edu.nus.cs3227.fencingtournament.domain;

import edu.nus.cs3227.fencingtournament.domain.elimination.EliminationBracket;
import edu.nus.cs3227.fencingtournament.domain.pool.Pool;

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
    private final Seeding seeding;
    private final List<Pool> pools;
    private final EliminationBracket eliminationBracket;

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
        this.pools = List.copyOf(pools);
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

    public List<Pool> pools() {
        return pools;
    }

    public EliminationBracket eliminationBracket() {
        return eliminationBracket;
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
        for (Fencer fencer : fencers) {
            if (fencer == null) {
                throw new IllegalArgumentException("Fencer roster must not contain null entries.");
            }
            if (!fencerIds.add(fencer.id())) {
                throw new IllegalArgumentException("Fencer IDs must be unique within a tournament.");
            }
        }
    }
}
