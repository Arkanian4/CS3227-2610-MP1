package edu.nus.cs3227.fencingtournament.ui;

import java.util.Map;
import java.util.UUID;

/** One fencer row in the pool matrix. Cell values are already oriented for this row fencer. */
public record PoolMatrixRow(UUID fencerId, String fencerName, Map<UUID, String> cells) {
    public String cell(UUID opponentId) {
        return cells.getOrDefault(opponentId, "");
    }
}
