package edu.nus.cs3227.fencingtournament.application;

/** Read-only progress summary for the pool phase. */
public record PoolProgress(int completedBouts, int totalBouts) {
    public boolean isComplete() {
        return totalBouts > 0 && completedBouts == totalBouts;
    }
}
