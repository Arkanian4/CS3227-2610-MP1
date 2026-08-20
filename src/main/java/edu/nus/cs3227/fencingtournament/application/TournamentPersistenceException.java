package edu.nus.cs3227.fencingtournament.application;

/** Indicates that an in-memory tournament change could not be written to local storage. */
public final class TournamentPersistenceException extends RuntimeException {
    public TournamentPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
