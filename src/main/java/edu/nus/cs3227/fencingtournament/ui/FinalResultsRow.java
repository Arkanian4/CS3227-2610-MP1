package edu.nus.cs3227.fencingtournament.ui;

/** Read-only final-results row for the completed-tournament screen. */
public record FinalResultsRow(int place, String fencerName, int poolSeed, int poolWins,
                              int poolMatches, int indicator, String directEliminationFinish) {
}
