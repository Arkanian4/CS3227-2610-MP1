package edu.nus.cs3227.fencingtournament.application;

import edu.nus.cs3227.fencingtournament.domain.Tournament;
import edu.nus.cs3227.fencingtournament.domain.elimination.EliminationBracket;
import edu.nus.cs3227.fencingtournament.domain.elimination.EliminationMatch;
import edu.nus.cs3227.fencingtournament.domain.pool.Pool;
import edu.nus.cs3227.fencingtournament.domain.pool.PoolBout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Verifies relationships that span persisted tournament value objects before import. */
final class TournamentImportValidator {
    private TournamentImportValidator() { }

    static void validate(Tournament tournament) {
        Set<UUID> rosterIds = tournament.fencers().stream().map(fencer -> fencer.id()).collect(java.util.stream.Collectors.toSet());
        if (tournament.seeding() == null || tournament.seeding().fencerIds().size() != rosterIds.size()
                || !new HashSet<>(tournament.seeding().fencerIds()).equals(rosterIds)) {
            throw new IllegalArgumentException("Seed order must contain every registered fencer exactly once.");
        }

        validatePools(tournament, rosterIds);
        validateBracket(tournament, rosterIds);
    }

    private static void validatePools(Tournament tournament, Set<UUID> rosterIds) {
        Set<UUID> assignedFencers = new HashSet<>();
        Set<UUID> poolIds = new HashSet<>();
        for (Pool pool : tournament.pools()) {
            if (pool.id() == null || !poolIds.add(pool.id()) || pool.name() == null || pool.name().isBlank()) {
                throw new IllegalArgumentException("Pool structure is invalid.");
            }
            Set<UUID> members = new HashSet<>(pool.memberIds());
            if (members.size() != pool.memberIds().size() || !rosterIds.containsAll(members)
                    || !assignedFencers.addAll(members)) {
                throw new IllegalArgumentException("Pool assignments must contain each registered fencer exactly once.");
            }
            Set<UUID> boutIds = new HashSet<>();
            Set<String> pairings = new HashSet<>();
            for (PoolBout bout : pool.bouts()) {
                if (!boutIds.add(bout.id()) || !members.contains(bout.firstFencerId()) || !members.contains(bout.secondFencerId())) {
                    throw new IllegalArgumentException("Pool bout references an unknown fencer.");
                }
                String pairing = pairKey(bout.firstFencerId(), bout.secondFencerId());
                if (!pairings.add(pairing)) throw new IllegalArgumentException("A pool contains a duplicate bout.");
            }
            int expectedBouts = members.size() * (members.size() - 1) / 2;
            if (pool.bouts().size() != expectedBouts || pairings.size() != expectedBouts) {
                throw new IllegalArgumentException("Pool bout schedule is incomplete or inconsistent.");
            }
        }
        if (!tournament.pools().isEmpty() && !assignedFencers.equals(rosterIds)) {
            throw new IllegalArgumentException("Pool assignments must include every registered fencer.");
        }
    }

    private static void validateBracket(Tournament tournament, Set<UUID> rosterIds) {
        EliminationBracket bracket = tournament.eliminationBracket();
        if (bracket == null) return;
        if (tournament.pools().isEmpty() || tournament.pools().stream().anyMatch(pool -> !pool.isComplete())) {
            throw new IllegalArgumentException("Direct Elimination requires completed pools.");
        }

        Map<UUID, EliminationMatch> byId = new HashMap<>();
        for (EliminationMatch match : bracket.matches()) {
            if (match.id() == null || byId.put(match.id(), match) != null || match.round() < 1 || match.position() < 0) {
                throw new IllegalArgumentException("Direct Elimination bracket structure is invalid.");
            }
            validateSlot(match.firstSlot().fencerId(), rosterIds);
            validateSlot(match.secondSlot().fencerId(), rosterIds);
            if (match.nextMatchId() == null ? match.nextMatchSlot() != null : match.nextMatchSlot() == null
                    || match.nextMatchSlot() != null && (match.nextMatchSlot() < 0 || match.nextMatchSlot() > 1)) {
                throw new IllegalArgumentException("Direct Elimination bracket progression is invalid.");
            }
        }
        for (EliminationMatch match : bracket.matches()) {
            if (match.nextMatchId() != null && !byId.containsKey(match.nextMatchId())) {
                throw new IllegalArgumentException("Direct Elimination bracket references an unknown next bout.");
            }
            if (match.winnerId() != null) {
                if (!rosterIds.contains(match.winnerId()) || (!match.winnerId().equals(match.firstSlot().fencerId())
                        && !match.winnerId().equals(match.secondSlot().fencerId()))) {
                    throw new IllegalArgumentException("Direct Elimination winner is inconsistent with its bout participants.");
                }
                if (match.score() != null) {
                    if (!match.firstSlot().resolved() || !match.secondSlot().resolved()
                            || match.firstSlot().fencerId() == null || match.secondSlot().fencerId() == null
                            || match.score().firstScore() > tournament.settings().eliminationBoutScoreLimit()
                            || match.score().secondScore() > tournament.settings().eliminationBoutScoreLimit()) {
                        throw new IllegalArgumentException("Direct Elimination score is invalid.");
                    }
                    UUID scoreWinner = match.score().firstFencerWon() ? match.firstSlot().fencerId() : match.secondSlot().fencerId();
                    if (!match.winnerId().equals(scoreWinner)) {
                        throw new IllegalArgumentException("Direct Elimination winner does not match its score.");
                    }
                }
            } else if (match.score() != null) {
                throw new IllegalArgumentException("Direct Elimination score has no winner.");
            }
        }
    }

    private static void validateSlot(UUID id, Set<UUID> rosterIds) {
        if (id != null && !rosterIds.contains(id)) {
            throw new IllegalArgumentException("Direct Elimination bracket references an unknown fencer.");
        }
    }

    private static String pairKey(UUID first, UUID second) {
        return first.compareTo(second) < 0 ? first + ":" + second : second + ":" + first;
    }
}
