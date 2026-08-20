package edu.nus.cs3227.fencingtournament.ui;

import edu.nus.cs3227.fencingtournament.application.PoolProgress;
import edu.nus.cs3227.fencingtournament.domain.Fencer;
import edu.nus.cs3227.fencingtournament.domain.TournamentPhase;
import edu.nus.cs3227.fencingtournament.domain.pool.Pool;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.List;
import java.util.Comparator;
import java.util.UUID;
import java.util.function.BiConsumer;

/** Main JavaFX workspace. It renders presentation state and delegates user intent to its controller. */
public final class TournamentView extends BorderPane {
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
    private final Tab standingsTab = new Tab("Standings");
    private final VBox createTournamentSection = new VBox();
    private final VBox registrationSection = new VBox();
    private final VBox seedingSection = new VBox();

    private final ListView<Pool> poolList = new ListView<>(FXCollections.observableArrayList());
    private final Label selectedPoolLabel = new Label("Select a pool");
    private final Label poolProgressLabel = new Label();
    private final TableView<PoolMatrixRow> poolMatrixTable = new TableView<>(FXCollections.observableArrayList());
    private final TextField firstScoreField = new TextField();
    private final TextField secondScoreField = new TextField();
    private final Button recordResultButton = new Button("Record result");
    private final Label selectedBoutLabel = new Label("Select a bout in the matrix");
    private final Label firstFencerLabel = new Label("—");
    private final Label secondFencerLabel = new Label("—");
    private final Label resultStateLabel = new Label();
    private final VBox scoreFields = new VBox();
    private final VBox resultEntry = new VBox();

    private final TableView<OverallSeedingRow> standingsTable = new TableView<>(FXCollections.observableArrayList());
    private final Label standingsStatusLabel = new Label();
    private BiConsumer<UUID, UUID> matrixCellHandler = (row, opponent) -> { };
    private UUID selectedMatrixRow;
    private UUID selectedMatrixOpponent;

    public TournamentView() {
        getStyleClass().add("workspace");
        setTop(buildHeader());
        setCenter(tabs);
        setBottom(buildStatusBar());
        fencersTab.setContent(buildSetupTab());
        poolsTab.setContent(buildPoolsTab());
        standingsTab.setContent(buildStandingsTab());
        for (Tab tab : List.of(fencersTab, poolsTab, standingsTab)) tab.setClosable(false);
        tabs.getTabs().addAll(fencersTab, poolsTab, standingsTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("phase-navigation");
        configureListCells();
        configureTables();
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
    public ListView<Pool> poolList() { return poolList; }
    public TableView<PoolMatrixRow> poolMatrixTable() { return poolMatrixTable; }
    public TextField firstScoreField() { return firstScoreField; }
    public TextField secondScoreField() { return secondScoreField; }
    public Button recordResultButton() { return recordResultButton; }
    public void setMatrixCellHandler(BiConsumer<UUID, UUID> handler) { matrixCellHandler = handler == null ? (row, opponent) -> { } : handler; }
    public void markSelectedMatrixCell(UUID row, UUID opponent) { selectedMatrixRow = row; selectedMatrixOpponent = opponent; poolMatrixTable.refresh(); }

    public void renderFencers(List<Fencer> fencers, List<Fencer> seedOrder) { fencerList.getItems().setAll(fencers); seedList.getItems().setAll(seedOrder); }
    public void renderPools(List<Pool> pools) {
        Pool selected = poolList.getSelectionModel().getSelectedItem();
        poolList.getItems().setAll(pools);
        if (!pools.isEmpty()) poolList.getSelectionModel().select(Math.max(0, selected == null ? 0 : indexOfPool(pools, selected.id())));
    }
    public void renderSelectedPool(String poolName, List<String> members, List<PoolMatrixRow> matrixRows) {
        selectedPoolLabel.setText(poolName);
        poolProgressLabel.setText(members.size() + " fencers · click an unfinished cell to record a result");
        poolMatrixTable.getItems().setAll(matrixRows);
        configureMatrixColumns(matrixRows);
        double height = Math.max(130, 44 + matrixRows.size() * 48);
        poolMatrixTable.setMinHeight(height); poolMatrixTable.setPrefHeight(height); poolMatrixTable.setMaxHeight(height);
    }
    public void renderStandings(List<PoolStandingRow> standings, boolean complete) {
        standingsTable.getItems().clear();
        standingsStatusLabel.setText("Pool seeding is calculated after all pool bouts are complete.");
        standingsStatusLabel.getStyleClass().setAll("standing-status", complete ? "is-final" : "is-provisional");
    }

    public void renderOverallSeeding(List<OverallSeedingRow> rows) {
        standingsTable.getItems().setAll(rows.stream()
                .sorted(Comparator.comparingInt(OverallSeedingRow::originalSeed))
                .toList());
        standingsStatusLabel.setText("Final pool seeding");
        standingsStatusLabel.getStyleClass().setAll("standing-status", "is-final");
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
    public void setPhaseControls(TournamentPhase phase, boolean hasTournament, boolean poolResultsFinalized) {
        boolean registration = hasTournament && phase == TournamentPhase.REGISTRATION;
        boolean seeding = hasTournament && phase == TournamentPhase.SEEDING;
        boolean pools = hasTournament && (phase == TournamentPhase.POOL_PHASE || phase == TournamentPhase.ELIMINATION_PHASE || phase == TournamentPhase.COMPLETE);
        showOnly(createTournamentSection, !hasTournament); showOnly(registrationSection, registration); showOnly(seedingSection, seeding);
        saveButton.setDisable(!hasTournament); fencerNameField.setDisable(!registration); addFencerButton.setDisable(!registration); removeFencerButton.setDisable(!registration); fencerList.setDisable(!registration);
        seedList.setDisable(!seeding); moveSeedUpButton.setDisable(!seeding); moveSeedDownButton.setDisable(!seeding); applySeedingButton.setDisable(!(registration || seeding));
        confirmSeedingButton.setDisable(!registration); applySeedingButton.setText("Apply revised order"); generatePoolsButton.setDisable(!seeding); poolsTab.setDisable(!pools); standingsTab.setDisable(!poolResultsFinalized);
    }
    public void setNoTournamentState() {
        tournamentNameLabel.setText("No tournament open"); phaseLabel.setText("Create a tournament or open an existing file"); progressLabel.setText("");
        fencerList.getItems().clear(); seedList.getItems().clear(); poolList.getItems().clear(); poolMatrixTable.getItems().clear(); standingsTable.getItems().clear(); showSelectedBout(null);
        setPhaseControls(TournamentPhase.REGISTRATION, false, false);
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
        Label matrixLabel = new Label("POOL MATRIX"); matrixLabel.getStyleClass().add("section-kicker"); VBox matrixArea = new VBox(8, matrixLabel, poolMatrixTable); matrixArea.getStyleClass().add("matrix-area");
        buildResultEntry(); VBox content = new VBox(16, poolHeading, matrixArea, resultEntry); content.getStyleClass().add("pools-content"); BorderPane root = new BorderPane(content); root.setLeft(navigator); BorderPane.setMargin(navigator, new Insets(0, 24, 0, 0)); return root;
    }
    private void buildResultEntry() {
        Label title = new Label("RECORD RESULT"); title.getStyleClass().add("section-kicker"); selectedBoutLabel.getStyleClass().add("selected-bout"); firstFencerLabel.getStyleClass().add("result-fencer"); secondFencerLabel.getStyleClass().add("result-fencer");
        firstScoreField.setPromptText("Score"); secondScoreField.setPromptText("Score"); firstScoreField.getStyleClass().add("score-field"); secondScoreField.getStyleClass().add("score-field"); firstScoreField.setPrefWidth(74); secondScoreField.setPrefWidth(74); recordResultButton.getStyleClass().add("primary-action");
        Label dash = new Label("—"); dash.getStyleClass().add("score-dash"); HBox scoreLine = new HBox(14, firstScoreField, dash, secondScoreField, recordResultButton); scoreLine.setAlignment(Pos.CENTER); scoreFields.getChildren().setAll(scoreLine); scoreFields.setAlignment(Pos.CENTER);
        HBox names = new HBox(40, firstFencerLabel, secondFencerLabel); names.setAlignment(Pos.CENTER); resultEntry.getChildren().setAll(title, selectedBoutLabel, names, resultStateLabel, scoreFields); resultEntry.setAlignment(Pos.CENTER); resultEntry.getStyleClass().add("result-entry");
    }
    private VBox buildStandingsTab() {
        Label title = new Label("Pool seeding"); title.getStyleClass().add("screen-title"); Label description = new Label("Overall ranking after every pool bout has been finalized."); description.getStyleClass().add("screen-subtitle"); HBox status = new HBox(standingsStatusLabel); status.setAlignment(Pos.CENTER_LEFT);
        VBox root = new VBox(6, title, description, status, standingsTable); root.getStyleClass().add("screen-content"); VBox.setVgrow(standingsTable, Priority.ALWAYS); return root;
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
        TableColumn<OverallSeedingRow, String> seed = textColumn("Seed", row -> Integer.toString(row.originalSeed()), 54);
        TableColumn<OverallSeedingRow, String> fencer = textColumn("Fencer", OverallSeedingRow::name, 180);
        fencer.getStyleClass().add("standing-name-column");
        standingsTable.getColumns().addAll(seed, fencer,
                textColumn("V", row -> Integer.toString(row.wins()), 48),
                textColumn("M", row -> Integer.toString(row.matches()), 52),
                textColumn("V/M", row -> String.format("%.2f", row.ratio()), 60),
                textColumn("TS", row -> Integer.toString(row.touchesScored()), 56),
                textColumn("TR", row -> Integer.toString(row.touchesReceived()), 56),
                textColumn("Indices", row -> Integer.toString(row.indicator()), 68));
        standingsTable.getColumns().forEach(column -> column.setSortable(false));
        standingsTable.setSortPolicy(table -> false);
        standingsTable.setPlaceholder(new Label("Pool seeding is not available yet.")); standingsTable.getStyleClass().add("standings-table");
    }
    private void configureMatrixColumns(List<PoolMatrixRow> rows) {
        poolMatrixTable.getColumns().clear(); TableColumn<PoolMatrixRow, String> name = textColumn("Fencer", PoolMatrixRow::fencerName, 174); name.getStyleClass().add("matrix-name-column"); poolMatrixTable.getColumns().add(name); if (rows.isEmpty()) return;
        for (UUID opponentId : rows.get(0).cells().keySet()) { String nameText = fencerNameForColumn(rows, opponentId); TableColumn<PoolMatrixRow, String> column = new TableColumn<>(nameText); column.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().cell(opponentId))); column.setPrefWidth(Math.max(84, nameText.length() * 8 + 26)); column.setCellFactory(ignored -> matrixCell(opponentId)); column.setSortable(false); column.getStyleClass().add("matrix-score-column"); poolMatrixTable.getColumns().add(column); }
        poolMatrixTable.getColumns().forEach(column -> column.setSortable(false));
        poolMatrixTable.setSortPolicy(table -> false);
    }
    private TableCell<PoolMatrixRow, String> matrixCell(UUID opponentId) { return new TableCell<>() { @Override protected void updateItem(String value, boolean empty) { super.updateItem(value, empty); getStyleClass().removeAll("matrix-diagonal", "matrix-pending", "matrix-win", "matrix-loss", "matrix-selected"); setText(empty ? null : value == null ? "" : value); setAlignment(Pos.CENTER); if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) { setOnMouseClicked(null); return; } PoolMatrixRow row = getTableView().getItems().get(getIndex()); setOnMouseClicked(event -> matrixCellHandler.accept(row.fencerId(), opponentId)); if ("—".equals(value)) getStyleClass().add("matrix-diagonal"); else if (value == null || value.isBlank()) getStyleClass().add("matrix-pending"); else getStyleClass().add(isWinner(row, opponentId, value) ? "matrix-win" : "matrix-loss"); if (row.fencerId().equals(selectedMatrixRow) && opponentId.equals(selectedMatrixOpponent)) getStyleClass().add("matrix-selected"); }}; }
    private boolean isWinner(PoolMatrixRow row, UUID opponentId, String score) { for (PoolMatrixRow opponent : poolMatrixTable.getItems()) if (opponent.fencerId().equals(opponentId)) return parse(score) > parse(opponent.cell(row.fencerId())); return false; }
    private static int parse(String value) { try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { return -1; } }
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
