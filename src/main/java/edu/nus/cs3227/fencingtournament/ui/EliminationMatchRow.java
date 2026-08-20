package edu.nus.cs3227.fencingtournament.ui;

import java.util.UUID;

/** Presentation data for a direct-elimination match card. */
public record EliminationMatchRow(UUID matchId, int round, int position,
                                  String first, String second, String firstScore, String secondScore,
                                  String status, boolean ready, boolean resolved,
                                  boolean firstWinner, boolean secondWinner) {
}
