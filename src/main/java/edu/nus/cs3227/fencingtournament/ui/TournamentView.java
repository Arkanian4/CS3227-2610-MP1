package edu.nus.cs3227.fencingtournament.ui;

import edu.nus.cs3227.fencingtournament.application.PoolProgress;
import edu.nus.cs3227.fencingtournament.domain.Fencer;
import edu.nus.cs3227.fencingtournament.domain.Tournament;
import edu.nus.cs3227.fencingtournament.domain.TournamentPhase;
import edu.nus.cs3227.fencingtournament.domain.pool.Pool;
import javafx.collections.FXCollections;
import javafx.application.Platform;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.shape.Line;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Comparator;
import java.util.UUID;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

/** Main JavaFX workspace. It renders presentation state and delegates user intent to its controller. */
public final class TournamentView extends BorderPane {
    private static final double TABLEAU_LEFT = 22;
    private static final double TABLEAU_ROUND_STEP = 244;
    private static final double TABLEAU_CARD_WIDTH = 214;
    private static final double TABLEAU_CARD_HEIGHT = 52;
    private static final double TABLEAU_ROW_HEIGHT = 26;
    private static final double TABLEAU_FIRST_CENTRE = 58;
    private static final double TABLEAU_BOTTOM_PADDING = 14;
    private static final double HORIZONTAL_SCROLLBAR_ALLOWANCE = 18;
    private static final double SEED_AUTO_SCROLL_EDGE = 42;
    private static final double SEED_AUTO_SCROLL_MIN_RATE = 0.35;
    private static final double SEED_AUTO_SCROLL_MAX_RATE = 1.15;
    private final Label tournamentNameLabel = new Label("No tournament open");
    private final Label phaseLabel = new Label("Start by creating or opening a tournament");
    private final Label progressLabel = new Label();
    private final VBox tournamentContext = new VBox();
    private final Label statusLabel = new Label();
    private final TextField tournamentNameField = new TextField();
    private final Button createButton = new Button("Create tournament");
    private final Label tournamentNameValidationErrorLabel = new Label();
    private final Button loadButton = new Button("Open");
    private final Button homeButton = new Button("Tournament Home");
    private final TextField homeTournamentNameField = new TextField();
    private final Button homeNewButton = new Button("+ New Tournament");
    private final Button homeCreateButton = new Button("Create");
    private final Button homeCancelButton = new Button("Cancel");
    private final VBox homeCreateForm = new VBox();
    private final Label homeTournamentNameValidationErrorLabel = new Label();
    private final VBox homeTournamentRows = new VBox();
    private final ScrollPane homeTournamentScroll = new ScrollPane(homeTournamentRows);
    private final VBox homeScreen = new VBox();
    private final List<HomeTimestampLabel> homeTimestampLabels = new java.util.ArrayList<>();
    private final Timeline homeTimeRefreshTimer = new Timeline(new KeyFrame(
            javafx.util.Duration.minutes(1), event -> refreshHomeTimeLabels()));
    private final HBox themeSwatches = new HBox(6);
    private final List<Button> themeSwatchButtons = new java.util.ArrayList<>();
    private final HBox appearanceToggle = new HBox(7);
    private final ToggleButton darkAppearanceToggle = new ToggleButton();
    private final Region appearanceToggleThumb = new Region();

    private final TextField fencerNameField = new TextField();
    private final Button addFencerButton = new Button("Add fencer");
    private final Label fencerValidationErrorLabel = new Label();
    private final ListView<Fencer> seedList = new ListView<>(FXCollections.observableArrayList());
    private double seedAutoScrollRate;
    private long seedAutoScrollLastNanos;
    private boolean seedAutoScrollRunning;
    private final AnimationTimer seedAutoScrollTimer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (seedAutoScrollLastNanos == 0) {
                seedAutoScrollLastNanos = now;
                return;
            }
            double elapsedSeconds = Math.min(0.05, (now - seedAutoScrollLastNanos) / 1_000_000_000.0);
            seedAutoScrollLastNanos = now;
            scrollSeedList(seedAutoScrollRate * elapsedSeconds);
        }
    };
    private final Label registeredFencerCountLabel = new Label("0 fencers");
    private final Button generatePoolsButton = new Button("Generate pools");
    private final ComboBox<Integer> maximumPoolSizeChoice = new ComboBox<>();
    private final Label seedingValidationErrorLabel = new Label();

    private final TabPane tabs = new TabPane();
    private final Tab fencersTab = new Tab("Setup");
    private final Tab poolsTab = new Tab("Pools");
    private final Tab standingsTab = new Tab("Pool Result");
    private final Tab eliminationTab = new Tab("Direct Elimination");
    private final Tab finalResultsTab = new Tab("Final Results");
    private final StackPane stageContent = new StackPane();
    private final Button setupNavigationButton = new Button("Setup");
    private final Button poolsNavigationButton = new Button("Pools");
    private final Button standingsNavigationButton = new Button("Pool Result");
    private final Button eliminationNavigationButton = new Button("Direct Elimination");
    private final Button finalResultsNavigationButton = new Button("Final Results");
    private final HBox stageProgress = new HBox();
    private final HBox stageProgressRow = new HBox(stageProgress);
    private final List<Label> stageProgressLabels = new java.util.ArrayList<>();
    private final List<VBox> stageProgressSteps = new java.util.ArrayList<>();
    private final List<Region> stageProgressConnectors = new java.util.ArrayList<>();
    private int tournamentProgressStage;
    private final Button generateEliminationButton = new Button("Generate direct elimination");
    private final VBox createTournamentSection = new VBox();
    private final VBox registrationSection = new VBox();
    private final VBox seedingSection = new VBox();
    private final Button editSetupButton = new Button("Edit setup");
    private final Label setupReadOnlyLabel = new Label("Setup is locked while later tournament stages exist.");
    private TournamentPhase controlledPhase = TournamentPhase.REGISTRATION;
    private boolean controlledHasTournament;

    private final ListView<Pool> poolList = new ListView<>(FXCollections.observableArrayList());
    private final Label selectedPoolLabel = new Label("Select a pool");
    private final Label poolProgressLabel = new Label();
    private final GridPane poolDashboard = new GridPane();
    private final ScrollPane poolDashboardScroll = new ScrollPane(poolDashboard);
    private final TextField firstScoreField = new TextField();
    private final TextField secondScoreField = new TextField();
    private final Button recordResultButton = new Button("Record result");
    private final Button editPoolResultButton = new Button("Edit result");
    private final Label firstFencerLabel = new Label("—");
    private final Label secondFencerLabel = new Label("—");
    private final Label resultStateLabel = new Label();
    private final VBox scoreFields = new VBox();
    private final VBox resultEntry = new VBox();
    private final VBox poolResultHeading = new VBox();
    private final HBox poolResultNames = new HBox();
    private final Label poolValidationErrorLabel = new Label();

    private final GridPane standingsGrid = new GridPane();
    private final Label standingsStatusLabel = new Label();
    private final GridPane finalResultsGrid = new GridPane();
    private final Pane bracketBoard = new Pane();
    private final StackPane bracketCanvas = new StackPane(bracketBoard);
    private final ScrollPane bracketScroll = new ScrollPane(bracketCanvas);
    private final HBox eliminationWorkspace = new HBox();
    private final Label selectedEliminationMatchLabel = new Label("Select a pending bout in the bracket");
    private final TextField eliminationFirstScoreField = new TextField();
    private final TextField eliminationSecondScoreField = new TextField();
    private final Button recordEliminationResultButton = new Button("Record result");
    private final Button editEliminationResultButton = new Button("Edit result");
    private final Button cancelEliminationEditButton = new Button("Cancel");
    private final Label eliminationFirstNameLabel = new Label("—");
    private final Label eliminationSecondNameLabel = new Label("—");
    private final Label eliminationValidationErrorLabel = new Label();
    private List<EliminationMatchRow> renderedEliminationMatches = List.of();
    private UUID selectedEliminationMatchId;
    private int activeEliminationRound = 1;
    private boolean eliminationRelayoutQueued;
    private Consumer<UUID> eliminationMatchHandler = ignored -> { };
    private Consumer<UUID> tournamentOpenHandler = ignored -> { };
    private Consumer<UUID> tournamentDeleteHandler = ignored -> { };
    private Consumer<PoolMatrixSelection> matrixCellHandler = ignored -> { };
    private BiConsumer<UUID, Integer> seedMoveHandler = (fencerId, targetIndex) -> { };
    private Consumer<UUID> fencerRemoveHandler = ignored -> { };
    private Runnable poolSelectionDismissHandler = () -> { };
    private UUID selectedMatrixRow;
    private UUID selectedMatrixOpponent;
    private UUID selectedMatrixPool;
    private List<PoolDashboardPanel> renderedPoolPanels = List.of();
    private Tab activeStage;

    public TournamentView() {
        getStyleClass().add("workspace");
        UiTheme.apply(this);
        setTop(buildHeader());
        setLeft(buildSidebar());
        setCenter(stageContent);
        setBottom(buildStatusBar());
        fencersTab.setContent(buildSetupTab());
        poolsTab.setContent(buildPoolsTab());
        standingsTab.setContent(buildStandingsTab());
        eliminationTab.setContent(buildEliminationTab());
        finalResultsTab.setContent(buildFinalResultsTab());
        buildHomeScreen();
        for (Tab tab : List.of(fencersTab, poolsTab, standingsTab, eliminationTab, finalResultsTab)) tab.setClosable(false);
        tabs.getTabs().addAll(fencersTab, poolsTab, standingsTab, eliminationTab, finalResultsTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setVisible(false);
        tabs.setManaged(false);
        configureListCells();
        configureScoreValidationFeedback();
        homeTimeRefreshTimer.setCycleCount(Timeline.INDEFINITE);
        setNoTournamentState();
    }

    public Scene scene() {
        Scene scene = new Scene(this, 1280, 800);
        UiTheme.apply(scene);
        return scene;
    }

    /** Rebuilds size-dependent boards only after their ScrollPane viewports have real bounds. */
    public void initializeAfterStageShown() {
        applyCss();
        layout();
        refreshSizeDependentStageLayout();
    }

    public TextField tournamentNameField() { return tournamentNameField; }
    public Button createButton() { return createButton; }
    public Label tournamentNameValidationErrorLabel() { return tournamentNameValidationErrorLabel; }
    public Button loadButton() { return loadButton; }
    public Button homeButton() { return homeButton; }
    public TextField homeTournamentNameField() { return homeTournamentNameField; }
    public Button homeNewButton() { return homeNewButton; }
    public Button homeCreateButton() { return homeCreateButton; }
    public Button homeCancelButton() { return homeCancelButton; }
    public Label homeTournamentNameValidationErrorLabel() { return homeTournamentNameValidationErrorLabel; }
    public void showWorkspace() {
        if (activeStage == null) selectStage(fencersTab);
    }
    public void showHome() {
        activeStage = null;
        stageContent.getChildren().setAll(homeScreen);
        refreshNavigationState();
        refreshHomeTimeLabels();
        homeTimeRefreshTimer.play();
    }
    public void selectSetupTab() { selectStage(fencersTab); }
    public void selectPoolsTab() { selectStage(poolsTab); }
    public void selectPoolResultTab() { selectStage(standingsTab); }
    public void selectEliminationTab() { selectStage(eliminationTab); }
    public void selectFinalResultsTab() { selectStage(finalResultsTab); }
    public Button setupNavigationButton() { return setupNavigationButton; }
    public Button poolsNavigationButton() { return poolsNavigationButton; }
    /** Selects the first workspace tab relevant to the opened tournament's current phase. */
    public void selectTabForPhase(TournamentPhase phase) {
        switch (phase) {
        case REGISTRATION, SEEDING -> selectStage(fencersTab);
        case POOL_PHASE -> selectStage(poolsTab);
        case ELIMINATION_PHASE -> selectStage(eliminationTab);
        case COMPLETE -> selectStage(finalResultsTab);
        }
    }
    public void clearPoolWorkspace() {
        poolList.getItems().clear(); selectedPoolLabel.setText("Select a pool");
        poolProgressLabel.setText(""); poolDashboard.getChildren().clear(); selectedMatrixRow = null; selectedMatrixOpponent = null;
        selectedMatrixPool = null; renderedPoolPanels = List.of(); showSelectedBout(null);
    }
    public void setTournamentOpenHandler(Consumer<UUID> handler) { tournamentOpenHandler = handler == null ? ignored -> { } : handler; }
    public void setTournamentDeleteHandler(Consumer<UUID> handler) { tournamentDeleteHandler = handler == null ? ignored -> { } : handler; }
    public void showNewTournamentForm(boolean show) {
        homeCreateForm.setVisible(show);
        homeCreateForm.setManaged(show);
        if (!show) clearHomeTournamentNameValidationError();
        if (show) homeTournamentNameField.requestFocus();
    }
    public void renderTournamentList(List<Tournament> tournaments) {
        List<Tournament> ongoing = orderByMostRecentlyModified(tournaments.stream()
                .filter(tournament -> tournament.phase() != TournamentPhase.COMPLETE).toList());
        List<Tournament> completed = orderByCompletionTime(tournaments.stream()
                .filter(tournament -> tournament.phase() == TournamentPhase.COMPLETE).toList());
        homeTournamentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        homeTournamentScroll.setVbarPolicy(tournaments.size() <= 2
                ? ScrollPane.ScrollBarPolicy.NEVER : ScrollPane.ScrollBarPolicy.AS_NEEDED);
        homeTournamentRows.getChildren().clear();
        homeTimestampLabels.clear();
        if (tournaments.isEmpty()) {
            Label emptyTitle = new Label("No tournaments yet."); emptyTitle.getStyleClass().add("home-empty-title");
            Label emptyText = new Label("Create your first tournament to get started."); emptyText.getStyleClass().add("screen-subtitle");
            homeTournamentRows.getChildren().add(new VBox(4, emptyTitle, emptyText));
            return;
        }
        if (!ongoing.isEmpty()) addHomeSection("ONGOING", ongoing);
        if (!completed.isEmpty()) addHomeSection("COMPLETED", completed);
    }
    public TextField fencerNameField() { return fencerNameField; }
    public Button addFencerButton() { return addFencerButton; }
    public Label fencerValidationErrorLabel() { return fencerValidationErrorLabel; }
    public ListView<Fencer> seedList() { return seedList; }
    public Button generatePoolsButton() { return generatePoolsButton; }
    public Button editSetupButton() { return editSetupButton; }
    public ComboBox<Integer> maximumPoolSizeChoice() { return maximumPoolSizeChoice; }
    public Label seedingValidationErrorLabel() { return seedingValidationErrorLabel; }
    public TabPane tabs() { return tabs; }
    public Tab poolsTab() { return poolsTab; }
    public Tab standingsTab() { return standingsTab; }
    public Tab eliminationTab() { return eliminationTab; }
    public Tab finalResultsTab() { return finalResultsTab; }
    public Button generateEliminationButton() { return generateEliminationButton; }
    public TextField eliminationFirstScoreField() { return eliminationFirstScoreField; }
    public TextField eliminationSecondScoreField() { return eliminationSecondScoreField; }
    public Button recordEliminationResultButton() { return recordEliminationResultButton; }
    public Button editEliminationResultButton() { return editEliminationResultButton; }
    public Button cancelEliminationEditButton() { return cancelEliminationEditButton; }
    public ListView<Pool> poolList() { return poolList; }
    public TextField firstScoreField() { return firstScoreField; }
    public TextField secondScoreField() { return secondScoreField; }
    public Button recordResultButton() { return recordResultButton; }
    public Button editPoolResultButton() { return editPoolResultButton; }
    public Label poolValidationErrorLabel() { return poolValidationErrorLabel; }
    public Label eliminationValidationErrorLabel() { return eliminationValidationErrorLabel; }
    public GridPane poolDashboard() { return poolDashboard; }
    public void setMatrixCellHandler(Consumer<PoolMatrixSelection> handler) { matrixCellHandler = handler == null ? ignored -> { } : handler; }
    public void setSeedMoveHandler(BiConsumer<UUID, Integer> handler) { seedMoveHandler = handler == null ? (fencerId, targetIndex) -> { } : handler; }
    public void setFencerRemoveHandler(Consumer<UUID> handler) { fencerRemoveHandler = handler == null ? ignored -> { } : handler; }
    public void setPoolSelectionDismissHandler(Runnable handler) { poolSelectionDismissHandler = handler == null ? () -> { } : handler; }
    public void markSelectedMatrixCell(UUID poolId, UUID row, UUID opponent) {
        selectedMatrixPool = poolId;
        selectedMatrixRow = row;
        selectedMatrixOpponent = opponent;
        renderPoolDashboard(renderedPoolPanels);
    }
    public void clearSelectedMatrixCell() {
        selectedMatrixPool = null;
        selectedMatrixRow = null;
        selectedMatrixOpponent = null;
        renderPoolDashboard(renderedPoolPanels);
    }
    public void setEliminationMatchHandler(Consumer<UUID> handler) { eliminationMatchHandler = handler == null ? ignored -> { } : handler; }

    public void renderFencers(List<Fencer> fencers, List<Fencer> seedOrder) {
        seedList.getItems().setAll(seedOrder);
        registeredFencerCountLabel.setText(fencers.size() + (fencers.size() == 1 ? " fencer" : " fencers"));
    }
    public void renderPools(List<Pool> pools) {
        Pool selected = poolList.getSelectionModel().getSelectedItem();
        poolList.getItems().setAll(pools);
        if (!pools.isEmpty()) poolList.getSelectionModel().select(Math.max(0, selected == null ? 0 : indexOfPool(pools, selected.id())));
    }
    public void renderSelectedPool(String poolName, List<String> members, List<PoolMatrixRow> matrixRows) {
        selectedPoolLabel.setText(poolName);
        poolProgressLabel.setText(members.size() + " fencers");
    }
    /** Renders all pools together so an organiser can scan progress without changing context. */
    public void renderPoolDashboard(List<PoolDashboardPanel> panels) {
        renderedPoolPanels = List.copyOf(panels);
        poolDashboard.getChildren().clear();
        poolDashboard.getColumnConstraints().clear();
        poolDashboard.getRowConstraints().clear();
        PoolLayout.BoardLayout layout = PoolLayout.calculate(panels.stream()
                        .map(panel -> panel.matrixRows().size()).toList(),
                poolDashboardScroll.getViewportBounds().getWidth(), PoolLayout.ORGANISER);
        poolDashboard.setHgap(PoolLayout.ORGANISER.horizontalGap());
        poolDashboard.setVgap(PoolLayout.ORGANISER.horizontalGap());
        poolDashboard.setPrefWidth(layout.usableWidth());
        poolDashboardScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        poolDashboardScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        if (panels.isEmpty()) {
            Label empty = new Label("Pools will appear here after generation.");
            empty.getStyleClass().add("compact-grid-empty");
            poolDashboard.getChildren().add(empty);
            return;
        }
        for (int column = 0; column < layout.columns(); column++) {
            ColumnConstraints constraints = new ColumnConstraints(layout.panelWidth());
            constraints.setMinWidth(layout.panelWidth());
            constraints.setPrefWidth(layout.panelWidth());
            constraints.setMaxWidth(layout.panelWidth());
            poolDashboard.getColumnConstraints().add(constraints);
        }
        for (int index = 0; index < panels.size(); index++) {
            poolDashboard.add(poolPanel(panels.get(index), layout.panelWidth()),
                    index % layout.columns(), index / layout.columns());
        }
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
    }

    public void renderEliminationBracket(List<EliminationMatchRow> matches) {
        renderedEliminationMatches = List.copyOf(matches);
        bracketBoard.getChildren().clear();
        if (matches.isEmpty()) return;
        int finalRound = matches.stream().mapToInt(EliminationMatchRow::round).max().orElse(1);
        activeEliminationRound = activeEliminationRound(matches, finalRound);
        int openingMatchCount = (int) matches.stream().filter(match -> match.round() == 1).count();
        double openingStep = openingStep(openingMatchCount, availableBracketHeight());
        java.util.Map<String, EliminationMatchRow> byPosition = matches.stream().collect(
                java.util.stream.Collectors.toMap(match -> match.round() + ":" + match.position(), match -> match));
        java.util.Map<String, BracketGeometry> geometry = calculateBracketGeometry(matches, byPosition, finalRound, openingStep);
        for (int round = 1; round <= finalRound; round++) {
            int currentRound = round;
            int matchCount = (int) matches.stream().filter(match -> match.round() == currentRound).count();
            Label heading = new Label(roundName(matchCount)); heading.getStyleClass().add("bracket-heading");
            if (round == finalRound) heading.getStyleClass().add("bracket-final-heading");
            if (round == activeEliminationRound) heading.getStyleClass().add("bracket-active-heading");
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
                .mapToDouble(BracketGeometry::centreY).max().orElse(TABLEAU_FIRST_CENTRE) + TABLEAU_CARD_HEIGHT / 2 + TABLEAU_BOTTOM_PADDING;
        double boardWidth = winnerX + 170;
        bracketBoard.setMinSize(boardWidth, boardHeight); bracketBoard.setPrefSize(boardWidth, boardHeight);
        bracketBoard.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        bracketCanvas.setMinSize(boardWidth, boardHeight); bracketCanvas.setPrefSize(boardWidth, boardHeight);
        focusActiveEliminationRound(boardWidth);
    }

    public void showSelectedEliminationMatch(EliminationMatchRow match) {
        endEliminationResultEdit();
        clearEliminationValidationError();
        if (match == null) {
            selectedEliminationMatchId = null;
            selectedEliminationMatchLabel.setText("Select a pending bout in the bracket"); eliminationFirstNameLabel.setText("—"); eliminationSecondNameLabel.setText("—");
            eliminationFirstScoreField.clear(); eliminationSecondScoreField.clear(); eliminationFirstScoreField.setDisable(true); eliminationSecondScoreField.setDisable(true); recordEliminationResultButton.setDisable(true); editEliminationResultButton.setVisible(false); editEliminationResultButton.setManaged(false); return;
        }
        selectedEliminationMatchId = match.matchId();
        if (!renderedEliminationMatches.isEmpty()) renderEliminationBracket(renderedEliminationMatches);
        int matchCount = (int) renderedEliminationMatches.stream().filter(candidate -> candidate.round() == match.round()).count();
        selectedEliminationMatchLabel.setText(roundName(matchCount) + " · selected bout");
        eliminationFirstNameLabel.setText(participantText(match.first())); eliminationSecondNameLabel.setText(participantText(match.second()));
        if (isAvailableEliminationBout(match)) {
            eliminationFirstScoreField.clear(); eliminationSecondScoreField.clear();
            eliminationFirstScoreField.setDisable(false); eliminationSecondScoreField.setDisable(false); recordEliminationResultButton.setDisable(false); editEliminationResultButton.setVisible(false); editEliminationResultButton.setManaged(false);
        } else {
            eliminationFirstScoreField.setText(match.first().score()); eliminationSecondScoreField.setText(match.second().score());
            eliminationFirstScoreField.setDisable(true); eliminationSecondScoreField.setDisable(true); recordEliminationResultButton.setDisable(true);
            boolean editable = match.resolved() && !match.bye();
            editEliminationResultButton.setVisible(editable); editEliminationResultButton.setManaged(editable);
        }
    }
    public void beginEliminationResultEdit(EliminationMatchRow match) {
        clearEliminationValidationError();
        eliminationFirstScoreField.setText(match.first().score()); eliminationSecondScoreField.setText(match.second().score());
        eliminationFirstScoreField.setDisable(false); eliminationSecondScoreField.setDisable(false);
        recordEliminationResultButton.setText("Save changes"); recordEliminationResultButton.setDisable(false);
        cancelEliminationEditButton.setVisible(true); cancelEliminationEditButton.setManaged(true);
        editEliminationResultButton.setVisible(false); editEliminationResultButton.setManaged(false);
        selectedEliminationMatchLabel.setText(selectedEliminationMatchLabel.getText().replace("selected bout", "editing result"));
    }
    public void endEliminationResultEdit() {
        recordEliminationResultButton.setText("Record result");
        cancelEliminationEditButton.setVisible(false); cancelEliminationEditButton.setManaged(false);
        clearEliminationValidationError();
    }
    public void showSelectedBout(PoolBoutRow bout) {
        clearPoolValidationError();
        if (bout == null) {
            firstFencerLabel.setText("—"); secondFencerLabel.setText("—"); resultStateLabel.setText("Select an unfinished bout in the matrix");
            scoreFields.setVisible(false); scoreFields.setManaged(false); recordResultButton.setDisable(true); editPoolResultButton.setVisible(false); editPoolResultButton.setManaged(false); collapsePoolResultEntry(); return;
        }
        expandPoolResultEntry();
        firstFencerLabel.setText(bout.firstName()); secondFencerLabel.setText(bout.secondName());
        if (bout.completed()) {
            resultStateLabel.setText("Completed · " + bout.scoreText()); resultStateLabel.getStyleClass().setAll("result-state", "is-completed");
            scoreFields.setVisible(false); scoreFields.setManaged(false); recordResultButton.setDisable(true); editPoolResultButton.setVisible(true); editPoolResultButton.setManaged(true);
        } else {
            resultStateLabel.setText("Pending result"); resultStateLabel.getStyleClass().setAll("result-state", "is-pending");
            scoreFields.setVisible(true); scoreFields.setManaged(true); firstScoreField.clear(); secondScoreField.clear(); recordResultButton.setDisable(false); editPoolResultButton.setVisible(false); editPoolResultButton.setManaged(false);
        }
    }
    public void beginPoolResultEdit(PoolBoutRow bout) {
        clearPoolValidationError();
        String[] scores = bout.scoreText().split("\\s*-\\s*");
        firstScoreField.setText(scores[0]); secondScoreField.setText(scores[1]); scoreFields.setVisible(true); scoreFields.setManaged(true);
        recordResultButton.setText("Save correction"); recordResultButton.setDisable(false); editPoolResultButton.setVisible(false); editPoolResultButton.setManaged(false);
        resultStateLabel.setText("Editing recorded result"); resultStateLabel.getStyleClass().setAll("result-state", "is-pending");
    }
    public void endPoolResultEdit() {
        recordResultButton.setText("Record result");
        clearPoolValidationError();
    }
    public void showPoolValidationError(String message, boolean firstFieldInvalid, boolean secondFieldInvalid) {
        showScoreValidationError(poolValidationErrorLabel, firstScoreField, secondScoreField,
                message, firstFieldInvalid, secondFieldInvalid);
    }
    public void showEliminationValidationError(String message, boolean firstFieldInvalid, boolean secondFieldInvalid) {
        showScoreValidationError(eliminationValidationErrorLabel, eliminationFirstScoreField, eliminationSecondScoreField,
                message, firstFieldInvalid, secondFieldInvalid);
    }
    public void clearPoolValidationError() {
        clearScoreValidationError(poolValidationErrorLabel, firstScoreField, secondScoreField);
    }
    public void clearEliminationValidationError() {
        clearScoreValidationError(eliminationValidationErrorLabel, eliminationFirstScoreField, eliminationSecondScoreField);
    }
    public void showTournamentNameValidationError(String message) {
        showFieldValidationError(tournamentNameValidationErrorLabel, tournamentNameField, message);
    }
    public void clearTournamentNameValidationError() {
        clearFieldValidationError(tournamentNameValidationErrorLabel, tournamentNameField);
    }
    public void showHomeTournamentNameValidationError(String message) {
        showFieldValidationError(homeTournamentNameValidationErrorLabel, homeTournamentNameField, message);
    }
    public void clearHomeTournamentNameValidationError() {
        clearFieldValidationError(homeTournamentNameValidationErrorLabel, homeTournamentNameField);
    }
    public void showFencerValidationError(String message, boolean markNameField) {
        showFieldValidationError(fencerValidationErrorLabel, markNameField ? fencerNameField : null, message);
    }
    public void clearFencerValidationError() {
        clearFieldValidationError(fencerValidationErrorLabel, fencerNameField);
    }
    public void showSeedingValidationError(String message) {
        seedingValidationErrorLabel.setText(message);
        seedingValidationErrorLabel.setVisible(true);
        seedingValidationErrorLabel.setManaged(true);
        if (!maximumPoolSizeChoice.getStyleClass().contains("input-invalid")) {
            maximumPoolSizeChoice.getStyleClass().add("input-invalid");
        }
        maximumPoolSizeChoice.requestFocus();
    }
    public void clearSeedingValidationError() {
        seedingValidationErrorLabel.setText("");
        seedingValidationErrorLabel.setVisible(false);
        seedingValidationErrorLabel.setManaged(false);
        maximumPoolSizeChoice.getStyleClass().remove("input-invalid");
    }
    private static void showFieldValidationError(Label errorLabel, TextField field, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        if (field != null) {
            markInputInvalid(field, true);
            field.requestFocus();
        }
    }
    private static void clearFieldValidationError(Label errorLabel, TextField field) {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        if (field != null) markInputInvalid(field, false);
    }
    private static void showScoreValidationError(Label errorLabel, TextField firstField, TextField secondField,
            String message, boolean firstFieldInvalid, boolean secondFieldInvalid) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        markScoreFieldInvalid(firstField, firstFieldInvalid);
        markScoreFieldInvalid(secondField, secondFieldInvalid);
        if (firstFieldInvalid) firstField.requestFocus();
        else if (secondFieldInvalid) secondField.requestFocus();
    }
    private static void clearScoreValidationError(Label errorLabel, TextField firstField, TextField secondField) {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        markScoreFieldInvalid(firstField, false);
        markScoreFieldInvalid(secondField, false);
    }
    private static void markScoreFieldInvalid(TextField field, boolean invalid) {
        markInputInvalid(field, invalid);
    }
    private static void markInputInvalid(TextField field, boolean invalid) {
        if (invalid) {
            if (!field.getStyleClass().contains("input-invalid")) field.getStyleClass().add("input-invalid");
        } else field.getStyleClass().remove("input-invalid");
    }
    private void collapsePoolResultEntry() {
        resultEntry.getChildren().setAll(resultStateLabel);
        resultEntry.setAlignment(Pos.CENTER_LEFT);
        resultEntry.getStyleClass().add("result-entry-compact");
    }
    private void expandPoolResultEntry() {
        resultEntry.getChildren().setAll(poolResultHeading, poolResultNames, resultStateLabel, scoreFields,
                editPoolResultButton, poolValidationErrorLabel);
        resultEntry.setAlignment(Pos.CENTER);
        resultEntry.getStyleClass().remove("result-entry-compact");
    }
    public void showPhase(TournamentPhase phase, PoolProgress progress) {
        phaseLabel.setText(phaseText(phase));
        progressLabel.setText(progress == null || progress.totalBouts() == 0 ? "" : progress.completedBouts() + " of " + progress.totalBouts() + " pool bouts complete");
        showTournamentContext(true);
    }
    public void showStatus(String message) { statusLabel.setText(message == null ? "" : message); }
    public void setPhaseControls(TournamentPhase phase, boolean hasTournament, boolean poolResultsFinalized, boolean hasEliminationBracket) {
        controlledPhase = phase;
        controlledHasTournament = hasTournament;
        boolean setup = hasTournament && (phase == TournamentPhase.REGISTRATION || phase == TournamentPhase.SEEDING);
        boolean pools = hasTournament && (phase == TournamentPhase.POOL_PHASE || phase == TournamentPhase.ELIMINATION_PHASE || phase == TournamentPhase.COMPLETE);
        refreshSetupEditability();
        showOnly(seedingSection, false);
        fencerNameField.setDisable(!setup); addFencerButton.setDisable(!setup); seedList.setDisable(!setup);
        maximumPoolSizeChoice.setDisable(!setup); generatePoolsButton.setDisable(!setup || seedList.getItems().size() < 2); poolsTab.setDisable(!pools); standingsTab.setDisable(!poolResultsFinalized); eliminationTab.setDisable(!hasEliminationBracket); finalResultsTab.setDisable(phase != TournamentPhase.COMPLETE); generateEliminationButton.setDisable(!poolResultsFinalized || hasEliminationBracket);
        setupNavigationButton.setDisable(!hasTournament); poolsNavigationButton.setDisable(!pools); standingsNavigationButton.setDisable(!poolResultsFinalized); eliminationNavigationButton.setDisable(!hasEliminationBracket); finalResultsNavigationButton.setDisable(phase != TournamentPhase.COMPLETE);
        tournamentProgressStage = phase == TournamentPhase.COMPLETE ? 4
                : hasEliminationBracket ? 3 : poolResultsFinalized ? 2 : pools ? 1 : 0;
        stageProgressRow.setVisible(hasTournament); stageProgressRow.setManaged(hasTournament);
        refreshNavigationState();
    }

    private void refreshSetupEditability() {
        boolean setupEditable = controlledHasTournament
                && (controlledPhase == TournamentPhase.REGISTRATION || controlledPhase == TournamentPhase.SEEDING);
        boolean viewingHistoricalSetup = controlledHasTournament && activeStage == fencersTab && !setupEditable;
        showOnly(createTournamentSection, !controlledHasTournament);
        showOnly(registrationSection, setupEditable || viewingHistoricalSetup);
        fencerNameField.setDisable(!setupEditable); addFencerButton.setDisable(!setupEditable); seedList.setDisable(!setupEditable);
        maximumPoolSizeChoice.setDisable(!setupEditable);
        generatePoolsButton.setDisable(!setupEditable || seedList.getItems().size() < 2);
        showOnly(setupReadOnlyLabel, viewingHistoricalSetup);
        showOnly(editSetupButton, viewingHistoricalSetup);
    }
    public void setNoTournamentState() {
        tournamentNameLabel.setText(""); phaseLabel.setText(""); progressLabel.setText(""); showTournamentContext(false);
        seedList.getItems().clear(); clearPoolWorkspace(); standingsGrid.getChildren().clear(); finalResultsGrid.getChildren().clear(); showSelectedBout(null);
        setPhaseControls(TournamentPhase.REGISTRATION, false, false, false);
        showHome();
    }
    public void showTournamentName(String name) { tournamentNameLabel.setText(name); showTournamentContext(true); }

    private VBox buildHeader() {
        Label appName = new Label("Fencing Tournament Manager"); appName.getStyleClass().add("app-name"); tournamentNameLabel.getStyleClass().add("tournament-name"); phaseLabel.getStyleClass().add("phase-name"); progressLabel.getStyleClass().add("progress-text");
        loadButton.getStyleClass().add("secondary-action");
        loadButton.setText("Open File");
        tournamentContext.getChildren().setAll(tournamentNameLabel, phaseLabel, progressLabel); tournamentContext.getStyleClass().add("tournament-context");
        HBox identity = new HBox(appName); identity.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(8, loadButton); actions.setAlignment(Pos.CENTER_RIGHT);
        HBox bar = new HBox(22, identity, spacer, tournamentContext, actions); bar.setAlignment(Pos.CENTER_LEFT); bar.getStyleClass().add("top-bar");
        buildStageProgress();
        return new VBox(bar, stageProgressRow);
    }
    private VBox buildSidebar() {
        Label homeLabel = new Label("WORKSPACE"); homeLabel.getStyleClass().add("sidebar-label");
        homeButton.getStyleClass().setAll("sidebar-nav-button");
        VBox navigation = new VBox(4, homeLabel, homeButton);
        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);
        VBox sidebarSettings = new VBox(14, buildAppearanceSelector(), buildThemeSelector());
        sidebarSettings.getStyleClass().add("sidebar-settings");
        VBox sidebar = new VBox(4, navigation, spacer, sidebarSettings);
        sidebar.getStyleClass().add("sidebar");
        return sidebar;
    }
    private VBox buildAppearanceSelector() {
        Label label = new Label("APPEARANCE");
        label.getStyleClass().add("sidebar-label");
        Label lightLabel = new Label("Light");
        Label darkLabel = new Label("Dark");
        lightLabel.getStyleClass().add("appearance-toggle-label");
        darkLabel.getStyleClass().add("appearance-toggle-label");
        darkAppearanceToggle.setAccessibleText("Toggle dark appearance");
        darkAppearanceToggle.setTooltip(new Tooltip("Switch between Light and Dark appearance"));
        darkAppearanceToggle.getStyleClass().add("appearance-switch-control");
        appearanceToggleThumb.getStyleClass().add("appearance-switch-thumb");
        appearanceToggleThumb.setMouseTransparent(true);
        StackPane switchTrack = new StackPane(darkAppearanceToggle, appearanceToggleThumb);
        switchTrack.getStyleClass().add("appearance-switch");
        StackPane.setMargin(appearanceToggleThumb, new Insets(3));
        darkAppearanceToggle.setOnAction(event -> {
            UiTheme.selectAppearance(darkAppearanceToggle.isSelected()
                    ? UiTheme.Appearance.DARK : UiTheme.Appearance.LIGHT);
            UiTheme.apply(this);
            refreshAppearanceToggle();
        });
        appearanceToggle.setAlignment(Pos.CENTER_LEFT);
        appearanceToggle.getStyleClass().add("appearance-toggle");
        appearanceToggle.getChildren().setAll(lightLabel, switchTrack, darkLabel);
        refreshAppearanceToggle();
        return new VBox(7, label, appearanceToggle);
    }
    private VBox buildThemeSelector() {
        Label label = new Label("COLOUR THEME");
        label.getStyleClass().add("sidebar-label");
        themeSwatches.getChildren().clear();
        themeSwatchButtons.clear();
        for (UiTheme.Theme theme : UiTheme.Theme.values()) {
            Button swatch = new Button();
            swatch.setAccessibleText(theme.displayName());
            swatch.setTooltip(new Tooltip(theme.displayName()));
            swatch.getStyleClass().addAll("theme-swatch", "theme-swatch-" + theme.name().toLowerCase().replace('_', '-'));
            swatch.setOnAction(event -> {
                UiTheme.selectTheme(theme);
                UiTheme.apply(this);
                refreshThemeSwatches();
            });
            themeSwatchButtons.add(swatch);
            themeSwatches.getChildren().add(swatch);
        }
        themeSwatches.getStyleClass().add("theme-swatches");
        refreshThemeSwatches();
        VBox appearance = new VBox(7, label, themeSwatches);
        appearance.getStyleClass().add("sidebar-appearance");
        return appearance;
    }
    private void refreshThemeSwatches() {
        for (int index = 0; index < themeSwatchButtons.size(); index++) {
            Button swatch = themeSwatchButtons.get(index);
            UiTheme.Theme theme = UiTheme.Theme.values()[index];
            if (theme == UiTheme.selectedTheme()) {
                if (!swatch.getStyleClass().contains("theme-swatch-selected")) swatch.getStyleClass().add("theme-swatch-selected");
            } else {
                swatch.getStyleClass().remove("theme-swatch-selected");
            }
        }
    }
    private void refreshAppearanceToggle() {
        boolean dark = UiTheme.selectedAppearance() == UiTheme.Appearance.DARK;
        darkAppearanceToggle.setSelected(dark);
        StackPane.setAlignment(appearanceToggleThumb, dark ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        appearanceToggle.lookupAll(".appearance-toggle-label").forEach(node -> {
            node.getStyleClass().remove("appearance-toggle-label-selected");
        });
        String selectedLabel = dark ? "Dark" : "Light";
        appearanceToggle.lookupAll(".appearance-toggle-label").stream()
                .filter(Label.class::isInstance).map(Label.class::cast)
                .filter(candidate -> candidate.getText().equals(selectedLabel))
                .findFirst().ifPresent(candidate -> candidate.getStyleClass().add("appearance-toggle-label-selected"));
    }
    private void buildStageProgress() {
        List<Button> buttons = List.of(setupNavigationButton, poolsNavigationButton, standingsNavigationButton,
                eliminationNavigationButton, finalResultsNavigationButton);
        List<Tab> stages = List.of(fencersTab, poolsTab, standingsTab, eliminationTab, finalResultsTab);
        List<String> labels = List.of("Setup", "Pools", "Pool Result", "Direct Elimination", "Final Results");
        stageProgress.getChildren().clear();
        stageProgressLabels.clear();
        stageProgressSteps.clear();
        stageProgressConnectors.clear();
        stageProgress.setAlignment(Pos.CENTER);
        for (int index = 0; index < buttons.size(); index++) {
            int stageIndex = index;
            Button button = buttons.get(index);
            button.setText("");
            button.setAccessibleText(labels.get(index) + " stage");
            // Preserve JavaFX's base "button" class: the circle CSS intentionally
            // composes it with stage-marker (".button.stage-marker").
            button.getStyleClass().setAll("button", "stage-marker");
            button.setOnAction(event -> selectStage(stages.get(stageIndex)));
            Label label = new Label(labels.get(index));
            label.getStyleClass().add("stage-progress-label");
            stageProgressLabels.add(label);
            StackPane markerSlot = new StackPane(button);
            markerSlot.setMinSize(24, 24);
            markerSlot.setPrefSize(24, 24);
            markerSlot.setMaxSize(24, 24);
            VBox step = new VBox(4, markerSlot, label);
            step.setMinSize(108, 50);
            step.setPrefSize(108, 50);
            step.setMaxSize(108, 50);
            step.setAlignment(Pos.TOP_CENTER);
            step.getStyleClass().add("stage-progress-step");
            step.setOnMouseClicked(event -> {
                if (!button.isDisabled()) {
                    selectStage(stages.get(stageIndex));
                }
            });
            stageProgressSteps.add(step);
            stageProgress.getChildren().add(step);
            if (index < buttons.size() - 1) {
                Region connector = new Region();
                connector.getStyleClass().add("stage-progress-connector");
                connector.setMinWidth(192);
                connector.setPrefWidth(192);
                connector.setMaxWidth(192);
                StackPane connectorSlot = new StackPane(connector);
                connectorSlot.setMinSize(108, 50);
                connectorSlot.setPrefSize(108, 50);
                connectorSlot.setMaxSize(108, 50);
                connectorSlot.getStyleClass().add("stage-progress-connector-slot");
                stageProgressConnectors.add(connector);
                stageProgress.getChildren().add(connectorSlot);
            }
        }
        stageProgressRow.setAlignment(Pos.CENTER);
        stageProgressRow.getStyleClass().add("stage-progress-row");
    }
    private List<Button> stageNavigationButtons() {
        return List.of(setupNavigationButton, poolsNavigationButton, standingsNavigationButton,
                eliminationNavigationButton, finalResultsNavigationButton);
    }
    private List<Tab> navigationStages() {
        return List.of(fencersTab, poolsTab, standingsTab, eliminationTab, finalResultsTab);
    }
    private void refreshStageProgress() {
        List<Button> buttons = stageNavigationButtons();
        List<Tab> stages = navigationStages();
        for (int index = 0; index < buttons.size(); index++) {
            Button button = buttons.get(index);
            Label label = stageProgressLabels.get(index);
            VBox step = stageProgressSteps.get(index);
            button.setText("");
            button.getStyleClass().removeAll("stage-marker-complete", "stage-marker-current", "stage-marker-locked");
            label.getStyleClass().removeAll("stage-progress-complete", "stage-progress-current", "stage-progress-locked", "stage-progress-viewing");
            step.getStyleClass().removeAll("stage-progress-step-accessible", "stage-progress-step-locked");
            if (index < tournamentProgressStage) {
                button.getStyleClass().add("stage-marker-complete");
                label.getStyleClass().add("stage-progress-complete");
            } else if (index == tournamentProgressStage) {
                button.getStyleClass().add("stage-marker-current");
                label.getStyleClass().add("stage-progress-current");
            } else {
                button.getStyleClass().add("stage-marker-locked");
                label.getStyleClass().add("stage-progress-locked");
            }
            step.getStyleClass().add(button.isDisabled()
                    ? "stage-progress-step-locked" : "stage-progress-step-accessible");
            if (activeStage == stages.get(index)) {
                label.getStyleClass().add("stage-progress-viewing");
            }
        }
        for (int index = 0; index < stageProgressConnectors.size(); index++) {
            Region connector = stageProgressConnectors.get(index);
            connector.getStyleClass().removeAll("stage-progress-connector-complete", "stage-progress-connector-locked");
            connector.getStyleClass().add(index < tournamentProgressStage
                    ? "stage-progress-connector-complete" : "stage-progress-connector-locked");
        }
    }
    private void selectStage(Tab stage) {
        homeTimeRefreshTimer.stop();
        activeStage = stage;
        tabs.getSelectionModel().select(stage);
        stageContent.getChildren().setAll(stage.getContent());
        refreshSetupEditability();
        refreshNavigationState();
        Platform.runLater(this::refreshSizeDependentStageLayout);
    }
    private void refreshSizeDependentStageLayout() {
        if (activeStage == poolsTab) {
            double viewportWidth = poolDashboardScroll.getViewportBounds().getWidth();
            if (viewportWidth > 0) {
            if (!renderedPoolPanels.isEmpty()) renderPoolDashboard(renderedPoolPanels);
            }
        }
        if (activeStage == eliminationTab && !renderedEliminationMatches.isEmpty()) {
            requestEliminationRelayout();
        }
    }
    private void refreshNavigationState() {
        setNavigationSelected(homeButton, activeStage == null);
        refreshStageProgress();
    }
    private void showTournamentContext(boolean visible) {
        tournamentContext.setVisible(visible);
        tournamentContext.setManaged(visible);
    }
    private static void setNavigationSelected(Button button, boolean selected) {
        if (selected && !button.getStyleClass().contains("sidebar-nav-selected")) button.getStyleClass().add("sidebar-nav-selected");
        if (!selected) button.getStyleClass().remove("sidebar-nav-selected");
    }
    private VBox buildHomeScreen() {
        Label title = new Label("Tournaments"); title.getStyleClass().add("home-title");
        Label subtitle = new Label("Manage ongoing and past club competitions."); subtitle.getStyleClass().add("screen-subtitle");
        homeNewButton.getStyleClass().add("primary-action"); homeCreateButton.getStyleClass().add("primary-action"); homeCancelButton.getStyleClass().add("secondary-action");
        HBox heading = new HBox(title, homeNewButton); heading.setAlignment(Pos.CENTER_LEFT); heading.setSpacing(20); heading.setMaxWidth(Double.MAX_VALUE); heading.getStyleClass().add("page-title-row"); HBox.setHgrow(title, Priority.ALWAYS);
        homeTournamentNameField.setPromptText("Tournament name"); homeTournamentNameField.setOnAction(event -> homeCreateButton.fire());
        HBox formActions = new HBox(8, homeCreateButton, homeCancelButton); HBox form = new HBox(10, homeTournamentNameField, formActions); HBox.setHgrow(homeTournamentNameField, Priority.ALWAYS);
        configureInlineValidationLabel(homeTournamentNameValidationErrorLabel, 520);
        homeCreateForm.getChildren().setAll(form, homeTournamentNameValidationErrorLabel); homeCreateForm.getStyleClass().add("home-create-form");
        homeTournamentRows.setSpacing(6); homeTournamentScroll.setFitToWidth(true); homeTournamentScroll.setFitToHeight(false); homeTournamentScroll.setPannable(true); homeTournamentScroll.getStyleClass().add("home-tournament-scroll");
        VBox content = new VBox(8, heading, subtitle, homeCreateForm, homeTournamentScroll); content.getStyleClass().add("home-content"); VBox.setVgrow(homeTournamentScroll, Priority.ALWAYS);
        homeScreen.getChildren().setAll(content); homeScreen.setAlignment(Pos.TOP_CENTER); homeScreen.getStyleClass().add("home-screen"); showNewTournamentForm(false); return homeScreen;
    }
    private void addHomeSection(String title, List<Tournament> tournaments) {
        Label heading = new Label(title); heading.getStyleClass().add("home-section-heading"); homeTournamentRows.getChildren().add(heading);
        tournaments.forEach(tournament -> homeTournamentRows.getChildren().add(homeTournamentRow(tournament)));
    }
    private GridPane homeTournamentRow(Tournament tournament) {
        Label name = new Label(tournament.name()); name.getStyleClass().add("home-tournament-name"); name.setWrapText(true);
        Label status = new Label(phaseText(tournament.phase())); status.getStyleClass().addAll("home-status", tournament.phase() == TournamentPhase.COMPLETE ? "home-status-complete" : "home-status-ongoing");
        Label metadata = new Label(tournament.fencers().size() + " fencers"); metadata.getStyleClass().add("home-tournament-meta");
        HBox details = new HBox(10, status, metadata); details.setAlignment(Pos.CENTER_LEFT);
        VBox summary = new VBox(4, name, details); summary.setMinWidth(0);
        Label updated = new Label(homeTimestampText(tournament)); updated.getStyleClass().add("home-tournament-updated");
        homeTimestampLabels.add(new HomeTimestampLabel(updated, tournament));
        updated.setMinWidth(112); updated.setMaxWidth(145); updated.setWrapText(true); updated.setTextAlignment(javafx.scene.text.TextAlignment.RIGHT);
        Button open = new Button(tournament.phase() == TournamentPhase.COMPLETE ? "View Results" : "Open"); open.getStyleClass().add(tournament.phase() == TournamentPhase.COMPLETE ? "home-view-action" : "primary-action"); open.setOnAction(event -> tournamentOpenHandler.accept(tournament.id()));
        MenuItem delete = new MenuItem("Delete"); delete.getStyleClass().add("menu-danger-action"); delete.setOnAction(event -> tournamentDeleteHandler.accept(tournament.id()));
        ContextMenu overflowMenu = new ContextMenu(delete); overflowMenu.getStyleClass().add("home-overflow-menu-popup");
        Button overflow = new Button("…"); overflow.getStyleClass().add("home-overflow-menu"); overflow.setAccessibleText("More actions");
        overflow.setOnAction(event -> overflowMenu.show(overflow, javafx.geometry.Side.BOTTOM, 0, 0));
        HBox actions = new HBox(10, updated, open, overflow); actions.setAlignment(Pos.CENTER_RIGHT);
        GridPane row = new GridPane();
        ColumnConstraints summaryColumn = new ColumnConstraints(); summaryColumn.setHgrow(Priority.ALWAYS); summaryColumn.setMinWidth(0);
        row.getColumnConstraints().setAll(summaryColumn, new ColumnConstraints());
        row.add(summary, 0, 0); row.add(actions, 1, 0);
        row.setHgap(18); row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add(tournament.phase() == TournamentPhase.COMPLETE ? "home-tournament-row-complete" : "home-tournament-row");
        return row;
    }

    private static String homeTimestampText(Tournament tournament) {
        return tournament.phase() == TournamentPhase.COMPLETE
                ? formatCompletedAt(tournament.completedAt().orElse(tournament.lastModified()))
                : formatLastModified(tournament.lastModified());
    }

    private void refreshHomeTimeLabels() {
        if (activeStage != null) return;
        homeTimestampLabels.forEach(item -> item.label().setText(homeTimestampText(item.tournament())));
    }

    private record HomeTimestampLabel(Label label, Tournament tournament) {
    }

    static List<Tournament> orderByMostRecentlyModified(List<Tournament> tournaments) {
        return tournaments.stream()
                .sorted(Comparator.comparing(Tournament::lastModified).reversed()
                        .thenComparing(Tournament::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    static List<Tournament> orderByCompletionTime(List<Tournament> tournaments) {
        return tournaments.stream()
                .sorted(Comparator.comparing((Tournament tournament) -> tournament.completedAt().orElse(Instant.EPOCH)).reversed()
                        .thenComparing(Tournament::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    static String formatLastModified(Instant timestamp) {
        return formatLastModified(timestamp, Instant.now(), ZoneId.systemDefault());
    }

    static String formatLastModified(Instant timestamp, Instant now, ZoneId zoneId) {
        if (timestamp == null) return "Updated before tracking";
        Duration age = Duration.between(timestamp, now);
        if (age.isNegative() || age.compareTo(Duration.ofMinutes(1)) < 0) return "Updated just now";
        LocalDate date = timestamp.atZone(zoneId).toLocalDate();
        LocalDate today = now.atZone(zoneId).toLocalDate();
        if (date.equals(today.minusDays(1))) return "Updated yesterday";
        if (age.compareTo(Duration.ofHours(1)) < 0) return "Updated " + age.toMinutes() + " min ago";
        if (date.equals(today)) return "Updated " + age.toHours() + "h ago";
        String pattern = date.getYear() == today.getYear() ? "d MMM" : "d MMM uuuu";
        return "Updated " + DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH).withZone(zoneId).format(timestamp);
    }

    static String formatCompletedAt(Instant timestamp) {
        return formatCompletedAt(timestamp, Instant.now(), ZoneId.systemDefault());
    }

    static String formatCompletedAt(Instant timestamp, Instant now, ZoneId zoneId) {
        LocalDate date = timestamp.atZone(zoneId).toLocalDate();
        LocalDate today = now.atZone(zoneId).toLocalDate();
        String pattern = date.getYear() == today.getYear() ? "d MMM" : "d MMM uuuu";
        return "Completed " + DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH).withZone(zoneId).format(timestamp);
    }
    private VBox buildSetupTab() {
        tournamentNameField.setPromptText("Tournament name, e.g. Friday Internal Open"); tournamentNameField.setOnAction(event -> createButton.fire()); createButton.getStyleClass().add("primary-action");
        configureInlineValidationLabel(tournamentNameValidationErrorLabel, 520);
        createTournamentSection.getChildren().setAll(sectionTitle("Start a tournament", "Create a local tournament, or open one you saved earlier."), formRow(tournamentNameField, createButton), tournamentNameValidationErrorLabel); createTournamentSection.getStyleClass().add("setup-empty-state");
        fencerNameField.setPromptText("Fencer name");
        fencerNameField.setOnAction(event -> addFencerButton.fire());
        addFencerButton.getStyleClass().add("primary-action");
        Label addLabel = new Label("FENCER NAME"); addLabel.getStyleClass().add("field-label");
        HBox addForm = new HBox(10, addLabel, fencerNameField, addFencerButton); addForm.getStyleClass().add("registration-add-form"); HBox.setHgrow(fencerNameField, Priority.ALWAYS);
        configureInlineValidationLabel(fencerValidationErrorLabel, 620);
        VBox addFencerForm = new VBox(4, addForm, fencerValidationErrorLabel);
        Label rosterTitle = new Label("SEED ORDER"); rosterTitle.getStyleClass().add("section-kicker"); registeredFencerCountLabel.getStyleClass().add("registration-count");
        HBox rosterHeading = new HBox(10, rosterTitle, registeredFencerCountLabel); rosterHeading.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(registeredFencerCountLabel, Priority.ALWAYS);
        Label noFencers = new Label("No fencers registered yet.\nAdd the first fencer above."); noFencers.getStyleClass().add("setup-empty-list"); noFencers.setWrapText(true);
        seedList.setPlaceholder(noFencers); seedList.getStyleClass().addAll("seed-list", "setup-seed-list");
        seedList.setFixedCellSize(42); seedList.setMinHeight(252); seedList.setPrefHeight(336); seedList.setMaxHeight(420);
        VBox.setVgrow(seedList, Priority.ALWAYS);
        VBox roster = new VBox(6, rosterHeading, seedList); roster.getStyleClass().add("registration-roster");
        VBox.setVgrow(roster, Priority.ALWAYS);
        generatePoolsButton.getStyleClass().add("primary-action");
        maximumPoolSizeChoice.getItems().setAll(5, 6, 7, 8); maximumPoolSizeChoice.getSelectionModel().select(Integer.valueOf(5)); maximumPoolSizeChoice.setPrefWidth(90);
        HBox poolOptions = new HBox(8, new Label("Maximum fencers per pool"), maximumPoolSizeChoice); poolOptions.setAlignment(Pos.CENTER_LEFT); poolOptions.getStyleClass().add("pool-options");
        configureInlineValidationLabel(seedingValidationErrorLabel, 280);
        HBox setupFooter = new HBox(16, poolOptions, generatePoolsButton); setupFooter.setAlignment(Pos.CENTER_RIGHT); HBox.setHgrow(poolOptions, Priority.ALWAYS); setupFooter.getStyleClass().add("setup-footer");
        editSetupButton.getStyleClass().add("secondary-action");
        setupReadOnlyLabel.getStyleClass().add("setup-read-only-note"); setupReadOnlyLabel.setWrapText(true);
        VBox setupTitle = sectionTitle("Setup", "Build the tournament field and arrange the initial seed order.");
        HBox setupHeading = new HBox(16, setupTitle, editSetupButton); setupHeading.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(setupTitle, Priority.ALWAYS);
        registrationSection.getChildren().setAll(
                setupHeading, setupReadOnlyLabel, addFencerForm, roster, seedingValidationErrorLabel, setupFooter);
        registrationSection.getStyleClass().addAll("setup-stage", "unified-setup-layout"); registrationSection.setMaxWidth(900);
        VBox root = new VBox(createTournamentSection, registrationSection); root.setAlignment(Pos.TOP_CENTER); root.getStyleClass().add("screen-content");
        VBox.setVgrow(registrationSection, Priority.ALWAYS);
        return root;
    }
    private BorderPane buildPoolsTab() {
        Label title = new Label("Pools"); title.getStyleClass().add("screen-title");
        Label description = new Label("Pool board · select a matchup to record or correct its result."); description.getStyleClass().add("screen-subtitle");
        HBox header = new HBox(16, title, description); header.setAlignment(Pos.BASELINE_LEFT); header.getStyleClass().add("pools-page-header");
        poolList.setVisible(false); poolList.setManaged(false);
        poolDashboard.setAlignment(Pos.TOP_LEFT); poolDashboard.setMinWidth(0); poolDashboard.setMaxWidth(Double.MAX_VALUE); poolDashboard.getStyleClass().add("pool-dashboard");
        poolDashboardScroll.setFitToWidth(true); poolDashboardScroll.setFitToHeight(false); poolDashboardScroll.setPannable(true);
        poolDashboardScroll.viewportBoundsProperty().addListener((observable, previous, current) -> {
            if (!renderedPoolPanels.isEmpty()) renderPoolDashboard(renderedPoolPanels);
        });
        poolDashboardScroll.getStyleClass().add("pool-dashboard-scroll");
        buildResultEntry();
        VBox content = new VBox(8, header, poolDashboardScroll, resultEntry);
        content.setAlignment(Pos.TOP_CENTER); content.getStyleClass().add("pools-content"); VBox.setVgrow(poolDashboardScroll, Priority.ALWAYS);
        content.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() != javafx.scene.input.MouseButton.PRIMARY || selectedMatrixRow == null) return;
            Node target = event.getTarget() instanceof Node node ? node : null;
            if (isInside(target, resultEntry) || hasStyleInHierarchy(target, "matrix-pending")
                    || hasStyleInHierarchy(target, "matrix-win") || hasStyleInHierarchy(target, "matrix-loss")) return;
            poolSelectionDismissHandler.run();
        });
        content.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE && selectedMatrixRow != null) {
                poolSelectionDismissHandler.run();
                event.consume();
            }
        });
        return new BorderPane(content);
    }
    private void buildResultEntry() {
        Label title = new Label("RECORD RESULT"); title.getStyleClass().add("section-kicker"); selectedPoolLabel.getStyleClass().add("result-pool-context"); firstFencerLabel.getStyleClass().add("result-fencer"); secondFencerLabel.getStyleClass().add("result-fencer");
        firstScoreField.setPromptText("Score"); secondScoreField.setPromptText("Score"); firstScoreField.getStyleClass().add("score-field"); secondScoreField.getStyleClass().add("score-field"); firstScoreField.setPrefWidth(110); secondScoreField.setPrefWidth(110); recordResultButton.getStyleClass().add("primary-action");
        poolValidationErrorLabel.getStyleClass().add("inline-validation-error");
        poolValidationErrorLabel.setWrapText(true);
        poolValidationErrorLabel.setMaxWidth(420);
        poolValidationErrorLabel.setVisible(false); poolValidationErrorLabel.setManaged(false);
        Label dash = new Label("—"); dash.getStyleClass().add("score-dash"); dash.setMinWidth(23); dash.setPrefWidth(23); dash.setMaxWidth(23); dash.setAlignment(Pos.CENTER);
        Label versus = new Label("vs"); versus.getStyleClass().add("result-versus"); versus.setMinWidth(23); versus.setPrefWidth(23); versus.setMaxWidth(23); versus.setAlignment(Pos.CENTER);
        poolResultNames.getChildren().setAll(firstFencerLabel, versus, secondFencerLabel); poolResultNames.setSpacing(14); poolResultNames.setAlignment(Pos.CENTER);
        HBox scoreLine = new HBox(14, firstScoreField, dash, secondScoreField); scoreLine.setAlignment(Pos.CENTER);
        scoreFields.getChildren().setAll(scoreLine, recordResultButton); scoreFields.setSpacing(8); scoreFields.setAlignment(Pos.CENTER); editPoolResultButton.getStyleClass().add("secondary-action");
        poolResultHeading.getChildren().setAll(selectedPoolLabel, title); poolResultHeading.setAlignment(Pos.CENTER); poolResultHeading.setSpacing(2);
        expandPoolResultEntry(); resultEntry.getStyleClass().add("result-entry");
    }
    private VBox buildStandingsTab() {
        Label title = new Label("Pool Result"); title.getStyleClass().add("screen-title"); Label description = new Label("Overall placing after every pool bout has been finalized."); description.getStyleClass().add("screen-subtitle"); generateEliminationButton.getStyleClass().add("primary-action");
        HBox titleRow = new HBox(title, generateEliminationButton); titleRow.setAlignment(Pos.CENTER_LEFT); titleRow.setSpacing(16); HBox.setHgrow(title, Priority.ALWAYS); titleRow.getStyleClass().add("page-title-row");
        HBox status = new HBox(standingsStatusLabel); status.setAlignment(Pos.CENTER_LEFT); status.getStyleClass().add("workspace-toolbar");
        ScrollPane standingsScroll = resultsScroll(standingsGrid); VBox root = new VBox(8, titleRow, description, status, standingsScroll); root.getStyleClass().add("screen-content"); VBox.setVgrow(standingsScroll, Priority.ALWAYS); return root;
    }
    private VBox buildEliminationTab() {
        Label title = new Label("Direct Elimination"); title.getStyleClass().add("screen-title");
        Label hint = new Label("Select a pending bracket bout to record its result."); hint.getStyleClass().add("screen-subtitle");
        bracketBoard.getStyleClass().add("bracket-board"); bracketCanvas.getStyleClass().add("bracket-canvas"); bracketCanvas.setAlignment(Pos.TOP_LEFT); bracketScroll.setFitToHeight(false); bracketScroll.setFitToWidth(false); bracketScroll.setPannable(true); bracketScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED); bracketScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); bracketScroll.getStyleClass().add("bracket-scroll");
        eliminationFirstScoreField.setPrefWidth(64); eliminationSecondScoreField.setPrefWidth(64); recordEliminationResultButton.getStyleClass().add("primary-action"); editEliminationResultButton.getStyleClass().add("secondary-action"); cancelEliminationEditButton.getStyleClass().add("secondary-action"); endEliminationResultEdit();
        eliminationValidationErrorLabel.getStyleClass().add("inline-validation-error");
        eliminationValidationErrorLabel.setWrapText(true);
        eliminationValidationErrorLabel.setMaxWidth(256);
        eliminationValidationErrorLabel.setVisible(false); eliminationValidationErrorLabel.setManaged(false);
        HBox firstRow = new HBox(12, eliminationFirstNameLabel, eliminationFirstScoreField); HBox.setHgrow(eliminationFirstNameLabel, Priority.ALWAYS); firstRow.getStyleClass().add("de-result-row");
        HBox secondRow = new HBox(12, eliminationSecondNameLabel, eliminationSecondScoreField); HBox.setHgrow(eliminationSecondNameLabel, Priority.ALWAYS); secondRow.getStyleClass().add("de-result-row");
        HBox actions = new HBox(8, recordEliminationResultButton, cancelEliminationEditButton);
        VBox entry = new VBox(7, new Label("RECORD RESULT"), selectedEliminationMatchLabel, firstRow, secondRow,
                actions, editEliminationResultButton, eliminationValidationErrorLabel); entry.setAlignment(Pos.CENTER_LEFT); entry.getStyleClass().add("de-result-entry");
        eliminationWorkspace.getChildren().setAll(bracketScroll, entry); eliminationWorkspace.setSpacing(14); eliminationWorkspace.getStyleClass().add("elimination-workspace"); HBox.setHgrow(bracketScroll, Priority.ALWAYS); VBox.setVgrow(eliminationWorkspace, Priority.ALWAYS);
        eliminationWorkspace.heightProperty().addListener((ignored, previous, current) -> requestEliminationRelayout());
        VBox root = new VBox(6, title, hint, eliminationWorkspace); root.getStyleClass().addAll("screen-content", "elimination-content"); return root;
    }
    private VBox buildFinalResultsTab() {
        Label title = new Label("Final Results"); title.getStyleClass().add("screen-title");
        Label complete = new Label("COMPLETED"); complete.getStyleClass().add("completion-status");
        Label tableTitle = new Label("FINAL STANDINGS"); tableTitle.getStyleClass().add("section-kicker");
        ScrollPane finalResultsScroll = resultsScroll(finalResultsGrid); VBox root = new VBox(12, title, complete, tableTitle, finalResultsScroll); root.getStyleClass().add("screen-content"); VBox.setVgrow(finalResultsScroll, Priority.ALWAYS); return root;
    }
    private static ScrollPane resultsScroll(GridPane grid) {
        ScrollPane scroll = new ScrollPane(grid); scroll.setFitToWidth(true); scroll.setFitToHeight(false); scroll.setPannable(true); scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED); scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED); scroll.getStyleClass().add("results-scroll"); return scroll;
    }
    private HBox buildStatusBar() { statusLabel.getStyleClass().add("status-text"); HBox bar = new HBox(statusLabel); bar.getStyleClass().add("status-bar"); return bar; }
    private static void configureInlineValidationLabel(Label label, double maxWidth) {
        label.getStyleClass().add("inline-validation-error");
        label.setWrapText(true);
        label.setMaxWidth(maxWidth);
        label.setVisible(false);
        label.setManaged(false);
    }
    private void configureScoreValidationFeedback() {
        firstScoreField.textProperty().addListener((observable, oldValue, newValue) -> clearPoolValidationError());
        secondScoreField.textProperty().addListener((observable, oldValue, newValue) -> clearPoolValidationError());
        eliminationFirstScoreField.textProperty().addListener((observable, oldValue, newValue) -> clearEliminationValidationError());
        eliminationSecondScoreField.textProperty().addListener((observable, oldValue, newValue) -> clearEliminationValidationError());
        tournamentNameField.textProperty().addListener((observable, oldValue, newValue) -> clearTournamentNameValidationError());
        homeTournamentNameField.textProperty().addListener((observable, oldValue, newValue) -> clearHomeTournamentNameValidationError());
        fencerNameField.textProperty().addListener((observable, oldValue, newValue) -> clearFencerValidationError());
        maximumPoolSizeChoice.valueProperty().addListener((observable, oldValue, newValue) -> clearSeedingValidationError());
    }
    private void configureListCells() {
        seedList.setCellFactory(ignored -> seedCell());
        configureSeedListAutoScroll();
        poolList.setCellFactory(ignored -> poolCell());
    }
    private void configureSeedListAutoScroll() {
        seedList.addEventFilter(DragEvent.DRAG_OVER, event -> {
            if (event.getDragboard().hasString() && !seedList.isDisabled()) {
                updateSeedAutoScroll(event.getSceneY());
            }
        });
        seedList.addEventFilter(DragEvent.DRAG_EXITED, event -> {
            Bounds bounds = seedList.localToScene(seedList.getBoundsInLocal());
            if (bounds == null || event.getSceneY() < bounds.getMinY() || event.getSceneY() > bounds.getMaxY()) {
                stopSeedAutoScroll();
            }
        });
        seedList.addEventFilter(DragEvent.DRAG_DROPPED, event -> stopSeedAutoScroll());
        seedList.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) stopSeedAutoScroll();
        });
        seedList.setOnDragDropped(event -> {
            boolean moved = false;
            if (event.getDragboard().hasString() && !seedList.isDisabled() && !seedList.getItems().isEmpty()) {
                try {
                    UUID fencerId = UUID.fromString(event.getDragboard().getString());
                    int source = indexOfSeed(fencerId);
                    int destination = seedList.getItems().size() - 1;
                    if (source >= 0 && source != destination) seedMoveHandler.accept(fencerId, destination);
                    moved = source >= 0;
                } catch (IllegalArgumentException ignored) { }
            }
            stopSeedAutoScroll();
            event.setDropCompleted(moved);
            event.consume();
        });
    }
    private void updateSeedAutoScroll(double sceneY) {
        Bounds bounds = seedList.localToScene(seedList.getBoundsInLocal());
        if (bounds == null || sceneY < bounds.getMinY() || sceneY > bounds.getMaxY()) {
            stopSeedAutoScroll();
            return;
        }
        double distanceFromTop = sceneY - bounds.getMinY();
        double distanceFromBottom = bounds.getMaxY() - sceneY;
        if (distanceFromTop < SEED_AUTO_SCROLL_EDGE) {
            startSeedAutoScroll(-autoScrollRate(distanceFromTop));
        } else if (distanceFromBottom < SEED_AUTO_SCROLL_EDGE) {
            startSeedAutoScroll(autoScrollRate(distanceFromBottom));
        } else {
            stopSeedAutoScroll();
        }
    }
    private static double autoScrollRate(double distanceFromEdge) {
        double proximity = 1.0 - Math.max(0, distanceFromEdge) / SEED_AUTO_SCROLL_EDGE;
        return SEED_AUTO_SCROLL_MIN_RATE + (SEED_AUTO_SCROLL_MAX_RATE - SEED_AUTO_SCROLL_MIN_RATE) * proximity;
    }
    private void startSeedAutoScroll(double rate) {
        seedAutoScrollRate = rate;
        if (!seedAutoScrollRunning) {
            seedAutoScrollRunning = true;
            seedAutoScrollLastNanos = 0;
            seedAutoScrollTimer.start();
        }
    }
    private void stopSeedAutoScroll() {
        seedAutoScrollRate = 0;
        seedAutoScrollLastNanos = 0;
        if (seedAutoScrollRunning) {
            seedAutoScrollTimer.stop();
            seedAutoScrollRunning = false;
        }
    }
    private void scrollSeedList(double amount) {
        ScrollBar bar = seedList.lookupAll(".scroll-bar").stream()
                .filter(ScrollBar.class::isInstance)
                .map(ScrollBar.class::cast)
                .filter(candidate -> candidate.getOrientation() == javafx.geometry.Orientation.VERTICAL)
                .findFirst().orElse(null);
        if (bar == null || amount == 0) return;
        double next = Math.max(bar.getMin(), Math.min(bar.getMax(), bar.getValue() + amount));
        if (Double.compare(next, bar.getValue()) == 0) {
            stopSeedAutoScroll();
            return;
        }
        bar.setValue(next);
    }
    private ListCell<Fencer> seedCell() {
        ListCell<Fencer> cell = new ListCell<>() {
            @Override protected void updateItem(Fencer fencer, boolean empty) {
                super.updateItem(fencer, empty);
                getStyleClass().removeAll("seed-drop-before", "seed-drop-after", "seed-dragging");
                if (empty || fencer == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label handle = new Label("≡"); handle.getStyleClass().add("seed-drag-handle");
                Label number = new Label(Integer.toString(getIndex() + 1)); number.getStyleClass().add("seed-row-number");
                Label name = new Label(fencer.name()); name.getStyleClass().add("seed-row-name");
                Button remove = new Button("×");
                remove.getStyleClass().add("seed-row-remove");
                remove.setAccessibleText("Remove fencer");
                remove.setTooltip(new Tooltip("Remove fencer"));
                remove.setOnAction(event -> {
                    event.consume();
                    fencerRemoveHandler.accept(fencer.id());
                });
                HBox details = new HBox(10, handle, number, name);
                details.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(name, Priority.ALWAYS);
                BorderPane row = new BorderPane();
                row.setLeft(details);
                row.setRight(remove);
                row.setMaxWidth(Double.MAX_VALUE);
                row.prefWidthProperty().bind(widthProperty().subtract(24));
                BorderPane.setAlignment(remove, Pos.CENTER_RIGHT);
                setText(null);
                setGraphic(row);
            }
        };
        cell.setOnDragDetected(event -> {
            Fencer fencer = cell.getItem();
            if (fencer == null || seedList.isDisabled()) return;
            Dragboard dragboard = cell.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent(); content.putString(fencer.id().toString()); dragboard.setContent(content);
            cell.getStyleClass().add("seed-dragging");
            event.consume();
        });
        cell.setOnDragOver(event -> {
            if (event.getGestureSource() != cell && event.getDragboard().hasString() && !seedList.isDisabled()) {
                event.acceptTransferModes(TransferMode.MOVE);
                cell.getStyleClass().removeAll("seed-drop-before", "seed-drop-after");
                cell.getStyleClass().add(event.getY() <= cell.getHeight() / 2 ? "seed-drop-before" : "seed-drop-after");
            }
            event.consume();
        });
        cell.setOnDragExited(event -> cell.getStyleClass().removeAll("seed-drop-before", "seed-drop-after"));
        cell.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean moved = false;
            if (dragboard.hasString() && cell.getItem() != null && !seedList.isDisabled()) {
                try {
                    UUID fencerId = UUID.fromString(dragboard.getString());
                    int source = indexOfSeed(fencerId);
                    int insertion = cell.getIndex() + (event.getY() > cell.getHeight() / 2 ? 1 : 0);
                    int destination = insertion > source ? insertion - 1 : insertion;
                    if (source >= 0 && destination != source) seedMoveHandler.accept(fencerId, destination);
                    moved = true;
                } catch (IllegalArgumentException ignored) { }
            }
            stopSeedAutoScroll();
            cell.getStyleClass().removeAll("seed-drop-before", "seed-drop-after"); event.setDropCompleted(moved); event.consume();
        });
        cell.setOnDragDone(event -> {
            stopSeedAutoScroll();
            cell.getStyleClass().remove("seed-dragging");
        });
        return cell;
    }

    private int indexOfSeed(UUID fencerId) {
        return java.util.stream.IntStream.range(0, seedList.getItems().size())
                .filter(index -> seedList.getItems().get(index).id().equals(fencerId)).findFirst().orElse(-1);
    }
    private ListCell<Pool> poolCell() { return new ListCell<>() { @Override protected void updateItem(Pool pool, boolean empty) { super.updateItem(pool, empty); setText(empty || pool == null ? null : "POOL #" + (getIndex() + 1)); }}; }
    private VBox poolPanel(PoolDashboardPanel panel, double width) {
        Label name = new Label(panel.poolName()); name.getStyleClass().add("pool-panel-title");
        Label progress = new Label(panel.fencerCount() + " fencers · " + panel.completedBouts()
                + " / " + panel.totalBouts() + " bouts"); progress.getStyleClass().add("pool-panel-progress");
        VBox heading = new VBox(1, name, progress); heading.getStyleClass().add("pool-panel-heading");
        GridPane grid = new GridPane();
        renderPoolMatrix(grid, panel.poolId(), panel.matrixRows(),
                PoolLayout.matrixMetrics(panel.matrixRows().size(), width, PoolLayout.ORGANISER));
        VBox panelNode = new VBox(5, heading, grid); panelNode.getStyleClass().add("pool-panel");
        panelNode.setAlignment(Pos.TOP_CENTER);
        panelNode.setMinWidth(width);
        panelNode.setPrefWidth(width);
        panelNode.setMaxWidth(width);
        return panelNode;
    }

    private void renderPoolMatrix(GridPane grid, UUID poolId, List<PoolMatrixRow> rows,
                                  PoolLayout.MatrixMetrics metrics) {
        prepareGrid(grid, "pool-score-grid");
        if (rows.isEmpty()) { addGridCell(grid, "Pool data unavailable.", 0, 0, 280, "compact-grid-empty"); return; }
        List<UUID> opponents = new java.util.ArrayList<>(rows.getFirst().cells().keySet());
        double nameWidth = metrics.nameWidth();
        double scoreWidth = metrics.scoreWidth();
        double rowHeight = metrics.rowHeight();
        addGridCell(grid, "", 0, 0, nameWidth, rowHeight, "compact-grid-header", "matrix-fencer-label");
        for (int column = 0; column < opponents.size(); column++) {
            String name = fencerNameForColumn(rows, opponents.get(column));
            Label header = addGridCell(grid, name, column + 1, 0, scoreWidth, rowHeight, "compact-grid-header", "matrix-fencer-label");
            configureMatrixName(header, name);
        }
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            PoolMatrixRow row = rows.get(rowIndex); int gridRow = rowIndex + 1;
            Label rowLabel = addGridCell(grid, row.fencerName(), 0, gridRow, nameWidth, rowHeight, "compact-grid-header", "matrix-fencer-label");
            configureMatrixName(rowLabel, row.fencerName());
            for (int column = 0; column < opponents.size(); column++) {
                UUID opponentId = opponents.get(column); String value = row.cell(opponentId);
                boolean diagonal = "—".equals(value);
                boolean winner = !diagonal && value != null && !value.isBlank() && isWinner(row, opponentId, value, rows);
                String displayValue = diagonal || value == null ? "" : winner ? "V" + value : value;
                Label cell = addGridCell(grid, displayValue, column + 1, gridRow, scoreWidth, rowHeight, "compact-grid-cell", "matrix-grid-cell");
                if (diagonal) cell.getStyleClass().add("matrix-diagonal");
                else if (value == null || value.isBlank()) cell.getStyleClass().add("matrix-pending");
                else cell.getStyleClass().add(winner ? "matrix-win" : "matrix-loss");
                if (poolId.equals(selectedMatrixPool) && row.fencerId().equals(selectedMatrixRow) && opponentId.equals(selectedMatrixOpponent)) cell.getStyleClass().add("matrix-selected");
                if (!row.fencerId().equals(opponentId)) cell.setOnMouseClicked(event -> matrixCellHandler.accept(new PoolMatrixSelection(poolId, row.fencerId(), opponentId)));
            }
        }
    }

    private void renderPoolResultGrid(List<OverallSeedingRow> rows) {
        prepareGrid(standingsGrid, "results-grid");
        String[] headers = {"Place", "Fencer", "V", "M", "V/M", "TS", "TR", "Indices", "Status"};
        double[] widths = {58, 180, 48, 52, 60, 56, 56, 68, 100};
        for (int column = 0; column < headers.length; column++) addGridCell(standingsGrid, headers[column], column, 0, widths[column], "compact-grid-header", column == 1 ? "compact-grid-name" : "");
        for (int index = 0; index < rows.size(); index++) {
            OverallSeedingRow row = rows.get(index); String status = row.rank() <= 16 ? "Advanced" : "Eliminated";
            String[] values = {Integer.toString(row.rank()), row.name(), Integer.toString(row.wins()), Integer.toString(row.matches()), String.format("%.2f", row.ratio()), Integer.toString(row.touchesScored()), Integer.toString(row.touchesReceived()), formatIndicator(row.indicator()), status};
            for (int column = 0; column < values.length; column++) {
                Label cell = addGridCell(standingsGrid, values[column], column, index + 1, widths[column], "compact-grid-cell", column == 1 ? "compact-grid-name" : "");
                if (column == values.length - 1) cell.getStyleClass().add(status.equals("Advanced") ? "pool-status-advanced" : "pool-status-eliminated");
            }
        }
    }

    private static String formatIndicator(int indicator) {
        return indicator > 0 ? "+" + indicator : Integer.toString(indicator);
    }

    private void renderFinalResultsGrid(List<FinalResultsRow> rows) {
        prepareGrid(finalResultsGrid, "results-grid", "final-results-grid");
        addGridCell(finalResultsGrid, "Place", 0, 0, 70, "compact-grid-header"); addGridCell(finalResultsGrid, "Fencer", 1, 0, 250, "compact-grid-header", "final-fencer-header");
        for (int index = 0; index < rows.size(); index++) {
            FinalResultsRow row = rows.get(index); String medalClass = switch (row.place()) { case 1 -> "final-place-gold"; case 2 -> "final-place-silver"; case 3, 4 -> "final-place-bronze"; default -> ""; };
            addGridCell(finalResultsGrid, Integer.toString(row.place()), 0, index + 1, 70, "compact-grid-cell", medalClass);
            addGridCell(finalResultsGrid, row.fencerName(), 1, index + 1, 250, "compact-grid-cell", "compact-grid-name", medalClass);
        }
    }

    private static void prepareGrid(GridPane grid, String... styleClasses) {
        grid.getChildren().clear(); grid.setHgap(0); grid.setVgap(0); grid.getStyleClass().setAll(styleClasses);
    }

    private static Label addGridCell(GridPane grid, String text, int column, int row, double width, String... styleClasses) {
        return addGridCell(grid, text, column, row, width, 34, styleClasses);
    }

    private static Label addGridCell(GridPane grid, String text, int column, int row, double width, double height, String... styleClasses) {
        Label cell = new Label(text); cell.setMinWidth(width); cell.setPrefWidth(width); cell.setMaxWidth(width); cell.setMinHeight(height); cell.setPrefHeight(height); cell.setAlignment(Pos.CENTER); cell.getStyleClass().addAll(styleClasses); grid.add(cell, column, row); return cell;
    }

    private static void configureMatrixName(Label label, String fullName) {
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        label.setTooltip(new Tooltip(fullName));
    }

    private static boolean isInside(Node target, Node container) {
        for (Node current = target; current != null; current = current.getParent()) {
            if (current == container) return true;
        }
        return false;
    }

    private static boolean hasStyleInHierarchy(Node target, String styleClass) {
        for (Node current = target; current != null; current = current.getParent()) {
            if (current.getStyleClass().contains(styleClass)) return true;
        }
        return false;
    }

    private boolean isWinner(PoolMatrixRow row, UUID opponentId, String score, List<PoolMatrixRow> rows) { for (PoolMatrixRow opponent : rows) if (opponent.fencerId().equals(opponentId)) return parse(score) > parse(opponent.cell(row.fencerId())); return false; }
    private static int parse(String value) { try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { return -1; } }
    private java.util.Map<String, BracketGeometry> calculateBracketGeometry(List<EliminationMatchRow> matches,
                                                                              java.util.Map<String, EliminationMatchRow> byPosition,
                                                                              int finalRound, double openingStep) {
        java.util.Map<String, BracketGeometry> geometry = new java.util.HashMap<>();
        for (int round = 1; round <= finalRound; round++) {
            int currentRound = round;
            matches.stream().filter(match -> match.round() == currentRound)
                    .sorted(Comparator.comparingInt(EliminationMatchRow::position)).forEach(match -> {
                        double centreY;
                        double firstRowY;
                        double secondRowY;
                        if (match.round() == 1) {
                            centreY = TABLEAU_FIRST_CENTRE + match.position() * openingStep;
                            firstRowY = centreY - TABLEAU_ROW_HEIGHT / 2;
                            secondRowY = centreY + TABLEAU_ROW_HEIGHT / 2;
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
        boolean available = isAvailableEliminationBout(match);
        if (match.bye()) card.getStyleClass().add("fencing-bye-card");
        else if (match.resolved()) card.getStyleClass().add("fencing-resolved-card");
        else if (available) card.getStyleClass().add("fencing-ready-card");
        else card.getStyleClass().add("fencing-future-card");
        if (finalRound) card.getStyleClass().add("fencing-final-card");
        if ((available || (match.resolved() && !match.bye())) && match.matchId().equals(selectedEliminationMatchId)) card.getStyleClass().add("fencing-selected-card");

        Pane firstRow = participantCardRow(match.first());
        Pane secondRow = participantCardRow(match.second());
        firstRow.getStyleClass().add("fencing-bout-row-first");
        if (match.resolved() && !match.bye()) {
            Pane winnerRow = match.first().winner() ? firstRow : secondRow;
            Pane loserRow = match.first().winner() ? secondRow : firstRow;
            winnerRow.getStyleClass().add("fencing-winner-row");
            loserRow.getStyleClass().add("fencing-loser-row");
        }
        firstRow.relocate(0, 0); secondRow.relocate(0, TABLEAU_ROW_HEIGHT);
        card.getChildren().addAll(firstRow, secondRow);
        if (available || (match.resolved() && !match.bye())) card.setOnMouseClicked(event -> eliminationMatchHandler.accept(match.matchId()));
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
        Pane row = new Pane(seed, name, score); row.setPrefSize(TABLEAU_CARD_WIDTH, TABLEAU_ROW_HEIGHT); return row;
    }

    private static String participantText(EliminationParticipant participant) {
        return participant.seed() == 0 ? participant.name() : participant.name() + " (" + participant.seed() + ")";
    }
    /** A DE bout is selectable as soon as both real competitors are known, independent of its round. */
    static boolean isAvailableEliminationBout(EliminationMatchRow match) {
        return match != null && !match.resolved() && !match.bye()
                && !match.first().unresolved() && !match.second().unresolved()
                && !match.first().bye() && !match.second().bye();
    }
    private static Line connector(double startX, double startY, double endX, double endY) {
        Line line = new Line(startX, startY, endX, endY); line.getStyleClass().add("bracket-connector"); return line;
    }
    private static String keyOf(EliminationMatchRow match) { return match.round() + ":" + match.position(); }
    private static double boardX(int round) { return TABLEAU_LEFT + (round - 1) * TABLEAU_ROUND_STEP; }
    private static double openingStep(int openingMatchCount, double availableHeight) {
        double preferredStep = openingMatchCount >= 8 ? TABLEAU_CARD_HEIGHT + 12
                : openingMatchCount >= 4 ? TABLEAU_CARD_HEIGHT + 24
                : openingMatchCount >= 2 ? TABLEAU_CARD_HEIGHT + 44 : TABLEAU_CARD_HEIGHT + 20;
        if (openingMatchCount < 2 || availableHeight <= 0) return preferredStep;
        double maximumStep = (availableHeight - TABLEAU_FIRST_CENTRE - TABLEAU_CARD_HEIGHT / 2 - TABLEAU_BOTTOM_PADDING)
                / (openingMatchCount - 1);
        return Math.max(TABLEAU_CARD_HEIGHT + 4, Math.min(preferredStep, maximumStep));
    }
    private static int activeEliminationRound(List<EliminationMatchRow> matches, int finalRound) {
        return matches.stream().filter(EliminationMatchRow::ready).mapToInt(EliminationMatchRow::round).min().orElse(finalRound);
    }
    private void focusActiveEliminationRound(double boardWidth) {
        Platform.runLater(() -> {
            double viewportWidth = bracketScroll.getViewportBounds().getWidth();
            if (viewportWidth <= 0 || boardWidth <= viewportWidth) return;
            double targetX = Math.max(0, Math.min(boardX(activeEliminationRound) - 16, boardWidth - viewportWidth));
            bracketScroll.setHvalue(targetX / (boardWidth - viewportWidth));
        });
    }
    private double availableBracketHeight() {
        double viewportHeight = bracketScroll.getViewportBounds().getHeight();
        return viewportHeight > 0 ? viewportHeight
                : Math.max(0, eliminationWorkspace.getHeight() - HORIZONTAL_SCROLLBAR_ALLOWANCE);
    }
    private void requestEliminationRelayout() {
        if (eliminationRelayoutQueued || renderedEliminationMatches.isEmpty()) return;
        eliminationRelayoutQueued = true;
        Platform.runLater(() -> {
            eliminationRelayoutQueued = false;
            if (!renderedEliminationMatches.isEmpty()) renderEliminationBracket(renderedEliminationMatches);
        });
    }
    private static String winnerLabel(EliminationMatchRow match) {
        return (match.first().winner() ? match.first() : match.second()).name();
    }
    private record BracketGeometry(double centreY, double firstRowY, double secondRowY) { }
    private static String roundName(int matchCount) {
        return switch (matchCount) {
            case 1 -> "Final";
            case 2 -> "Semi-finals";
            case 4 -> "Quarter-finals";
            default -> "Round of " + (matchCount * 2);
        };
    }
    private static VBox sectionTitle(String title, String description) { Label heading = new Label(title); heading.getStyleClass().add("screen-title"); Label text = new Label(description); text.getStyleClass().add("screen-subtitle"); return new VBox(3, heading, text); }
    private static HBox formRow(TextField field, Button action) { HBox row = new HBox(10, field, action); row.getStyleClass().add("form-row"); HBox.setHgrow(field, Priority.ALWAYS); return row; }
    private static HBox compactHeader(String title, Button action) { Label heading = new Label(title); heading.getStyleClass().add("list-heading"); HBox row = new HBox(heading, action); row.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(heading, Priority.ALWAYS); return row; }
    private static HBox actionRow(Button... buttons) { HBox row = new HBox(8, buttons); row.setAlignment(Pos.CENTER_RIGHT); row.getStyleClass().add("action-row"); return row; }
    private static void showOnly(Node node, boolean visible) { node.setVisible(visible); node.setManaged(visible); }
    private static String fencerNameForColumn(List<PoolMatrixRow> rows, UUID id) { return rows.stream().filter(row -> row.fencerId().equals(id)).map(PoolMatrixRow::fencerName).findFirst().orElse("Fencer"); }
    private static int indexOfPool(List<Pool> pools, UUID id) { for (int index = 0; index < pools.size(); index++) if (pools.get(index).id().equals(id)) return index; return 0; }
    private static String phaseText(TournamentPhase phase) { return switch (phase) { case REGISTRATION, SEEDING -> "Setup"; case POOL_PHASE -> "Pool phase"; case ELIMINATION_PHASE -> "Elimination phase"; case COMPLETE -> "Tournament complete"; }; }
}
