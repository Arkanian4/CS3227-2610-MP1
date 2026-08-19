package edu.nus.cs3227.fencingtournament.domain.elimination;

import java.util.List;
import java.util.UUID;

/** Fixed bracket topology plus its current match state. */
public record EliminationBracket(UUID id, int size, List<EliminationMatch> matches) {
    public EliminationBracket {
        matches = List.copyOf(matches);
    }
}

