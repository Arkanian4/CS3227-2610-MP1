package edu.nus.cs3227.fencingtournament.domain.standings;

import java.util.List;

/** Ordered criteria applied when calculating pool and overall standings. */
public record TieBreakPolicy(List<TieBreakCriterion> criteria) {
    public TieBreakPolicy {
        criteria = List.copyOf(criteria);
    }
}
