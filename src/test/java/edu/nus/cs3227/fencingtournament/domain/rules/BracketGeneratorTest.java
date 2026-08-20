package edu.nus.cs3227.fencingtournament.domain.rules;

import edu.nus.cs3227.fencingtournament.domain.elimination.EliminationBracket;
import edu.nus.cs3227.fencingtournament.domain.elimination.EliminationMatch;
import edu.nus.cs3227.fencingtournament.domain.pool.BoutScore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BracketGeneratorTest {
    private final BracketGenerator generator = new BracketGenerator();

    @Test
    void tenFencersUseSixteenPlaceBracketWithTopSeedByes() {
        List<UUID> fencers = ids(10);
        EliminationBracket bracket = generator.generate(fencers);
        List<EliminationMatch> firstRound = bracket.matches().stream().filter(match -> match.round() == 1).toList();

        assertEquals(16, bracket.size());
        assertEquals(10, firstRound.stream().flatMap(match -> java.util.stream.Stream.of(
                match.firstSlot().fencerId(), match.secondSlot().fencerId())).filter(java.util.Objects::nonNull).distinct().count());
        assertEquals(6, firstRound.stream().filter(EliminationMatch::isBye).count());
        assertTrue(firstRound.stream().filter(EliminationMatch::isBye)
                .allMatch(match -> match.winnerId().equals(fencers.get(0)) || match.winnerId().equals(fencers.get(1))
                        || match.winnerId().equals(fencers.get(2)) || match.winnerId().equals(fencers.get(3))
                        || match.winnerId().equals(fencers.get(4)) || match.winnerId().equals(fencers.get(5))));
    }

    @Test
    void seedOneAndTwoStartInOppositeHalvesAndGenerationIsDeterministic() {
        List<UUID> fencers = ids(8);
        EliminationBracket first = generator.generate(fencers);
        EliminationBracket second = generator.generate(fencers);
        List<EliminationMatch> opening = first.matches().stream().filter(match -> match.round() == 1).toList();
        int seedOneMatch = matchPosition(opening, fencers.get(0));
        int seedTwoMatch = matchPosition(opening, fencers.get(1));

        assertEquals(first, second);
        assertTrue(seedOneMatch < 2 && seedTwoMatch >= 2);
    }

    @Test
    void recordedResultAdvancesWinnerAndCompletesTwoFencerBracket() {
        List<UUID> fencers = ids(2);
        EliminationBracket bracket = generator.generate(fencers);
        EliminationMatch opening = bracket.matches().getFirst();
        EliminationBracket completed = bracket.recordResult(opening.id(), new BoutScore(15, 8), 15);

        assertTrue(completed.isComplete());
        assertEquals(fencers.get(0), completed.matches().getFirst().winnerId());
    }

    private static int matchPosition(List<EliminationMatch> matches, UUID fencerId) {
        return matches.stream().filter(match -> fencerId.equals(match.firstSlot().fencerId())
                || fencerId.equals(match.secondSlot().fencerId())).findFirst().orElseThrow().position();
    }

    private static List<UUID> ids(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> UUID.nameUUIDFromBytes(("de-" + index).getBytes())).toList();
    }
}
