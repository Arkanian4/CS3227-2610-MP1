package edu.nus.cs3227.fencingtournament.domain.rules;

import edu.nus.cs3227.fencingtournament.domain.elimination.EliminationBracket;
import edu.nus.cs3227.fencingtournament.domain.elimination.EliminationMatch;
import edu.nus.cs3227.fencingtournament.domain.pool.BoutScore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void editingSemiFinalReplaysThePendingFinalWithTheCorrectedWinner() {
        List<UUID> fencers = ids(4);
        EliminationBracket bracket = generator.generate(fencers);
        List<EliminationMatch> opening = bracket.matches().stream().filter(match -> match.round() == 1)
                .sorted(java.util.Comparator.comparingInt(EliminationMatch::position)).toList();
        bracket = bracket.recordResult(opening.get(0).id(), new BoutScore(15, 4), 15);
        bracket = bracket.recordResult(opening.get(1).id(), new BoutScore(15, 7), 15);

        EliminationBracket corrected = bracket.replaceResult(opening.get(0).id(), new BoutScore(3, 15), 15, false);
        EliminationMatch finalMatch = corrected.matches().stream().filter(match -> match.round() == 2).findFirst().orElseThrow();

        assertTrue(finalMatch.isReady());
        assertEquals(opening.get(0).secondSlot().fencerId(), finalMatch.firstSlot().fencerId());
    }

    @Test
    void editingScoreWithoutChangingWinnerPreservesCompletedFinal() {
        EliminationBracket bracket = completedFourFencerBracket();
        EliminationMatch semiFinal = openingMatches(bracket).getFirst();
        EliminationMatch finalMatch = finalMatch(bracket);

        EliminationBracket corrected = bracket.replaceResult(semiFinal.id(), new BoutScore(15, 12), 15, false);

        assertEquals(new BoutScore(15, 12), match(corrected, semiFinal.id()).score());
        assertEquals(finalMatch.score(), finalMatch(corrected).score());
        assertTrue(corrected.isComplete());
    }

    @Test
    void changedWinnerAfterCompletedFinalRequiresResetAndClearsOnlyDependentFinal() {
        EliminationBracket bracket = completedFourFencerBracket();
        EliminationMatch firstSemiFinal = openingMatches(bracket).getFirst();
        EliminationMatch secondSemiFinal = openingMatches(bracket).get(1);

        assertThrows(IllegalStateException.class,
                () -> bracket.replaceResult(firstSemiFinal.id(), new BoutScore(6, 15), 15, false));
        EliminationBracket corrected = bracket.replaceResult(firstSemiFinal.id(), new BoutScore(6, 15), 15, true);

        assertEquals(new BoutScore(6, 15), match(corrected, firstSemiFinal.id()).score());
        assertEquals(match(bracket, secondSemiFinal.id()).score(), match(corrected, secondSemiFinal.id()).score());
        assertFalse(finalMatch(corrected).isResolved());
        assertFalse(corrected.isComplete());
    }

    @Test
    void editingFinalToDifferentWinnerKeepsBracketComplete() {
        EliminationBracket bracket = completedFourFencerBracket();
        EliminationMatch finalMatch = finalMatch(bracket);

        EliminationBracket corrected = bracket.replaceResult(finalMatch.id(), new BoutScore(7, 15), 15, false);

        assertTrue(corrected.isComplete());
        assertEquals(finalMatch.secondSlot().fencerId(), finalMatch(corrected).winnerId());
    }

    @Test
    void invalidEditedScoreIsRejectedWithoutChangingBracket() {
        EliminationBracket bracket = completedFourFencerBracket();
        EliminationMatch finalMatch = finalMatch(bracket);

        assertThrows(IllegalArgumentException.class,
                () -> bracket.replaceResult(finalMatch.id(), new BoutScore(14, 10), 15, false));

        assertEquals(new BoutScore(15, 11), finalMatch(bracket).score());
        assertTrue(bracket.isComplete());
    }

    private EliminationBracket completedFourFencerBracket() {
        EliminationBracket bracket = generator.generate(ids(4));
        for (EliminationMatch semiFinal : openingMatches(bracket)) bracket = bracket.recordResult(semiFinal.id(), new BoutScore(15, 8), 15);
        return bracket.recordResult(finalMatch(bracket).id(), new BoutScore(15, 11), 15);
    }

    private static List<EliminationMatch> openingMatches(EliminationBracket bracket) {
        return bracket.matches().stream().filter(match -> match.round() == 1)
                .sorted(java.util.Comparator.comparingInt(EliminationMatch::position)).toList();
    }

    private static EliminationMatch finalMatch(EliminationBracket bracket) {
        return bracket.matches().stream().filter(match -> match.nextMatchId() == null).findFirst().orElseThrow();
    }

    private static EliminationMatch match(EliminationBracket bracket, UUID matchId) {
        return bracket.matches().stream().filter(match -> match.id().equals(matchId)).findFirst().orElseThrow();
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
