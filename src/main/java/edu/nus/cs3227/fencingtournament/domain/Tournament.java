package edu.nus.cs3227.fencingtournament.domain;

import edu.nus.cs3227.fencingtournament.domain.elimination.EliminationBracket;
import edu.nus.cs3227.fencingtournament.domain.pool.Pool;

import java.util.List;
import java.util.UUID;

/** Aggregate root for one locally managed tournament. Behaviour is added incrementally. */
public final class Tournament {
    private final UUID id;
    private final String name;
    private final TournamentSettings settings;
    private final List<Fencer> fencers;
    private final Seeding seeding;
    private final List<Pool> pools;
    private final EliminationBracket eliminationBracket;

    public Tournament(UUID id, String name, TournamentSettings settings, List<Fencer> fencers,
                      Seeding seeding, List<Pool> pools, EliminationBracket eliminationBracket) {
        this.id = id;
        this.name = name;
        this.settings = settings;
        this.fencers = List.copyOf(fencers);
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
        return fencers;
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
}

