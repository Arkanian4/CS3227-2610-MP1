package edu.nus.cs3227.fencingtournament.ui;

import edu.nus.cs3227.fencingtournament.application.PoolProgress;
import edu.nus.cs3227.fencingtournament.application.TournamentService;
import edu.nus.cs3227.fencingtournament.application.TournamentPersistenceException;
import edu.nus.cs3227.fencingtournament.domain.Fencer;
import edu.nus.cs3227.fencingtournament.domain.Tournament;
import edu.nus.cs3227.fencingtournament.domain.TournamentPhase;
import edu.nus.cs3227.fencingtournament.domain.pool.BoutScore;
import edu.nus.cs3227.fencingtournament.domain.pool.Pool;
import edu.nus.cs3227.fencingtournament.domain.pool.PoolBout;
import edu.nus.cs3227.fencingtournament.domain.elimination.EliminationBracket;
import edu.nus.cs3227.fencingtournament.domain.elimination.EliminationMatch;
import edu.nus.cs3227.fencingtournament.domain.standings.OverallStanding;
import edu.nus.cs3227.fencingtournament.domain.standings.FinalStanding;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Routes JavaFX events to the application service and renders returned state. */
public final class TournamentController {
    private final TournamentService service;
    private final TournamentView view;
    private PoolBoutRow selectedBout;
    private EliminationMatchRow selectedEliminationMatch;
    private boolean selectedPoolRowIsScheduledFirst = true;
    private boolean editingPoolResult;
    private boolean editingEliminationResult;

    public TournamentController(TournamentService service, TournamentView view) {
        this.service = service;
        this.view = view;
        wireActions();
        refreshWorkspace();
    }

    private void wireActions() {
        view.createButton().setOnAction(event -> createTournament());
        view.homeCreateButton().setOnAction(event -> createTournamentFromHome());
        view.homeNewButton().setOnAction(event -> view.showNewTournamentForm(true));
        view.homeCancelButton().setOnAction(event -> { view.homeTournamentNameField().clear(); view.showNewTournamentForm(false); });
        view.setTournamentOpenHandler(this::openTournament);
        view.setTournamentDeleteHandler(this::deleteTournament);
        view.homeButton().setOnAction(event -> { service.returnToTournamentHome(); refreshWorkspace(); });
        view.loadButton().setOnAction(event -> loadTournament());
        view.addFencerButton().setOnAction(event -> addFencer());
        view.removeFencerButton().setOnAction(event -> removeFencer());
        view.moveSeedUpButton().setOnAction(event -> moveSeed(-1));
        view.moveSeedDownButton().setOnAction(event -> moveSeed(1));
        view.setSeedMoveHandler(this::moveSeedFencer);
        view.confirmSeedingButton().setOnAction(event -> applySeeding());
        view.generatePoolsButton().setOnAction(event -> generatePools());
        view.generateEliminationButton().setOnAction(event -> generateEliminationBracket());
        view.poolList().getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> renderSelectedPool(newValue));
        view.setMatrixCellHandler(selection -> {
            Pool selectedPool = service.pools().stream()
                    .filter(pool -> pool.id().equals(selection.poolId()))
                    .findFirst().orElse(null);
            if (selectedPool == null) return;
            view.poolList().getSelectionModel().select(selectedPool);
            view.markSelectedMatrixCell(selection.poolId(), selection.rowFencerId(), selection.opponentFencerId());
            selectedPool.bouts().stream().filter(bout -> samePair(bout, selection.rowFencerId(), selection.opponentFencerId())).findFirst()
                    .ifPresent(bout -> selectBout(orientedBoutRow(bout, selection.rowFencerId()),
                            bout.firstFencerId().equals(selection.rowFencerId())));
        });
        view.setPoolSelectionDismissHandler(this::dismissSelectedPoolBout);
        view.recordResultButton().setOnAction(event -> recordResult());
        view.editPoolResultButton().setOnAction(event -> beginPoolResultEdit());
        view.setEliminationMatchHandler(this::selectEliminationMatch);
        view.recordEliminationResultButton().setOnAction(event -> recordEliminationResult());
        view.editEliminationResultButton().setOnAction(event -> beginEliminationResultEdit());
        view.cancelEliminationEditButton().setOnAction(event -> cancelEliminationResultEdit());
    }

    private void createTournament() {
        try {
            service.createTournament(view.tournamentNameField().getText());
            view.tournamentNameField().clear();
            refreshWorkspace();
            view.showStatus("Tournament created. Register fencers, then apply seeding.");
        } catch (IllegalArgumentException | TournamentPersistenceException exception) {
            showError(exception);
        }
    }

    private void createTournamentFromHome() {
        try {
            service.createTournament(view.homeTournamentNameField().getText());
            view.homeTournamentNameField().clear();
            view.showNewTournamentForm(false);
            refreshWorkspace();
            view.showStatus("Tournament created.");
        } catch (IllegalArgumentException | TournamentPersistenceException exception) { showError(exception); }
    }

    private void openTournament(UUID tournamentId) {
        try { service.openTournament(tournamentId); refreshWorkspace(); view.showStatus("Tournament opened."); }
        catch (IllegalArgumentException | TournamentPersistenceException exception) { showError(exception); }
    }

    private void deleteTournament(UUID tournamentId) {
        Tournament tournament = service.listTournaments().stream()
                .filter(candidate -> candidate.id().equals(tournamentId)).findFirst().orElse(null);
        if (tournament == null) {
            refreshWorkspace();
            view.showStatus("That tournament no longer exists.");
            return;
        }
        if (!confirmTournamentDeletion(tournament.name())) return;
        try {
            if (!service.deleteTournament(tournamentId)) {
                refreshWorkspace();
                view.showStatus("That tournament no longer exists.");
                return;
            }
            refreshWorkspace();
            view.showStatus("Tournament deleted.");
        } catch (TournamentPersistenceException exception) {
            refreshWorkspace();
            showError("Could not delete tournament: " + exception.getMessage());
        }
    }

    private boolean confirmTournamentDeletion(String tournamentName) {
        ButtonType delete = new ButtonType("Delete tournament", ButtonBar.ButtonData.OK_DONE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "This permanently removes the tournament and all of its fencers, pool results, Direct Elimination results, and final standings. This action cannot be undone.",
                ButtonType.CANCEL, delete);
        UiTheme.apply(alert.getDialogPane());
        alert.setTitle("Delete tournament");
        alert.setHeaderText("Delete \"" + tournamentName + "\"?");
        alert.getDialogPane().lookupButton(delete).getStyleClass().add("danger-action");
        return alert.showAndWait().orElse(ButtonType.CANCEL) == delete;
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
            refreshWorkspace();
            view.showStatus("Tournament loaded.");
        } catch (TournamentPersistenceException exception) {
            refreshWorkspace();
            showError("Could not load tournament: " + exception.getMessage());
        } catch (IOException | IllegalArgumentException exception) {
            showError("Could not load tournament: " + exception.getMessage());
        }
    }

    private void addFencer() {
        try {
            service.addFencer(view.fencerNameField().getText());
            view.fencerNameField().clear();
            refreshWorkspace();
            view.showStatus("Fencer added.");
        } catch (IllegalArgumentException | IllegalStateException | TournamentPersistenceException exception) {
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
        UiTheme.apply(confirmation.getDialogPane());
        confirmation.setTitle("Remove fencer");
        confirmation.setHeaderText(null);
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            service.removeFencer(selected.id());
            refreshWorkspace();
            view.showStatus("Fencer removed.");
        } catch (IllegalArgumentException | IllegalStateException | TournamentPersistenceException exception) {
            showError(exception);
        }
    }

    private void moveSeed(int direction) {
        int selected = view.seedList().getSelectionModel().getSelectedIndex();
        int destination = selected + direction;
        if (selected < 0 || destination < 0 || destination >= view.seedList().getItems().size()) return;
        moveSeedFencer(view.seedList().getItems().get(selected).id(), destination);
    }

    private void moveSeedFencer(UUID fencerId, int destination) {
        try {
            if (!service.moveSeedFencer(fencerId, destination)) return;
            refreshWorkspace();
            view.seedList().getSelectionModel().select(destination);
        } catch (IllegalArgumentException | IllegalStateException | TournamentPersistenceException exception) {
            showError(exception);
        }
    }

    private void applySeeding() {
        try {
            service.seedFencers(view.seedList().getItems().stream().map(Fencer::id).toList());
            refreshWorkspace();
            view.showStatus("Seeding applied. Generate pools when ready.");
        } catch (IllegalArgumentException | IllegalStateException | TournamentPersistenceException exception) {
            showError(exception);
        }
    }

    private void generatePools() {
        try {
            Integer maximumPoolSize = view.maximumPoolSizeChoice().getValue();
            if (maximumPoolSize == null) {
                view.showStatus("Choose the maximum number of fencers per pool first.");
                return;
            }
            service.generatePools(maximumPoolSize);
            refreshWorkspace();
            view.selectPoolsTab();
            view.showStatus("Pools generated. Select a pool to record results.");
        } catch (IllegalArgumentException | IllegalStateException | TournamentPersistenceException exception) {
            showError(exception);
        }
    }

    private void renderSelectedPool(Pool pool) {
        if (pool == null) return;
        List<String> members = pool.memberIds().stream().map(this::fencerName).toList();
        List<PoolBoutRow> bouts = pool.bouts().stream().map(this::boutRow).toList();
        int poolNumber = service.pools().indexOf(pool) + 1;
        view.renderSelectedPool("POOL #" + poolNumber, members, matrixRows(pool));
        UUID selectedBoutId = selectedBout == null ? null : selectedBout.boutId();
        UUID selectedRowFencerId = selectedBout == null ? null : selectedBout.firstId();
        selectedBout = selectedBoutId == null ? null : pool.bouts().stream()
                .filter(bout -> bout.id().equals(selectedBoutId))
                .findFirst()
                .map(bout -> orientedBoutRow(bout, selectedRowFencerId))
                .orElse(null);
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
            BoutScore score = selectedPoolRowIsScheduledFirst ? new BoutScore(first, second) : new BoutScore(second, first);
            if (editingPoolResult) {
                boolean resetElimination = service.poolEditNeedsReset();
                if (resetElimination && !confirmPoolReset()) return;
                service.replacePoolBoutResult(pool.id(), row.boutId(), score, resetElimination);
                editingPoolResult = false; view.endPoolResultEdit();
            } else service.recordPoolBoutResult(pool.id(), row.boutId(), score);
            refreshWorkspace();
            view.showStatus("Result saved.");
        } catch (NumberFormatException exception) {
            view.showStatus("Scores must be whole numbers.");
        } catch (IllegalArgumentException | IllegalStateException | TournamentPersistenceException exception) {
            showError(exception);
        }
    }

    private void beginPoolResultEdit() {
        if (selectedBout == null || !selectedBout.completed()) return;
        editingPoolResult = true;
        view.beginPoolResultEdit(selectedBout);
    }

    private void dismissSelectedPoolBout() {
        if (selectedBout == null && !editingPoolResult) return;
        selectedBout = null;
        selectedPoolRowIsScheduledFirst = true;
        editingPoolResult = false;
        view.endPoolResultEdit();
        view.clearSelectedMatrixCell();
        view.showSelectedBout(null);
    }

    private boolean confirmPoolReset() {
        ButtonType reset = new ButtonType("Edit and reset DE", ButtonBar.ButtonData.OK_DONE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Changing this pool result will invalidate the current Direct Elimination bracket, including all DE results. Continue?",
                ButtonType.CANCEL, reset);
        UiTheme.apply(alert.getDialogPane());
        alert.setTitle("Reset Direct Elimination"); alert.setHeaderText(null);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == reset;
    }

    private void generateEliminationBracket() {
        try {
            service.generateEliminationBracket();
            refreshWorkspace();
            view.selectEliminationTab();
            view.showStatus("Direct elimination bracket generated.");
        } catch (IllegalArgumentException | IllegalStateException | TournamentPersistenceException exception) { showError(exception); }
    }

    private void selectEliminationMatch(UUID matchId) {
        EliminationBracket bracket = service.currentTournament().map(Tournament::eliminationBracket).orElse(null);
        if (bracket == null) return;
        selectedEliminationMatch = eliminationRows(bracket).stream()
                .filter(match -> match.matchId().equals(matchId)).findFirst().orElse(null);
        view.showSelectedEliminationMatch(selectedEliminationMatch);
    }

    private void recordEliminationResult() {
        if (selectedEliminationMatch == null || (!selectedEliminationMatch.ready() && !editingEliminationResult)) {
            view.showStatus("Select a pending DE bout first."); return;
        }
        try {
            BoutScore score = new BoutScore(
                    Integer.parseInt(view.eliminationFirstScoreField().getText().trim()),
                    Integer.parseInt(view.eliminationSecondScoreField().getText().trim()));
            if (editingEliminationResult) {
                boolean resetDownstream = service.eliminationEditNeedsReset(selectedEliminationMatch.matchId(), score);
                if (resetDownstream && !confirmEliminationReset()) return;
                service.replaceEliminationBoutResult(selectedEliminationMatch.matchId(), score, resetDownstream);
                editingEliminationResult = false;
                view.endEliminationResultEdit();
            } else {
                service.recordEliminationBoutResult(selectedEliminationMatch.matchId(), score);
            }
            refreshWorkspace();
            if (service.currentPhase() == TournamentPhase.COMPLETE) {
                view.selectFinalResultsTab();
                view.showStatus("Tournament complete. Final results are ready.");
            } else {
                view.showStatus("DE result recorded.");
            }
        } catch (NumberFormatException exception) { view.showStatus("Scores must be whole numbers.");
        } catch (IllegalArgumentException | IllegalStateException | TournamentPersistenceException exception) { showError(exception); }
    }

    private void beginEliminationResultEdit() {
        if (selectedEliminationMatch == null || !selectedEliminationMatch.resolved() || selectedEliminationMatch.bye()) return;
        editingEliminationResult = true;
        view.beginEliminationResultEdit(selectedEliminationMatch);
    }

    private void cancelEliminationResultEdit() {
        editingEliminationResult = false;
        view.endEliminationResultEdit();
        view.showSelectedEliminationMatch(selectedEliminationMatch);
    }

    private boolean confirmEliminationReset() {
        ButtonType reset = new ButtonType("Edit and reset later results", ButtonBar.ButtonData.OK_DONE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Changing this result changes the advancing fencer and will invalidate later Direct Elimination results that depend on it. Continue?",
                ButtonType.CANCEL, reset);
        UiTheme.apply(alert.getDialogPane());
        alert.setTitle("Reset later Direct Elimination results");
        alert.setHeaderText(null);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == reset;
    }

    private void selectBout(PoolBoutRow bout) {
        selectedBout = bout;
        view.showSelectedBout(bout);
    }
    private void selectBout(PoolBoutRow bout, boolean rowIsScheduledFirst) {
        selectedPoolRowIsScheduledFirst = rowIsScheduledFirst;
        selectBout(bout);
    }

    private void refreshWorkspace() {
        Optional<Tournament> current = service.currentTournament();
        if (current.isEmpty()) {
            view.renderTournamentList(service.listTournaments());
            view.setNoTournamentState();
            return;
        }
        view.renderTournamentList(service.listTournaments());
        view.showWorkspace();
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
            view.renderPoolDashboard(tournament.pools().stream().map(this::poolDashboardPanel).toList());
            renderSelectedPool(view.poolList().getSelectionModel().getSelectedItem());
            if (poolResultsFinalized) {
                List<OverallSeedingRow> rows = service.overallStandings().stream()
                        .map(this::overallSeedingRow).toList();
                view.renderOverallSeeding(rows);
            } else {
                view.renderStandings(List.of(), false);
            }
        }
        else {
            selectedBout = null;
            view.clearPoolWorkspace();
        }
        if (tournament.eliminationBracket() != null) {
            EliminationBracket bracket = tournament.eliminationBracket();
            List<EliminationMatchRow> eliminationRows = eliminationRows(bracket);
            view.renderEliminationBracket(eliminationRows);
            selectedEliminationMatch = selectedEliminationMatch == null ? null : eliminationRows.stream()
                    .filter(match -> match.matchId().equals(selectedEliminationMatch.matchId())).findFirst().orElse(null);
            view.showSelectedEliminationMatch(selectedEliminationMatch);
        }
        if (phase == TournamentPhase.COMPLETE) {
            view.renderFinalResults(service.finalStandings().stream().map(this::finalResultsRow).toList());
        }
        view.setPhaseControls(phase, true, poolResultsFinalized, tournament.eliminationBracket() != null);
        view.selectTabForPhase(phase);
    }

    private PoolBoutRow boutRow(PoolBout bout) {
        String score = bout.score() == null ? "—" : bout.score().firstScore() + " - " + bout.score().secondScore();
        return new PoolBoutRow(bout.id(), bout.firstFencerId(), bout.secondFencerId(),
                fencerName(bout.firstFencerId()), fencerName(bout.secondFencerId()),
                score, bout.score() == null ? "Pending" : "Completed", bout.score() != null);
    }

    private PoolBoutRow orientedBoutRow(PoolBout bout, UUID rowFencerId) {
        if (bout.firstFencerId().equals(rowFencerId)) return boutRow(bout);
        String score = bout.score() == null ? "—" : bout.score().secondScore() + " - " + bout.score().firstScore();
        return new PoolBoutRow(bout.id(), bout.secondFencerId(), bout.firstFencerId(),
                fencerName(bout.secondFencerId()), fencerName(bout.firstFencerId()),
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

    private PoolDashboardPanel poolDashboardPanel(Pool pool) {
        int poolNumber = service.pools().indexOf(pool) + 1;
        int completed = (int) pool.bouts().stream().filter(bout -> bout.score() != null).count();
        return new PoolDashboardPanel(pool.id(), "POOL #" + poolNumber, pool.memberIds().size(),
                completed, pool.bouts().size(), matrixRows(pool));
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

    private FinalResultsRow finalResultsRow(FinalStanding standing) {
        return new FinalResultsRow(standing.place(), fencerName(standing.fencerId()), standing.poolSeed(),
                standing.poolVictories(), standing.poolBoutsFenced(), standing.indicator(),
                standing.directEliminationFinish());
    }

    private List<EliminationMatchRow> eliminationRows(EliminationBracket bracket) {
        java.util.Map<String, Integer> advancingDisplaySeeds = new java.util.HashMap<>();
        List<EliminationMatchRow> rows = new java.util.ArrayList<>();
        bracket.matches().stream().sorted(java.util.Comparator.comparingInt(EliminationMatch::round)
                .thenComparingInt(EliminationMatch::position)).forEach(match -> {
            int firstSeed = match.round() == 1 ? seedOf(match.firstSlot().fencerId())
                    : advancingDisplaySeeds.getOrDefault(match.id() + ":0", 0);
            int secondSeed = match.round() == 1 ? seedOf(match.secondSlot().fencerId())
                    : advancingDisplaySeeds.getOrDefault(match.id() + ":1", 0);
            rows.add(eliminationRow(match, firstSeed, secondSeed));
            if (match.isResolved() && match.nextMatchId() != null) {
                boolean firstWon = match.winnerId().equals(match.firstSlot().fencerId());
                int winnerSeed = firstWon ? firstSeed : secondSeed;
                int loserSeed = firstWon ? secondSeed : firstSeed;
                int advancingSeed = winnerSeed == 0 ? loserSeed : loserSeed == 0 ? winnerSeed : Math.min(winnerSeed, loserSeed);
                advancingDisplaySeeds.put(match.nextMatchId() + ":" + match.nextMatchSlot(), advancingSeed);
            }
        });
        return List.copyOf(rows);
    }

    private EliminationMatchRow eliminationRow(EliminationMatch match, int firstSeed, int secondSeed) {
        return new EliminationMatchRow(match.id(), match.round(), match.position(),
                eliminationParticipant(match, true, firstSeed), eliminationParticipant(match, false, secondSeed),
                match.isReady(), match.isResolved(), match.isBye());
    }

    private EliminationParticipant eliminationParticipant(EliminationMatch match, boolean first, int displaySeed) {
        var slot = first ? match.firstSlot() : match.secondSlot();
        UUID fencerId = slot.fencerId();
        if (fencerId == null) {
            String placeholder = slot.resolved() ? "BYE" : "Awaiting opponent";
            return new EliminationParticipant(0, placeholder, "", false, slot.resolved(), !slot.resolved());
        }
        String score = match.score() == null ? "" : Integer.toString(first
                ? match.score().firstScore() : match.score().secondScore());
        boolean winner = fencerId.equals(match.winnerId());
        String name = service.currentTournament().flatMap(tournament -> tournament.findFencer(fencerId))
                .map(Fencer::name).orElse("Unknown fencer");
        return new EliminationParticipant(displaySeed, name, score, winner, false, false);
    }

    private int seedOf(UUID fencerId) {
        List<OverallStanding> standings = service.overallStandings();
        return standings.stream().filter(standing -> standing.fencerId().equals(fencerId))
                .mapToInt(OverallStanding::rank).findFirst().orElse(0);
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

    private void showError(Exception exception) {
        if (exception instanceof TournamentPersistenceException) refreshWorkspace();
        showError(exception.getMessage());
    }
    private void showError(String message) { view.showStatus(message == null ? "Operation failed." : message); }
}
