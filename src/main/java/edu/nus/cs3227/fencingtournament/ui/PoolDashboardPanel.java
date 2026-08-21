package edu.nus.cs3227.fencingtournament.ui;

import java.util.List;
import java.util.UUID;

/** Presentation data for one pool in the operational pools dashboard. */
public record PoolDashboardPanel(UUID poolId, String poolName, int fencerCount,
                                 int completedBouts, int totalBouts,
                                 List<PoolMatrixRow> matrixRows) {
    public PoolDashboardPanel {
        matrixRows = List.copyOf(matrixRows);
    }
}
