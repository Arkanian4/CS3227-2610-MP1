package edu.nus.cs3227.fencingtournament.ui;

/** One displayed side of a DE bout, including the distinction between bye and future winner. */
public record EliminationParticipant(int seed, String name, String score,
                                    boolean winner, boolean bye, boolean unresolved) {
}
