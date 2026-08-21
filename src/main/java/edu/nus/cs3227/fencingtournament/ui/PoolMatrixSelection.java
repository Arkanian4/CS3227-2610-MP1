package edu.nus.cs3227.fencingtournament.ui;

import java.util.UUID;

/** A matrix cell selected by the organiser, including its source pool. */
public record PoolMatrixSelection(UUID poolId, UUID rowFencerId, UUID opponentFencerId) { }
