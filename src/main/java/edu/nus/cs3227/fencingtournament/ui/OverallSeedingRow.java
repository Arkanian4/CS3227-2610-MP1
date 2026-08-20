package edu.nus.cs3227.fencingtournament.ui;

/** Presentation row for the finalized tournament-wide pool seeding. */
public record OverallSeedingRow(String name, int rank, int wins, int matches,
                                double ratio, int touchesScored, int touchesReceived,
                                int indicator, int originalSeed) {
}
