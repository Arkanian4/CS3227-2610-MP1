package edu.nus.cs3227.fencingtournament.ui;

/** Presentation row for a pool standing. */
public record PoolStandingRow(String name, int rank, int bouts, int wins, double ratio,
                              int scored, int received, int indicator) {
}
