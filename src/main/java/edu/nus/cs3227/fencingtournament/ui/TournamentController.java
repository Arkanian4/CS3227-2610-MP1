package edu.nus.cs3227.fencingtournament.ui;

import edu.nus.cs3227.fencingtournament.application.PoolProgress;
import edu.nus.cs3227.fencingtournament.application.TournamentService;
import edu.nus.cs3227.fencingtournament.domain.Fencer;
import edu.nus.cs3227.fencingtournament.domain.Tournament;
import edu.nus.cs3227.fencingtournament.domain.TournamentPhase;
import edu.nus.cs3227.fencingtournament.domain.pool.BoutScore;
import edu.nus.cs3227.fencingtournament.domain.pool.Pool;
import edu.nus.cs3227.fencingtournament.domain.pool.PoolBout;
import edu.nus.cs3227.fencingtournament.domain.standings.OverallStanding;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Routes JavaFX events to the application service and renders returned state. */
public final class TournamentController {
    private final TournamentService service;
    private final TournamentView view;
    private Path currentFile;
    private PoolBoutRow selectedBout;

    public TournamentController(TournamentService service, TournamentView view) {
        this.service = service;
        this.view = view;
        wireActions();
        refreshWorkspace();
    }

    private void wireActions() {
        view.createButton().setOnAction(event -> createTournament());
        view.loadButton().setOnAction(event -> loadTournament());
        view.saveButton().setOnAction(event -> saveTournament());
        view.addFencerButton().setOnAction(event -> addFencer());
        view.removeFencerButton().setOnAction(event -> removeFencer());
        view.moveSeedUpButton().setOnAction(event -> moveSeed(-1));
        view.moveSeedDownButton().setOnAction(event -> moveSeed(1));
        view.confirmSeedingButton().setOnAction(event -> applySeeding());
        view.applySeedingButton().setOnAction(event -> applySeeding());
        view.generatePoolsButton().setOnAction(event -> generatePools());
        view.poolList().getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> renderSelectedPool(newValue));
        view.setMatrixCellHandler((rowId, opponentId) -> {
            view.markSelectedMatrixCell(rowId, opponentId);
            Pool selectedPool = view.poolList().getSelectionModel().getSelectedItem();
            if (selectedPool == null) return;
            selectedPool.bouts().stream().map(this::boutRow)
                    .filter(bout -> samePair(bout, rowId, opponentId))
                    .findFirst()
                    .ifPresent(this::selectBout);
        });
        view.recordResultButton().setOnAction(event -> recordResult());
    }

    private void createTournament() {
        try {
            service.createTournament(view.tournamentNameField().getText());
            currentFile = null;
            view.tournamentNameField().clear();
            refreshWorkspace();
            view.showStatus("Tournament created. Register fencers, then apply seeding.");
        } catch (IllegalArgumentException exception) {
            showError(exception);
        }
    }

    private void loadTournament() {
        var selected = jsonFileChooser("Open tournament").showOpenDialog(view.getScene().getWindow());
        if (selected == null) return;
        try {
            Optional<Tournament> loaded = service.loadTournament(selected.toPath());
            if (loaded.isEmpty()) {
                view.showStatus("The selected tournament file does not exist.");
                return;
            }
            currentFile = selected.toPath();
            refreshWorkspace();
            view.showStatus("Tournament loaded.");
        } catch (IOException | IllegalArgumentException exception) {
            showError("Could not load tournament: " + exception.getMessage());
        }
    }

    private void saveTournament() {
        Path path = currentFile;
        if (path == null) {
            var selected = jsonFileChooser("Save tournament").showSaveDialog(view.getScene().getWindow());
            if (selected == null) return;
            path = selected.toPath();
        }
        try {
            service.saveTournament(path);
            currentFile = path;
            view.showStatus("Tournament saved.");
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            showError("Could not save tournament: " + exception.getMessage());
        }
    }

    private void addFencer() {
        try {
            service.addFencer(view.fencerNameField().getText());
            view.fencerNameField().clear();
            refreshWorkspace();
            view.showStatus("Fencer added.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showError(exception);
        }
    }

    private void removeFencer() {
        Fencer selected = view.fencerList().getSelectionModel().getSelectedItem();
        if (selected == null) {
            view.showStatus("Select a fencer to remove.");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove " + selected.name() + " from this tournament?", ButtonType.CANCEL, ButtonType.OK);
        confirmation.setTitle("Remove fencer");
        confirmation.setHeaderText(null);
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            service.removeFencer(selected.id());
            refreshWorkspace();
            view.showStatus("Fencer removed.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showError(exception);
        }
    }

    private void moveSeed(int direction) {
        int selected = view.seedList().getSelectionModel().getSelectedIndex();
        int destination = selected + direction;
        if (selected < 0 || destination < 0 || destination >= view.seedList().getItems().size()) return;
        var items = view.seedList().getItems();
        Fencer fencer = items.remove(selected);
        items.add(destination, fencer);
        view.seedList().getSelectionModel().select(destination);
    }

    private void applySeeding() {
        try {
            service.seedFencers(view.seedList().getItems().stream().map(Fencer::id).toList());
            refreshWorkspace();
            view.showStatus("Seeding applied. Generate pools when ready.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showError(exception);
        }
    }

    private void generatePools() {
        try {
            service.generatePools();
            refreshWorkspace();
            view.tabs().getSelectionModel().select(view.poolsTab());
            view.showStatus("Pools generated. Select a pool to record results.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showError(exception);
        }
    }

    private void renderSelectedPool(Pool pool) {
        if (pool == null) return;
        List<String> members = pool.memberIds().stream().map(this::fencerName).toList();
        List<PoolBoutRow> bouts = pool.bouts().stream().map(this::boutRow).toList();
        view.renderSelectedPool(pool.name(), members, matrixRows(pool));
        selectedBout = selectedBout == null ? null : bouts.stream()
                .filter(bout -> bout.boutId().equals(selectedBout.boutId()))
                .findFirst().orElse(null);
        view.showSelectedBout(selectedBout);
    }

    private void recordResult() {
        PoolBoutRow row = selectedBout;
        Pool pool = view.poolList().getSelectionModel().getSelectedItem();
        if (row == null || pool == null) {
            view.showStatus("Select a pending bout first.");
            return;
        }
        try {
            int first = Integer.parseInt(view.firstScoreField().getText().trim());
            int second = Integer.parseInt(view.secondScoreField().getText().trim());
            service.recordPoolBoutResult(pool.id(), row.boutId(), new BoutScore(first, second));
            refreshWorkspace();
            view.showStatus("Result recorded.");
        } catch (NumberFormatException exception) {
            view.showStatus("Scores must be whole numbers.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showError(exception);
        }
    }

    private void selectBout(PoolBoutRow bout) {
        selectedBout = bout;
        view.showSelectedBout(bout);
    }

    private void refreshWorkspace() {
        Optional<Tournament> current = service.currentTournament();
        if (current.isEmpty()) {
            view.setNoTournamentState();
            return;
        }
        Tournament tournament = current.orElseThrow();
        TournamentPhase phase = service.currentPhase();
        PoolProgress progress = phase == TournamentPhase.POOL_PHASE ? service.poolProgress() : null;
        view.showTournamentName(tournament.name());
        view.showPhase(phase, progress);
        List<Fencer> fencers = tournament.fencers();
        List<Fencer> seedOrder = tournament.seeding() == null ? fencers : tournament.seeding().fencerIds().stream()
                .map(tournament::findFencer).flatMap(Optional::stream).toList();
        view.renderFencers(fencers, seedOrder);
        boolean poolResultsFinalized = !tournament.pools().isEmpty()
                && tournament.pools().stream().allMatch(Pool::isComplete);
        if (!tournament.pools().isEmpty()) {
            view.renderPools(tournament.pools());
            renderSelectedPool(view.poolList().getSelectionModel().getSelectedItem());
            if (poolResultsFinalized) {
                List<OverallSeedingRow> rows = service.overallStandings().stream()
                        .map(this::overallSeedingRow).toList();
                view.renderOverallSeeding(rows);
            } else {
                view.renderStandings(List.of(), false);
            }
        }
        view.setPhaseControls(phase, true, poolResultsFinalized);
    }

    private PoolBoutRow boutRow(PoolBout bout) {
        String score = bout.score() == null ? "—" : bout.score().firstScore() + " - " + bout.score().secondScore();
        return new PoolBoutRow(bout.id(), bout.firstFencerId(), bout.secondFencerId(),
                fencerName(bout.firstFencerId()), fencerName(bout.secondFencerId()),
                score, bout.score() == null ? "Pending" : "Completed", bout.score() != null);
    }

    private List<PoolMatrixRow> matrixRows(Pool pool) {
        return pool.memberIds().stream().map(rowFencerId -> {
            java.util.Map<UUID, String> cells = new java.util.LinkedHashMap<>();
            for (UUID columnFencerId : pool.memberIds()) {
                if (rowFencerId.equals(columnFencerId)) {
                    cells.put(columnFencerId, "—");
                    continue;
                }
                PoolBout bout = pool.bouts().stream()
                        .filter(candidate -> samePair(candidate, rowFencerId, columnFencerId))
                        .findFirst().orElse(null);
                if (bout == null || bout.score() == null) {
                    cells.put(columnFencerId, "");
                } else if (bout.firstFencerId().equals(rowFencerId)) {
                    cells.put(columnFencerId, Integer.toString(bout.score().firstScore()));
                } else {
                    cells.put(columnFencerId, Integer.toString(bout.score().secondScore()));
                }
            }
            return new PoolMatrixRow(rowFencerId, fencerName(rowFencerId), cells);
        }).toList();
    }

    private static boolean samePair(PoolBout bout, UUID first, UUID second) {
        return (bout.firstFencerId().equals(first) && bout.secondFencerId().equals(second))
                || (bout.firstFencerId().equals(second) && bout.secondFencerId().equals(first));
    }

    private static boolean samePair(PoolBoutRow bout, UUID first, UUID second) {
        return (bout.firstId().equals(first) && bout.secondId().equals(second))
                || (bout.firstId().equals(second) && bout.secondId().equals(first));
    }

    private OverallSeedingRow overallSeedingRow(OverallStanding standing) {
        return new OverallSeedingRow(fencerName(standing.fencerId()), standing.rank(), standing.victories(),
                standing.boutsFenced(), standing.victoryRatio(), standing.touchesScored(),
                standing.touchesReceived(), standing.indicator(), standing.seed());
    }

    private String fencerName(UUID id) {
        return service.currentTournament().flatMap(tournament -> tournament.findFencer(id))
                .map(Fencer::name).orElse("Unknown fencer");
    }

    private static FileChooser jsonFileChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tournament JSON (*.json)", "*.json"));
        return chooser;
    }

    private void showError(Exception exception) { showError(exception.getMessage()); }
    private void showError(String message) { view.showStatus(message == null ? "Operation failed." : message); }
}
