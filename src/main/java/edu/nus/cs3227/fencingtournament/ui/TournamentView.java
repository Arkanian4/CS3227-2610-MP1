package edu.nus.cs3227.fencingtournament.ui;

import edu.nus.cs3227.fencingtournament.application.PoolProgress;
import edu.nus.cs3227.fencingtournament.domain.Fencer;
import edu.nus.cs3227.fencingtournament.domain.TournamentPhase;
import edu.nus.cs3227.fencingtournament.domain.pool.Pool;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/** Main tournament workspace. It owns controls and rendering, not tournament rules. */
public final class TournamentView extends BorderPane {
    private final Label tournamentNameLabel = new Label("No tournament selected");
    private final Label phaseLabel = new Label("No tournament");
    private final Label progressLabel = new Label();
    private final Label statusLabel = new Label("Create or load a tournament to begin.");
    private final TextField tournamentNameField = new TextField();
    private final Button createButton = new Button("Create tournament");
    private final Button loadButton = new Button("Load");
    private final Button saveButton = new Button("Save");

    private final TextField fencerNameField = new TextField();
    private final Button addFencerButton = new Button("Add fencer");
    private final Button removeFencerButton = new Button("Remove selected");
    private final ListView<Fencer> fencerList = new ListView<>(FXCollections.observableArrayList());
    private final ListView<Fencer> seedList = new ListView<>(FXCollections.observableArrayList());
    private final Button moveSeedUpButton = new Button("Move up");
    private final Button moveSeedDownButton = new Button("Move down");
    private final Button applySeedingButton = new Button("Apply seeding");
    private final Button generatePoolsButton = new Button("Generate pools");

    private final TabPane tabs = new TabPane();
    private final Tab fencersTab = new Tab("Fencers & seeding");
    private final Tab poolsTab = new Tab("Pools & results");
    private final Tab standingsTab = new Tab("Standings");
    private final ListView<Pool> poolList = new ListView<>(FXCollections.observableArrayList());
    private final Label selectedPoolLabel = new Label("Select a pool");
    private final ListView<String> poolMembersList = new ListView<>(FXCollections.observableArrayList());
    private final TableView<PoolBoutRow> boutTable = new TableView<>(FXCollections.observableArrayList());
    private final TextField firstScoreField = new TextField();
    private final TextField secondScoreField = new TextField();
    private final Button recordResultButton = new Button("Record result");
    private final Label selectedBoutLabel = new Label("Select a pending bout");
    private final ComboBox<Pool> standingsPoolSelector = new ComboBox<>();
    private final TableView<PoolStandingRow> standingsTable = new TableView<>(FXCollections.observableArrayList());
    private final Label standingsStatusLabel = new Label();

    public TournamentView() {
        setPadding(new Insets(18));
        setTop(buildHeader());
        setCenter(tabs);
        setBottom(buildStatusBar());

        fencersTab.setContent(buildFencersTab());
        poolsTab.setContent(buildPoolsTab());
        standingsTab.setContent(buildStandingsTab());
        fencersTab.setClosable(false);
        poolsTab.setClosable(false);
        standingsTab.setClosable(false);
        tabs.getTabs().addAll(fencersTab, poolsTab, standingsTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        configureListCells();
        configureTables();
        setNoTournamentState();
    }

    public Scene scene() { return new Scene(this, 1000, 680); }
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
    public Button applySeedingButton() { return applySeedingButton; }
    public Button generatePoolsButton() { return generatePoolsButton; }
    public TabPane tabs() { return tabs; }
    public Tab poolsTab() { return poolsTab; }
    public Tab standingsTab() { return standingsTab; }
    public ListView<Pool> poolList() { return poolList; }
    public TableView<PoolBoutRow> boutTable() { return boutTable; }
    public TextField firstScoreField() { return firstScoreField; }
    public TextField secondScoreField() { return secondScoreField; }
    public Button recordResultButton() { return recordResultButton; }
    public ComboBox<Pool> standingsPoolSelector() { return standingsPoolSelector; }

    public void renderFencers(List<Fencer> fencers, List<Fencer> seedOrder) {
        fencerList.getItems().setAll(fencers);
        seedList.getItems().setAll(seedOrder);
    }

    public void renderPools(List<Pool> pools) {
        Pool selected = poolList.getSelectionModel().getSelectedItem();
        poolList.getItems().setAll(pools);
        if (!pools.isEmpty()) {
            int selectedIndex = selected == null ? 0 : indexOfPool(pools, selected.id());
            poolList.getSelectionModel().select(Math.max(0, selectedIndex));
        }
        standingsPoolSelector.getItems().setAll(pools);
        if (!pools.isEmpty()) standingsPoolSelector.getSelectionModel().select(0);
    }

    public void renderSelectedPool(String poolName, List<String> members, List<PoolBoutRow> bouts) {
        selectedPoolLabel.setText(poolName);
        poolMembersList.getItems().setAll(members);
        boutTable.getItems().setAll(bouts);
        firstScoreField.clear();
        secondScoreField.clear();
        selectedBoutLabel.setText("Select a pending bout");
        recordResultButton.setDisable(true);
    }

    public void renderStandings(List<PoolStandingRow> standings, boolean complete) {
        standingsTable.getItems().setAll(standings);
        standingsStatusLabel.setText(complete ? "Final pool standings" : "Provisional standings — bouts remain incomplete");
    }

    public void showSelectedBout(PoolBoutRow bout) {
        if (bout == null) {
            selectedBoutLabel.setText("Select a pending bout");
            recordResultButton.setDisable(true);
            return;
        }
        selectedBoutLabel.setText(bout.firstName() + " vs " + bout.secondName() + " — " + bout.status());
        recordResultButton.setDisable(bout.completed());
    }

    public void showPhase(TournamentPhase phase, PoolProgress progress) {
        phaseLabel.setText(phaseLabel(phase));
        progressLabel.setText(progress == null || progress.totalBouts() == 0
                ? "" : progress.completedBouts() + " / " + progress.totalBouts() + " pool bouts completed");
    }

    public void showStatus(String message) { statusLabel.setText(message == null ? "" : message); }

    public void setPhaseControls(TournamentPhase phase, boolean hasTournament) {
        boolean registration = hasTournament && phase == TournamentPhase.REGISTRATION;
        boolean seeding = hasTournament && phase == TournamentPhase.SEEDING;
        boolean pools = hasTournament && (phase == TournamentPhase.POOL_PHASE
                || phase == TournamentPhase.ELIMINATION_PHASE || phase == TournamentPhase.COMPLETE);
        tournamentNameField.setDisable(hasTournament);
        createButton.setDisable(false);
        saveButton.setDisable(!hasTournament);
        fencerNameField.setDisable(!registration);
        addFencerButton.setDisable(!registration);
        removeFencerButton.setDisable(!registration);
        fencerList.setDisable(!registration);
        seedList.setDisable(!(registration || seeding));
        moveSeedUpButton.setDisable(!(registration || seeding));
        moveSeedDownButton.setDisable(!(registration || seeding));
        applySeedingButton.setDisable(!(registration || seeding));
        generatePoolsButton.setDisable(!seeding);
        // Keep this tab available as the entry point for creating a tournament.
        fencersTab.setDisable(false);
        poolsTab.setDisable(!pools);
        standingsTab.setDisable(!pools);
    }

    public void setNoTournamentState() {
        tournamentNameLabel.setText("No tournament selected");
        phaseLabel.setText("No tournament");
        progressLabel.setText("");
        fencerList.getItems().clear();
        seedList.getItems().clear();
        poolList.getItems().clear();
        standingsPoolSelector.getItems().clear();
        setPhaseControls(TournamentPhase.REGISTRATION, false);
    }

    public void showTournamentName(String name) { tournamentNameLabel.setText(name); }

    private VBox buildHeader() {
        Label title = new Label("Fencing Tournament Manager");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        tournamentNameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        phaseLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        HBox actions = new HBox(8, loadButton, saveButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(actions, Priority.ALWAYS);
        HBox heading = new HBox(16, title, actions);
        heading.setAlignment(Pos.CENTER_LEFT);
        VBox header = new VBox(6, heading, tournamentNameLabel, phaseLabel, progressLabel, new Separator());
        header.setPadding(new Insets(0, 0, 12, 0));
        return header;
    }

    private VBox buildFencersTab() {
        tournamentNameField.setPromptText("e.g. Friday Internal Open");
        tournamentNameField.setOnAction(event -> createButton.fire());
        HBox createRow = new HBox(10, new Label("Tournament name"), tournamentNameField, createButton);
        createRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(tournamentNameField, Priority.ALWAYS);

        fencerNameField.setPromptText("Enter a display name");
        fencerNameField.setOnAction(event -> addFencerButton.fire());
        HBox addRow = new HBox(10, new Label("Fencer name"), fencerNameField, addFencerButton);
        addRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(fencerNameField, Priority.ALWAYS);

        VBox roster = new VBox(8, boldLabel("Registered fencers"), fencerList, removeFencerButton);
        VBox seeds = new VBox(8, boldLabel("Seed order"), seedList,
                new HBox(8, moveSeedUpButton, moveSeedDownButton),
                new HBox(8, applySeedingButton, generatePoolsButton));
        VBox.setVgrow(fencerList, Priority.ALWAYS);
        VBox.setVgrow(seedList, Priority.ALWAYS);
        HBox columns = new HBox(18, roster, seeds);
        HBox.setHgrow(roster, Priority.ALWAYS);
        HBox.setHgrow(seeds, Priority.ALWAYS);
        VBox content = new VBox(14, createRow, addRow, new Separator(), columns);
        VBox.setVgrow(columns, Priority.ALWAYS);
        return content;
    }

    private BorderPane buildPoolsTab() {
        poolList.setPrefWidth(140);
        BorderPane details = new BorderPane();
        details.setTop(selectedPoolLabel);
        details.setLeft(poolMembersList);
        details.setCenter(boutTable);
        details.setBottom(buildResultEntry());
        BorderPane.setMargin(selectedPoolLabel, new Insets(0, 0, 8, 0));
        BorderPane.setMargin(poolMembersList, new Insets(0, 12, 0, 0));
        BorderPane root = new BorderPane(details);
        root.setLeft(poolList);
        BorderPane.setMargin(details, new Insets(0, 0, 0, 14));
        return root;
    }

    private VBox buildResultEntry() {
        firstScoreField.setPromptText("Score");
        secondScoreField.setPromptText("Score");
        firstScoreField.setPrefWidth(70);
        secondScoreField.setPrefWidth(70);
        HBox scores = new HBox(8, selectedBoutLabel, firstScoreField, new Label("-"),
                secondScoreField, recordResultButton);
        scores.setAlignment(Pos.CENTER_LEFT);
        scores.setPadding(new Insets(12, 0, 0, 0));
        return new VBox(scores);
    }

    private VBox buildStandingsTab() {
        HBox selector = new HBox(10, new Label("Pool"), standingsPoolSelector, standingsStatusLabel);
        selector.setAlignment(Pos.CENTER_LEFT);
        VBox root = new VBox(12, selector, standingsTable);
        VBox.setVgrow(standingsTable, Priority.ALWAYS);
        return root;
    }

    private HBox buildStatusBar() {
        HBox status = new HBox(statusLabel);
        status.setPadding(new Insets(12, 0, 0, 0));
        return status;
    }

    private void configureListCells() {
        fencerList.setCellFactory(ignored -> fencerCell());
        seedList.setCellFactory(ignored -> new ListCell<>() {
            @Override protected void updateItem(Fencer fencer, boolean empty) {
                super.updateItem(fencer, empty);
                setText(empty || fencer == null ? null : (getIndex() + 1) + ". " + fencer.name());
            }
        });
        poolList.setCellFactory(ignored -> new ListCell<>() {
            @Override protected void updateItem(Pool pool, boolean empty) {
                super.updateItem(pool, empty);
                setText(empty || pool == null ? null : pool.name());
            }
        });
        standingsPoolSelector.setCellFactory(ignored -> new ListCell<>() {
            @Override protected void updateItem(Pool pool, boolean empty) {
                super.updateItem(pool, empty);
                setText(empty || pool == null ? null : pool.name());
            }
        });
        standingsPoolSelector.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Pool pool, boolean empty) {
                super.updateItem(pool, empty);
                setText(empty || pool == null ? null : pool.name());
            }
        });
    }

    private ListCell<Fencer> fencerCell() {
        return new ListCell<>() {
            @Override protected void updateItem(Fencer fencer, boolean empty) {
                super.updateItem(fencer, empty);
                setText(empty || fencer == null ? null : fencer.name());
            }
        };
    }

    private void configureTables() {
        boutTable.getColumns().addAll(
                textColumn("Fencer", PoolBoutRow::firstName),
                textColumn("Opponent", PoolBoutRow::secondName),
                textColumn("Score", PoolBoutRow::scoreText),
                textColumn("Status", PoolBoutRow::status));
        boutTable.setPlaceholder(new Label("No pool selected."));
        standingsTable.getColumns().addAll(
                textColumn("Fencer", PoolStandingRow::name),
                textColumn("Rank", row -> Integer.toString(row.rank())),
                textColumn("Bouts", row -> Integer.toString(row.bouts())),
                textColumn("Wins", row -> Integer.toString(row.wins())),
                textColumn("Ratio", row -> String.format("%.3f", row.ratio())),
                textColumn("Scored", row -> Integer.toString(row.scored())),
                textColumn("Received", row -> Integer.toString(row.received())),
                textColumn("Indicator", row -> Integer.toString(row.indicator())));
        standingsTable.setPlaceholder(new Label("No standings available."));
    }

    private static <T> TableColumn<T, String> textColumn(String title,
                                                          java.util.function.Function<T, String> value) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(value.apply(data.getValue())));
        column.setPrefWidth(100);
        return column;
    }

    private static Label boldLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        return label;
    }

    private static int indexOfPool(List<Pool> pools, java.util.UUID id) {
        for (int index = 0; index < pools.size(); index++) {
            if (pools.get(index).id().equals(id)) return index;
        }
        return 0;
    }

    private static String phaseLabel(TournamentPhase phase) {
        return switch (phase) {
            case REGISTRATION -> "Phase: Registration — add fencers and prepare seeding";
            case SEEDING -> "Phase: Seeding — order fencers and generate pools";
            case POOL_PHASE -> "Phase: Pool phase";
            case ELIMINATION_PHASE -> "Phase: Elimination";
            case COMPLETE -> "Phase: Complete";
        };
    }
}
