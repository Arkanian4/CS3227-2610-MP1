package edu.nus.cs3227.fencingtournament.domain.pool;

/** Scores for a completed, non-tied bout. Validation is added with bout result behaviour. */
public record BoutScore(int firstScore, int secondScore) {
    public BoutScore {
        if (firstScore < 0 || secondScore < 0) {
            throw new IllegalArgumentException("Bout scores must not be negative.");
        }
        if (firstScore == secondScore) {
            throw new IllegalArgumentException("A completed bout cannot be tied.");
        }
    }

    public boolean firstFencerWon() {
        return firstScore > secondScore;
    }
}

