package edu.nus.cs3227.fencingtournament.ui;

import java.util.UUID;

/** Presentation row for a scheduled pool bout. */
public record PoolBoutRow(UUID boutId, String firstName, String secondName,
                          String scoreText, String status, boolean completed) {
}
