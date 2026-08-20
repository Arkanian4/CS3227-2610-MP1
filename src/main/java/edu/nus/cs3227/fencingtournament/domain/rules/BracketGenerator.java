package edu.nus.cs3227.fencingtournament.domain.rules;

import edu.nus.cs3227.fencingtournament.domain.elimination.BracketSlot;
import edu.nus.cs3227.fencingtournament.domain.elimination.EliminationBracket;
import edu.nus.cs3227.fencingtournament.domain.elimination.EliminationMatch;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Generates a deterministic standard seeded direct-elimination bracket. */
public final class BracketGenerator {
    public EliminationBracket generate(List<UUID> qualifiedFencerIds) {
        if (qualifiedFencerIds == null || qualifiedFencerIds.size() < 2 || qualifiedFencerIds.stream().anyMatch(id -> id == null)
                || qualifiedFencerIds.stream().distinct().count() != qualifiedFencerIds.size()) throw new IllegalArgumentException("At least two unique qualified fencers are required.");
        int size = nextPowerOfTwo(qualifiedFencerIds.size()); int rounds = Integer.numberOfTrailingZeros(size); List<Integer> line = seedLine(size); List<EliminationMatch> matches = new ArrayList<>();
        for (int round = 1; round <= rounds; round++) for (int position = 0; position < (size >> round); position++) {
            UUID next = round == rounds ? null : matchId(size, round + 1, position / 2); Integer nextSlot = round == rounds ? null : position % 2;
            BracketSlot first = round == 1 ? BracketSlot.initial(fencerForSeed(line.get(position * 2), qualifiedFencerIds)) : BracketSlot.pending();
            BracketSlot second = round == 1 ? BracketSlot.initial(fencerForSeed(line.get(position * 2 + 1), qualifiedFencerIds)) : BracketSlot.pending();
            matches.add(new EliminationMatch(matchId(size, round, position), round, position, first, second, null, null, next, nextSlot));
        }
        return new EliminationBracket(UUID.nameUUIDFromBytes(("de:" + qualifiedFencerIds).getBytes(StandardCharsets.UTF_8)), size, matches).resolveByes();
    }
    private static UUID fencerForSeed(int seed, List<UUID> fencers) { return seed <= fencers.size() ? fencers.get(seed - 1) : null; }
    private static int nextPowerOfTwo(int value) { int size = 1; while (size < value) size <<= 1; return size; }
    private static UUID matchId(int size, int round, int position) { return UUID.nameUUIDFromBytes(("de:" + size + ':' + round + ':' + position).getBytes(StandardCharsets.UTF_8)); }
    private static List<Integer> seedLine(int size) { List<Integer> seeds = new ArrayList<>(List.of(1)); for (int next = 2; next <= size; next *= 2) { List<Integer> expanded = new ArrayList<>(next); for (int seed : seeds) { expanded.add(seed); expanded.add(next + 1 - seed); } seeds = expanded; } return seeds; }
}
