package edu.nus.cs3227.fencingtournament.ui;

import edu.nus.cs3227.fencingtournament.application.PoolProgress;
import edu.nus.cs3227.fencingtournament.domain.Fencer;
import edu.nus.cs3227.fencingtournament.domain.TournamentPhase;
import edu.nus.cs3227.fencingtournament.domain.pool.Pool;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.shape.Line;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.List;
import java.util.Comparator;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Main JavaFX workspace. It renders presentation state and delegates user intent to its controller. */
public final class TournamentView extends BorderPane {
    private static final double TABLEAU_LEFT = 22;
    private static final double TABLEAU_ROUND_STEP = 244;
    private static final double TABLEAU_CARD_WIDTH = 214;
    private static final double TABLEAU_CARD_HEIGHT = 56;
    private static final double TABLEAU_FIRST_CENTRE = 78;
    private static final double TABLEAU_OPENING_STEP = 104;
    private static final double TABLEAU_ROW_SEPARATION = 28;
    private final Label tournamentNameLabel = new Label("No tournament open");
    private final Label phaseLabel = new Label("Start by creating or opening a tournament");
    private final Label progressLabel = new Label();
    private final Label statusLabel = new Label();
    private final TextField tournamentNameField = new TextField();
    private final Button createButton = new Button("Create tournament");
    private final Button loadButton = new Button("Open");
    private final Button saveButton = new Button("Save");

    private final TextField fencerNameField = new TextField();
    private final Button addFencerButton = new Button("Add fencer");
    private final Button removeFencerButton = new Button("Remove selected");
    private final ListView<Fencer> fencerList = new ListView<>(FXCollections.observableArrayList());
    private final ListView<Fencer> seedList = new ListView<>(FXCollections.observableArrayList());
    private final Button moveSeedUpButton = new Button("Move up");
    private final Button moveSeedDownButton = new Button("Move down");
    private final Button confirmSeedingButton = new Button("Continue to seeding");
    private final Button applySeedingButton = new Button("Confirm seed order");
    private final Button generatePoolsButton = new Button("Generate pools");

    private final TabPane tabs = new TabPane();
    private final Tab fencersTab = new Tab("Setup");
    private final Tab poolsTab = new Tab("Pools");
    private final Tab standingsTab = new Tab("Pool Result");
    private final Tab eliminationTab = new Tab("Direct Elimination");
    private final Tab finalResultsTab = new Tab("Final Results");
    private final Button generateEliminationButton = new Button("Generate direct elimination");
    private final VBox createTournamentSection = new VBox();
    private final VBox registrationSection = new VBox();
    private final VBox seedingSection = new VBox();

    private final ListView<Pool> poolList = new ListView<>(FXCollections.observableArrayList());
    private final Label selectedPoolLabel = new Label("Select a pool");
    private final Label poolProgressLabel = new Label();
    private final GridPane poolMatrixGrid = new GridPane();
    private final TextField firstScoreField = new TextField();
    private final TextField secondScoreField = new TextField();
    private final Button recordResultButton = new Button("Record result");
    private final Label selectedBoutLabel = new Label("Select a bout in the matrix");
    private final Label firstFencerLabel = new Label("—");
    private final Label secondFencerLabel = new Label("—");
    private final Label resultStateLabel = new Label();
    private final VBox scoreFields = new VBox();
    private final VBox resultEntry = new VBox();

    private final GridPane standingsGrid = new GridPane();
    private final Label standingsStatusLabel = new Label();
    private final GridPane finalResultsGrid = new GridPane();
    private final Label championResultLabel = new Label("—");
    private final Label runnerUpResultLabel = new Label("—");
    private final Pane bracketBoard = new Pane();
    private final StackPane bracketCanvas = new StackPane(bracketBoard);
    private final ScrollPane bracketScroll = new ScrollPane(bracketCanvas);
    private final Label selectedEliminationMatchLabel = new Label("Select a pending bout in the bracket");
    private final TextField eliminationFirstScoreField = new TextField();
    private final TextField eliminationSecondScoreField = new TextField();
    private final Button recordEliminationResultButton = new Button("Record result");
    private final Label eliminationFirstNameLabel = new Label("—");
    private final Label eliminationSecondNameLabel = new Label("—");
    private List<EliminationMatchRow> renderedEliminationMatches = List.of();
    private UUID selectedEliminationMatchId;
    private Consumer<UUID> eliminationMatchHandler = ignored -> { };
    private BiConsumer<UUID, UUID> matrixCellHandler = (row, opponent) -> { };
    private UUID selectedMatrixRow;
    private UUID selectedMatrixOpponent;
    private List<PoolMatrixRow> renderedMatrixRows = List.of();

    public TournamentView() {
        getStyleClass().add("workspace");
        setTop(buildHeader());
        setCenter(tabs);
        setBottom(buildStatusBar());
        fencersTab.setContent(buildSetupTab());
        poolsTab.setContent(buildPoolsTab());
        standingsTab.setContent(buildStandingsTab());
        eliminationTab.setContent(buildEliminationTab());
        finalResultsTab.setContent(buildFinalResultsTab());
        for (Tab tab : List.of(fencersTab, poolsTab, standingsTab, eliminationTab, finalResultsTab)) tab.setClosable(false);
        tabs.getTabs().addAll(fencersTab, poolsTab, standingsTab, eliminationTab, finalResultsTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("phase-navigation");
        configureListCells();
        setNoTournamentState();
    }

    public Scene scene() {
        Scene scene = new Scene(this, 1280, 800);
        URL stylesheet = getClass().getResource("tournament.css");
        if (stylesheet != null) scene.getStylesheets().add(stylesheet.toExternalForm());
        return scene;
    }

    public TextField tournamentNameField() { return tournamentNameField; }
    public Button createButton() { return createButton; }
    public Button loadButton() { return loadButton; }
    public Button saveButton() { return saveButton; }
    public TextField fencerNameField() { return fencerNameField; }
    public Button addFencerButton() { return addFencerButton; }
    public Button removeFencerButton() { return removeFencerButton; }
    public ListView<Fencer> fencerList() { return fencerList; }
    public ListView<Fencer> seedList() { return seedList; }
    public Button moveSeedUpButton() { return moveSeedUpButton; }
    public Button moveSeedDownButton() { return moveSeedDownButton; }
    public Button confirmSeedingButton() { return confirmSeedingButton; }
    public Button applySeedingButton() { return applySeedingButton; }
    public Button generatePoolsButton() { return generatePoolsButton; }
    public TabPane tabs() { return tabs; }
    public Tab poolsTab() { return poolsTab; }
    public Tab standingsTab() { return standingsTab; }
    public Tab eliminationTab() { return eliminationTab; }
    public Tab finalResultsTab() { return finalResultsTab; }
    public Button generateEliminationButton() { return generateEliminationButton; }
    public TextField eliminationFirstScoreField() { return eliminationFirstScoreField; }
    public TextField eliminationSecondScoreField() { return eliminationSecondScoreField; }
    public Button recordEliminationResultButton() { return recordEliminationResultButton; }
    public ListView<Pool> poolList() { return poolList; }
    public TextField firstScoreField() { return firstScoreField; }
    public TextField secondScoreField() { return secondScoreField; }
    public Button recordResultButton() { return recordResultButton; }
    public void setMatrixCellHandler(BiConsumer<UUID, UUID> handler) { matrixCellHandler = handler == null ? (row, opponent) -> { } : handler; }
    public void markSelectedMatrixCell(UUID row, UUID opponent) { selectedMatrixRow = row; selectedMatrixOpponent = opponent; renderPoolMatrix(renderedMatrixRows); }
    public void setEliminationMatchHandler(Consumer<UUID> handler) { eliminationMatchHandler = handler == null ? ignored -> { } : handler; }

    public void renderFencers(List<Fencer> fencers, List<Fencer> seedOrder) { fencerList.getItems().setAll(fencers); seedList.getItems().setAll(seedOrder); }
    public void renderPools(List<Pool> pools) {
        Pool selected = poolList.getSelectionModel().getSelectedItem();
        poolList.getItems().setAll(pools);
        if (!pools.isEmpty()) poolList.getSelectionModel().select(Math.max(0, selected == null ? 0 : indexOfPool(pools, selected.id())));
    }
    public void renderSelectedPool(String poolName, List<String> members, List<PoolMatrixRow> matrixRows) {
        selectedPoolLabel.setText(poolName);
        poolProgressLabel.setText(members.size() + " fencers · click an unfinished cell to record a result");
        renderPoolMatrix(matrixRows);
    }
    public void renderStandings(List<PoolStandingRow> standings, boolean complete) {
        standingsGrid.getChildren().setAll(new Label("Pool Result is calculated after all pool bouts are complete."));
        standingsStatusLabel.setText("Pool Result is calculated after all pool bouts are complete.");
        standingsStatusLabel.getStyleClass().setAll("standing-status", complete ? "is-final" : "is-provisional");
    }

    public void renderOverallSeeding(List<OverallSeedingRow> rows) {
        renderPoolResultGrid(rows.stream().sorted(Comparator.comparingInt(OverallSeedingRow::rank)).toList());
        standingsStatusLabel.setText("Final Pool Result");
        standingsStatusLabel.getStyleClass().setAll("standing-status", "is-final");
    }
    public void renderFinalResults(List<FinalResultsRow> rows) {
        renderFinalResultsGrid(rows);
        championResultLabel.setText(rows.isEmpty() ? "—" : rows.getFirst().fencerName() + " (Seed " + rows.getFirst().poolSeed() + ")");
        runnerUpResultLabel.setText(rows.size() < 2 ? "—" : rows.get(1).fencerName() + " (Seed " + rows.get(1).poolSeed() + ")");
    }

    public void renderEliminationBracket(List<EliminationMatchRow> matches) {
        renderedEliminationMatches = List.copyOf(matches);
        bracketBoard.getChildren().clear();
        if (matches.isEmpty()) return;
        int finalRound = matches.stream().mapToInt(EliminationMatchRow::round).max().orElse(1);
        java.util.Map<String, EliminationMatchRow> byPosition = matches.stream().collect(
                java.util.stream.Collectors.toMap(match -> match.round() + ":" + match.position(), match -> match));
        java.util.Map<String, BracketGeometry> geometry = calculateBracketGeometry(matches, byPosition, finalRound);
        for (int round = 1; round <= finalRound; round++) {
            int currentRound = round;
            int matchCount = (int) matches.stream().filter(match -> match.round() == currentRound).count();
            Label heading = new Label(roundName(matchCount)); heading.getStyleClass().add("bracket-heading");
            if (round == finalRound) heading.getStyleClass().add("bracket-final-heading");
            heading.relocate(boardX(round) + (TABLEAU_CARD_WIDTH - heading.prefWidth(-1)) / 2, 4); bracketBoard.getChildren().add(heading);
        }
        EliminationMatchRow finalMatch = byPosition.get(finalRound + ":0");
        for (EliminationMatchRow target : matches) {
            if (target.round() == 1) continue;
            BracketGeometry targetGeometry = geometry.get(keyOf(target));
            for (int slot = 0; slot < 2; slot++) {
                EliminationMatchRow source = byPosition.get((target.round() - 1) + ":" + (target.position() * 2 + slot));
                if (source == null) continue;
                BracketGeometry sourceGeometry = geometry.get(keyOf(source));
                double sourceX = boardX(source.round()) + TABLEAU_CARD_WIDTH;
                double middleX = sourceX + 15;
                bracketBoard.getChildren().addAll(connector(sourceX, sourceGeometry.centreY(), middleX, sourceGeometry.centreY()),
                        connector(middleX, sourceGeometry.centreY(), middleX, targetGeometry.centreY()),
                        connector(middleX, targetGeometry.centreY(), boardX(target.round()), targetGeometry.centreY()));
            }
        }
        matches.stream().sorted(Comparator.comparingInt(EliminationMatchRow::round).thenComparingInt(EliminationMatchRow::position))
                .forEach(match -> drawBoutCard(match, geometry.get(keyOf(match)), match.round() == finalRound));
        double winnerX = boardX(finalRound) + TABLEAU_ROUND_STEP;
        Label winnerHeading = new Label("Winner"); winnerHeading.getStyleClass().add("bracket-heading"); winnerHeading.relocate(winnerX + (170 - winnerHeading.prefWidth(-1)) / 2, 4);
        BracketGeometry finalGeometry = geometry.get(keyOf(finalMatch));
        Label champion = new Label(finalMatch.resolved() ? winnerLabel(finalMatch) : "Champion");
        champion.getStyleClass().add("bracket-champion");
        double championX = winnerX + (170 - champion.prefWidth(-1)) / 2;
        champion.relocate(championX, finalGeometry.centreY() - 11);
        bracketBoard.getChildren().addAll(winnerHeading, connector(boardX(finalRound) + TABLEAU_CARD_WIDTH, finalGeometry.centreY(), championX - 6, finalGeometry.centreY()), champion);
        double boardHeight = matches.stream().map(match -> geometry.get(keyOf(match)))
                .mapToDouble(BracketGeometry::centreY).max().orElse(TABLEAU_FIRST_CENTRE) + TABLEAU_CARD_HEIGHT / 2 + 26;
        double boardWidth = winnerX + 170;
        bracketBoard.setMinSize(boardWidth, boardHeight); bracketBoard.setPrefSize(boardWidth, boardHeight);
        bracketBoard.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        bracketCanvas.setMinSize(boardWidth, boardHeight); bracketCanvas.setPrefSize(boardWidth, boardHeight);
        bracketScroll.setPrefViewportHeight(Math.min(500, boardHeight + 8));
    }

    public void showSelectedEliminationMatch(EliminationMatchRow match) {
        if (match == null) {
            selectedEliminationMatchId = null;
            selectedEliminationMatchLabel.setText("Select a pending bout in the bracket"); eliminationFirstNameLabel.setText("—"); eliminationSecondNameLabel.setText("—");
            eliminationFirstScoreField.clear(); eliminationSecondScoreField.clear(); eliminationFirstScoreField.setDisable(true); eliminationSecondScoreField.setDisable(true); recordEliminationResultButton.setDisable(true); return;
        }
        selectedEliminationMatchId = match.matchId();
        if (!renderedEliminationMatches.isEmpty()) renderEliminationBracket(renderedEliminationMatches);
        int matchCount = (int) renderedEliminationMatches.stream().filter(candidate -> candidate.round() == match.round()).count();
        selectedEliminationMatchLabel.setText(roundName(matchCount) + " · selected bout");
        eliminationFirstNameLabel.setText(participantText(match.first())); eliminationSecondNameLabel.setText(participantText(match.second()));
        if (match.ready()) {
            eliminationFirstScoreField.clear(); eliminationSecondScoreField.clear();
            eliminationFirstScoreField.setDisable(false); eliminationSecondScoreField.setDisable(false); recordEliminationResultButton.setDisable(false);
        } else {
            eliminationFirstScoreField.setText(match.first().score()); eliminationSecondScoreField.setText(match.second().score());
            eliminationFirstScoreField.setDisable(true); eliminationSecondScoreField.setDisable(true); recordEliminationResultButton.setDisable(true);
        }
    }
    public void showSelectedBout(PoolBoutRow bout) {
        if (bout == null) {
            selectedBoutLabel.setText("Select an unfinished bout in the matrix"); firstFencerLabel.setText("—"); secondFencerLabel.setText("—"); resultStateLabel.setText("");
            scoreFields.setVisible(false); scoreFields.setManaged(false); recordResultButton.setDisable(true); return;
        }
        firstFencerLabel.setText(bout.firstName()); secondFencerLabel.setText(bout.secondName()); selectedBoutLabel.setText(bout.firstName() + "  —  " + bout.secondName());
        if (bout.completed()) {
            resultStateLabel.setText("Completed · " + bout.scoreText()); resultStateLabel.getStyleClass().setAll("result-state", "is-completed");
            scoreFields.setVisible(false); scoreFields.setManaged(false); recordResultButton.setDisable(true);
        } else {
            resultStateLabel.setText("Pending result"); resultStateLabel.getStyleClass().setAll("result-state", "is-pending");
            scoreFields.setVisible(true); scoreFields.setManaged(true); firstScoreField.clear(); secondScoreField.clear(); recordResultButton.setDisable(false);
        }
    }
    public void showPhase(TournamentPhase phase, PoolProgress progress) {
        phaseLabel.setText(phaseText(phase));
        progressLabel.setText(progress == null || progress.totalBouts() == 0 ? "" : progress.completedBouts() + " of " + progress.totalBouts() + " pool bouts complete");
    }
    public void showStatus(String message) { statusLabel.setText(message == null ? "" : message); }
    public void setPhaseControls(TournamentPhase phase, boolean hasTournament, boolean poolResultsFinalized, boolean hasEliminationBracket) {
        boolean registration = hasTournament && phase == TournamentPhase.REGISTRATION;
        boolean seeding = hasTournament && phase == TournamentPhase.SEEDING;
        boolean pools = hasTournament && (phase == TournamentPhase.POOL_PHASE || phase == TournamentPhase.ELIMINATION_PHASE || phase == TournamentPhase.COMPLETE);
        showOnly(createTournamentSection, !hasTournament); showOnly(registrationSection, registration); showOnly(seedingSection, seeding);
        saveButton.setDisable(!hasTournament); fencerNameField.setDisable(!registration); addFencerButton.setDisable(!registration); removeFencerButton.setDisable(!registration); fencerList.setDisable(!registration);
        seedList.setDisable(!seeding); moveSeedUpButton.setDisable(!seeding); moveSeedDownButton.setDisable(!seeding); applySeedingButton.setDisable(!(registration || seeding));
        confirmSeedingButton.setDisable(!registration); applySeedingButton.setText("Apply revised order"); generatePoolsButton.setDisable(!seeding); poolsTab.setDisable(!pools); standingsTab.setDisable(!poolResultsFinalized); eliminationTab.setDisable(!hasEliminationBracket); finalResultsTab.setDisable(phase != TournamentPhase.COMPLETE); generateEliminationButton.setDisable(!poolResultsFinalized || hasEliminationBracket);
    }
    public void setNoTournamentState() {
        tournamentNameLabel.setText("No tournament open"); phaseLabel.setText("Create a tournament or open an existing file"); progressLabel.setText("");
        fencerList.getItems().clear(); seedList.getItems().clear(); poolList.getItems().clear(); poolMatrixGrid.getChildren().clear(); standingsGrid.getChildren().clear(); finalResultsGrid.getChildren().clear(); championResultLabel.setText("—"); runnerUpResultLabel.setText("—"); showSelectedBout(null);
        setPhaseControls(TournamentPhase.REGISTRATION, false, false, false);
    }
    public void showTournamentName(String name) { tournamentNameLabel.setText(name); }

    private VBox buildHeader() {
        Label appName = new Label("Fencing Tournament Manager"); appName.getStyleClass().add("app-name"); tournamentNameLabel.getStyleClass().add("tournament-name"); phaseLabel.getStyleClass().add("phase-name"); progressLabel.getStyleClass().add("progress-text");
        loadButton.getStyleClass().add("secondary-action"); saveButton.getStyleClass().add("primary-action");
        HBox identity = new HBox(14, appName, tournamentNameLabel); identity.setAlignment(Pos.CENTER_LEFT);
        VBox state = new VBox(2, phaseLabel, progressLabel); HBox actions = new HBox(8, loadButton, saveButton); actions.setAlignment(Pos.CENTER_RIGHT); HBox.setHgrow(actions, Priority.ALWAYS);
        HBox bar = new HBox(20, identity, state, actions); bar.setAlignment(Pos.CENTER_LEFT); bar.getStyleClass().add("top-bar"); return new VBox(bar);
    }
    private VBox buildSetupTab() {
        tournamentNameField.setPromptText("Tournament name, e.g. Friday Internal Open"); tournamentNameField.setOnAction(event -> createButton.fire()); createButton.getStyleClass().add("primary-action");
        createTournamentSection.getChildren().setAll(sectionTitle("Start a tournament", "Create a local tournament, or open one you saved earlier."), formRow(tournamentNameField, createButton)); createTournamentSection.getStyleClass().add("setup-empty-state");
        fencerNameField.setPromptText("Enter a fencer display name"); fencerNameField.setOnAction(event -> addFencerButton.fire()); addFencerButton.getStyleClass().add("primary-action"); confirmSeedingButton.getStyleClass().add("primary-action"); removeFencerButton.getStyleClass().add("quiet-danger-action"); fencerList.setPlaceholder(new Label("No fencers registered yet.")); fencerList.setPrefHeight(280); fencerList.setMinHeight(180); fencerList.setMaxHeight(320);
        registrationSection.getChildren().setAll(sectionTitle("Registration", "Add the fencers competing in this tournament."), formRow(fencerNameField, addFencerButton), compactHeader("Registered fencers", removeFencerButton), fencerList, actionRow(confirmSeedingButton)); registrationSection.getStyleClass().add("setup-stage");
        seedList.setPlaceholder(new Label("No fencers registered yet.")); VBox.setVgrow(seedList, Priority.ALWAYS); HBox reorder = new HBox(8, moveSeedUpButton, moveSeedDownButton); reorder.getStyleClass().add("secondary-actions"); generatePoolsButton.getStyleClass().add("primary-action");
        seedingSection.getChildren().setAll(sectionTitle("Seed the field", "Set the order used to distribute fencers across pools."), seedList, reorder, actionRow(applySeedingButton, generatePoolsButton)); seedingSection.getStyleClass().add("setup-stage");
        VBox root = new VBox(createTournamentSection, registrationSection, seedingSection); root.getStyleClass().add("screen-content"); return root;
    }
    private BorderPane buildPoolsTab() {
        Label poolsTitle = new Label("POOLS"); poolsTitle.getStyleClass().add("side-label"); poolList.setPrefWidth(164); poolList.setFixedCellSize(38);
        VBox navigator = new VBox(8, poolsTitle, poolList); navigator.getStyleClass().add("pool-navigator"); VBox.setVgrow(poolList, Priority.ALWAYS);
        selectedPoolLabel.getStyleClass().add("screen-title"); poolProgressLabel.getStyleClass().add("screen-subtitle"); VBox poolHeading = new VBox(2, selectedPoolLabel, poolProgressLabel);
        Label matrixLabel = new Label("POOL MATRIX"); matrixLabel.getStyleClass().add("section-kicker"); VBox matrixArea = new VBox(8, matrixLabel, poolMatrixGrid); matrixArea.getStyleClass().add("matrix-area");
        buildResultEntry(); VBox content = new VBox(16, poolHeading, matrixArea, resultEntry); content.getStyleClass().add("pools-content"); BorderPane root = new BorderPane(content); root.setLeft(navigator); BorderPane.setMargin(navigator, new Insets(0, 24, 0, 0)); return root;
    }
    private void buildResultEntry() {
        Label title = new Label("RECORD RESULT"); title.getStyleClass().add("section-kicker"); selectedBoutLabel.getStyleClass().add("selected-bout"); firstFencerLabel.getStyleClass().add("result-fencer"); secondFencerLabel.getStyleClass().add("result-fencer");
        firstScoreField.setPromptText("Score"); secondScoreField.setPromptText("Score"); firstScoreField.getStyleClass().add("score-field"); secondScoreField.getStyleClass().add("score-field"); firstScoreField.setPrefWidth(74); secondScoreField.setPrefWidth(74); recordResultButton.getStyleClass().add("primary-action");
        Label dash = new Label("—"); dash.getStyleClass().add("score-dash"); HBox scoreLine = new HBox(14, firstScoreField, dash, secondScoreField, recordResultButton); scoreLine.setAlignment(Pos.CENTER); scoreFields.getChildren().setAll(scoreLine); scoreFields.setAlignment(Pos.CENTER);
        HBox names = new HBox(40, firstFencerLabel, secondFencerLabel); names.setAlignment(Pos.CENTER); resultEntry.getChildren().setAll(title, selectedBoutLabel, names, resultStateLabel, scoreFields); resultEntry.setAlignment(Pos.CENTER); resultEntry.getStyleClass().add("result-entry");
    }
    private VBox buildStandingsTab() {
        Label title = new Label("Pool Result"); title.getStyleClass().add("screen-title"); Label description = new Label("Overall placing after every pool bout has been finalized."); description.getStyleClass().add("screen-subtitle"); HBox status = new HBox(standingsStatusLabel, generateEliminationButton); status.setSpacing(16); status.setAlignment(Pos.CENTER_LEFT); generateEliminationButton.getStyleClass().add("primary-action");
        VBox root = new VBox(6, title, description, status, standingsGrid); root.getStyleClass().add("screen-content"); return root;
    }
    private VBox buildEliminationTab() {
        Label title = new Label("Direct Elimination"); title.getStyleClass().add("screen-title");
        Label hint = new Label("Select a pending bracket bout to record its result."); hint.getStyleClass().add("screen-subtitle");
        bracketBoard.getStyleClass().add("bracket-board"); bracketCanvas.getStyleClass().add("bracket-canvas"); bracketCanvas.setAlignment(Pos.TOP_CENTER); bracketScroll.setFitToHeight(false); bracketScroll.setFitToWidth(true); bracketScroll.setPannable(true); bracketScroll.setPrefViewportHeight(380); bracketScroll.getStyleClass().add("bracket-scroll");
        eliminationFirstScoreField.setPrefWidth(64); eliminationSecondScoreField.setPrefWidth(64); recordEliminationResultButton.getStyleClass().add("primary-action");
        HBox firstRow = new HBox(12, eliminationFirstNameLabel, eliminationFirstScoreField); HBox.setHgrow(eliminationFirstNameLabel, Priority.ALWAYS); firstRow.getStyleClass().add("de-result-row");
        HBox secondRow = new HBox(12, eliminationSecondNameLabel, eliminationSecondScoreField); HBox.setHgrow(eliminationSecondNameLabel, Priority.ALWAYS); secondRow.getStyleClass().add("de-result-row");
        VBox entry = new VBox(7, new Label("RECORD RESULT"), selectedEliminationMatchLabel, firstRow, secondRow, recordEliminationResultButton); entry.setAlignment(Pos.CENTER_LEFT); entry.getStyleClass().add("de-result-entry");
        VBox root = new VBox(16, title, hint, bracketScroll, entry); root.getStyleClass().add("screen-content"); VBox.setVgrow(bracketScroll, Priority.ALWAYS); return root;
    }
    private VBox buildFinalResultsTab() {
        Label title = new Label("Final Results"); title.getStyleClass().add("screen-title");
        Label complete = new Label("COMPLETED"); complete.getStyleClass().add("completion-status");
        Label championTitle = new Label("CHAMPION"); championTitle.getStyleClass().add("section-kicker"); championResultLabel.getStyleClass().add("champion-name");
        Label runnerTitle = new Label("RUNNER-UP"); runnerTitle.getStyleClass().add("section-kicker"); runnerUpResultLabel.getStyleClass().add("runner-up-name");
        VBox champion = new VBox(3, championTitle, championResultLabel); champion.getStyleClass().addAll("finalist-summary", "champion-summary");
        VBox runnerUp = new VBox(3, runnerTitle, runnerUpResultLabel); runnerUp.getStyleClass().add("finalist-summary");
        HBox finalists = new HBox(12, champion, runnerUp); finalists.getStyleClass().add("finalists-row");
        Label tableTitle = new Label("FINAL STANDINGS"); tableTitle.getStyleClass().add("section-kicker");
        VBox root = new VBox(12, title, complete, finalists, tableTitle, finalResultsGrid); root.getStyleClass().add("screen-content"); return root;
    }
    private HBox buildStatusBar() { statusLabel.getStyleClass().add("status-text"); HBox bar = new HBox(statusLabel); bar.getStyleClass().add("status-bar"); return bar; }
    private void configureListCells() {
        fencerList.setCellFactory(ignored -> fencerCell()); seedList.setCellFactory(ignored -> new ListCell<>() { @Override protected void updateItem(Fencer fencer, boolean empty) { super.updateItem(fencer, empty); setText(empty || fencer == null ? null : (getIndex() + 1) + "   " + fencer.name()); }});
        poolList.setCellFactory(ignored -> poolCell());
    }
    private ListCell<Pool> poolCell() { return new ListCell<>() { @Override protected void updateItem(Pool pool, boolean empty) { super.updateItem(pool, empty); setText(empty || pool == null ? null : pool.name()); }}; }
    private ListCell<Fencer> fencerCell() { return new ListCell<>() { @Override protected void updateItem(Fencer fencer, boolean empty) { super.updateItem(fencer, empty); setText(empty || fencer == null ? null : fencer.name()); }}; }
    private void configureTables() {
        poolMatrixTable.setPlaceholder(new Label("Select a pool to see its matrix.")); poolMatrixTable.setFixedCellSize(48); poolMatrixTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY); poolMatrixTable.getStyleClass().add("pool-matrix");
        TableColumn<OverallSeedingRow, String> fencer = textColumn("Fencer", OverallSeedingRow::name, 180);
        fencer.getStyleClass().add("standing-name-column");
        standingsTable.getColumns().addAll(textColumn("Place", row -> Integer.toString(row.rank()), 58), fencer,
                textColumn("V", row -> Integer.toString(row.wins()), 48),
                textColumn("M", row -> Integer.toString(row.matches()), 52),
                textColumn("V/M", row -> String.format("%.2f", row.ratio()), 60),
                textColumn("TS", row -> Integer.toString(row.touchesScored()), 56),
                textColumn("TR", row -> Integer.toString(row.touchesReceived()), 56),
                textColumn("Indices", row -> Integer.toString(row.indicator()), 68));
        standingsTable.getColumns().forEach(column -> {
            column.setSortable(false);
            column.setReorderable(false);
        });
        standingsTable.setSortPolicy(table -> false);
        standingsTable.setPlaceholder(new Label("Pool Result is not available yet.")); standingsTable.getStyleClass().add("standings-table");
        TableColumn<FinalResultsRow, String> finalFencer = textColumn("Fencer", FinalResultsRow::fencerName, 190);
        finalFencer.getStyleClass().add("standing-name-column");
        finalResultsTable.getColumns().addAll(textColumn("Place", row -> Integer.toString(row.place()), 58), finalFencer,
                textColumn("Pool seed", row -> Integer.toString(row.poolSeed()), 74),
                textColumn("Pool V/M", row -> row.poolWins() + "/" + row.poolMatches(), 76),
                textColumn("Indicator", row -> Integer.toString(row.indicator()), 72),
                textColumn("DE finish", FinalResultsRow::directEliminationFinish, 164));
        finalResultsTable.getColumns().forEach(column -> { column.setSortable(false); column.setReorderable(false); });
        finalResultsTable.setSortPolicy(table -> false); finalResultsTable.setPlaceholder(new Label("Final results will appear once the final is complete.")); finalResultsTable.getStyleClass().addAll("standings-table", "final-results-table");
    }
    private void configureMatrixColumns(List<PoolMatrixRow> rows) {
        poolMatrixTable.getColumns().clear(); TableColumn<PoolMatrixRow, String> name = textColumn("Fencer", PoolMatrixRow::fencerName, 174); name.getStyleClass().add("matrix-name-column"); poolMatrixTable.getColumns().add(name); if (rows.isEmpty()) return;
        for (UUID opponentId : rows.get(0).cells().keySet()) { String nameText = fencerNameForColumn(rows, opponentId); TableColumn<PoolMatrixRow, String> column = new TableColumn<>(nameText); column.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().cell(opponentId))); column.setPrefWidth(Math.max(84, nameText.length() * 8 + 26)); column.setCellFactory(ignored -> matrixCell(opponentId)); column.setSortable(false); column.setReorderable(false); column.getStyleClass().add("matrix-score-column"); poolMatrixTable.getColumns().add(column); }
        poolMatrixTable.getColumns().forEach(column -> {
            column.setSortable(false);
            column.setReorderable(false);
        });
        poolMatrixTable.setSortPolicy(table -> false);
    }
    private TableCell<PoolMatrixRow, String> matrixCell(UUID opponentId) { return new TableCell<>() { @Override protected void updateItem(String value, boolean empty) { super.updateItem(value, empty); getStyleClass().removeAll("matrix-diagonal", "matrix-pending", "matrix-win", "matrix-loss", "matrix-selected"); setText(empty ? null : value == null ? "" : value); setAlignment(Pos.CENTER); if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) { setOnMouseClicked(null); return; } PoolMatrixRow row = getTableView().getItems().get(getIndex()); setOnMouseClicked(event -> matrixCellHandler.accept(row.fencerId(), opponentId)); if ("—".equals(value)) getStyleClass().add("matrix-diagonal"); else if (value == null || value.isBlank()) getStyleClass().add("matrix-pending"); else getStyleClass().add(isWinner(row, opponentId, value) ? "matrix-win" : "matrix-loss"); if (row.fencerId().equals(selectedMatrixRow) && opponentId.equals(selectedMatrixOpponent)) getStyleClass().add("matrix-selected"); }}; }
    private boolean isWinner(PoolMatrixRow row, UUID opponentId, String score) { for (PoolMatrixRow opponent : poolMatrixTable.getItems()) if (opponent.fencerId().equals(opponentId)) return parse(score) > parse(opponent.cell(row.fencerId())); return false; }
    private static int parse(String value) { try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { return -1; } }
    private java.util.Map<String, BracketGeometry> calculateBracketGeometry(List<EliminationMatchRow> matches,
                                                                              java.util.Map<String, EliminationMatchRow> byPosition,
                                                                              int finalRound) {
        java.util.Map<String, BracketGeometry> geometry = new java.util.HashMap<>();
        for (int round = 1; round <= finalRound; round++) {
            int currentRound = round;
            matches.stream().filter(match -> match.round() == currentRound)
                    .sorted(Comparator.comparingInt(EliminationMatchRow::position)).forEach(match -> {
                        double centreY;
                        double firstRowY;
                        double secondRowY;
                        if (match.round() == 1) {
                            centreY = TABLEAU_FIRST_CENTRE + match.position() * TABLEAU_OPENING_STEP;
                            firstRowY = centreY - TABLEAU_ROW_SEPARATION / 2;
                            secondRowY = centreY + TABLEAU_ROW_SEPARATION / 2;
                        } else {
                            BracketGeometry firstSource = geometry.get((match.round() - 1) + ":" + (match.position() * 2));
                            BracketGeometry secondSource = geometry.get((match.round() - 1) + ":" + (match.position() * 2 + 1));
                            firstRowY = firstSource.centreY();
                            secondRowY = secondSource.centreY();
                            centreY = (firstRowY + secondRowY) / 2;
                        }
                        geometry.put(keyOf(match), new BracketGeometry(centreY, firstRowY, secondRowY));
                    });
        }
        return geometry;
    }

    private void drawBoutCard(EliminationMatchRow match, BracketGeometry geometry, boolean finalRound) {
        Pane card = new Pane();
        card.setPrefSize(TABLEAU_CARD_WIDTH, TABLEAU_CARD_HEIGHT);
        card.getStyleClass().add("fencing-bout-card");
        if (match.bye()) card.getStyleClass().add("fencing-bye-card");
        else if (match.resolved()) card.getStyleClass().add("fencing-resolved-card");
        else if (match.ready()) card.getStyleClass().add("fencing-ready-card");
        else card.getStyleClass().add("fencing-future-card");
        if (finalRound) card.getStyleClass().add("fencing-final-card");
        if (match.ready() && match.matchId().equals(selectedEliminationMatchId)) card.getStyleClass().add("fencing-selected-card");

        Pane firstRow = participantCardRow(match.first());
        Pane secondRow = participantCardRow(match.second());
        firstRow.getStyleClass().add("fencing-bout-row-first");
        firstRow.relocate(0, 0); secondRow.relocate(0, 28);
        card.getChildren().addAll(firstRow, secondRow);
        if (match.ready()) card.setOnMouseClicked(event -> eliminationMatchHandler.accept(match.matchId()));
        card.relocate(boardX(match.round()), geometry.centreY() - TABLEAU_CARD_HEIGHT / 2);
        bracketBoard.getChildren().add(card);
    }

    private static Pane participantCardRow(EliminationParticipant participant) {
        Label seed = new Label(participant.seed() == 0 ? "" : Integer.toString(participant.seed()));
        seed.getStyleClass().add("fencing-bout-seed"); seed.relocate(9, 6);
        Label name = new Label(participant.unresolved() ? "Awaiting opponent" : participant.name());
        name.getStyleClass().add("fencing-bout-name"); name.setMaxWidth(144); name.setTextOverrun(OverrunStyle.ELLIPSIS); name.relocate(31, 5);
        Label score = new Label(participant.score());
        score.getStyleClass().add("fencing-bout-score"); score.relocate(178, 5);
        if (participant.winner()) { name.getStyleClass().add("bracket-winner"); score.getStyleClass().add("bracket-winner"); }
        if (participant.unresolved()) name.getStyleClass().add("bracket-unresolved");
        if (participant.bye()) name.getStyleClass().add("fencing-bye-label");
        Pane row = new Pane(seed, name, score); row.setPrefSize(TABLEAU_CARD_WIDTH, 28); return row;
    }

    private static String participantText(EliminationParticipant participant) {
        return participant.seed() == 0 ? participant.name() : participant.name() + " (" + participant.seed() + ")";
    }
    private static Line connector(double startX, double startY, double endX, double endY) {
        Line line = new Line(startX, startY, endX, endY); line.getStyleClass().add("bracket-connector"); return line;
    }
    private static String keyOf(EliminationMatchRow match) { return match.round() + ":" + match.position(); }
    private static double boardX(int round) { return TABLEAU_LEFT + (round - 1) * TABLEAU_ROUND_STEP; }
    private static String winnerLabel(EliminationMatchRow match) { return participantText(match.first().winner() ? match.first() : match.second()); }
    private record BracketGeometry(double centreY, double firstRowY, double secondRowY) { }
    private static String roundName(int matchCount) {
        return switch (matchCount) {
            case 1 -> "Final";
            case 2 -> "Semi-finals";
            case 4 -> "Quarter-finals";
            default -> "Round of " + (matchCount * 2);
        };
    }
    private static <T> TableColumn<T, String> textColumn(String title, java.util.function.Function<T, String> value, double width) { TableColumn<T, String> column = new TableColumn<>(title); column.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(value.apply(data.getValue()))); column.setPrefWidth(width); return column; }
    private static VBox sectionTitle(String title, String description) { Label heading = new Label(title); heading.getStyleClass().add("screen-title"); Label text = new Label(description); text.getStyleClass().add("screen-subtitle"); return new VBox(3, heading, text); }
    private static HBox formRow(TextField field, Button action) { HBox row = new HBox(10, field, action); row.getStyleClass().add("form-row"); HBox.setHgrow(field, Priority.ALWAYS); return row; }
    private static HBox compactHeader(String title, Button action) { Label heading = new Label(title); heading.getStyleClass().add("list-heading"); HBox row = new HBox(heading, action); row.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(heading, Priority.ALWAYS); return row; }
    private static HBox actionRow(Button... buttons) { HBox row = new HBox(8, buttons); row.setAlignment(Pos.CENTER_RIGHT); row.getStyleClass().add("action-row"); return row; }
    private static void showOnly(VBox node, boolean visible) { node.setVisible(visible); node.setManaged(visible); }
    private static String fencerNameForColumn(List<PoolMatrixRow> rows, UUID id) { return rows.stream().filter(row -> row.fencerId().equals(id)).map(PoolMatrixRow::fencerName).findFirst().orElse("Fencer"); }
    private static int indexOfPool(List<Pool> pools, UUID id) { for (int index = 0; index < pools.size(); index++) if (pools.get(index).id().equals(id)) return index; return 0; }
    private static String phaseText(TournamentPhase phase) { return switch (phase) { case REGISTRATION -> "Setup · registration"; case SEEDING -> "Setup · seeding"; case POOL_PHASE -> "Pool phase"; case ELIMINATION_PHASE -> "Elimination phase"; case COMPLETE -> "Tournament complete"; }; }
}
