package edu.nus.cs3227.fencingtournament.application;

import java.nio.file.Path;
import java.util.List;

/** Per-file outcomes from a non-recursive folder import. */
public record TournamentFolderImportResult(List<Item> imported, List<Item> skipped, List<Item> rejected) {
    public TournamentFolderImportResult {
        imported = List.copyOf(imported);
        skipped = List.copyOf(skipped);
        rejected = List.copyOf(rejected);
    }

    /** A concise, user-facing outcome for one source file. */
    public record Item(Path file, String tournamentName, String detail) {
        public Item {
            if (file == null) throw new IllegalArgumentException("Import file must not be null.");
            tournamentName = tournamentName == null ? "" : tournamentName;
            detail = detail == null ? "" : detail;
        }

        public String displayName() {
            return tournamentName.isBlank() ? file.getFileName().toString() : tournamentName;
        }
    }
}
