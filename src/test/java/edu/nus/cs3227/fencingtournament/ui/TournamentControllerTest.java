package edu.nus.cs3227.fencingtournament.ui;

import edu.nus.cs3227.fencingtournament.application.TournamentRepository;
import edu.nus.cs3227.fencingtournament.application.TournamentService;
import edu.nus.cs3227.fencingtournament.domain.Fencer;
import edu.nus.cs3227.fencingtournament.domain.Tournament;
import edu.nus.cs3227.fencingtournament.domain.TournamentPhase;
import edu.nus.cs3227.fencingtournament.domain.pool.BoutScore;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class TournamentControllerTest {
    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            started.countDown();
        });
        if (!started.await(10, TimeUnit.SECONDS)) fail("JavaFX toolkit did not start.");
    }

    @Test
    void generateEliminationButtonCreatesAndDisplaysBracketAfterCompletedPools() throws Exception {
        onJavaFxThread(() -> {
            TournamentService service = new TournamentService(new InMemoryRepository());
            service.createTournament("Club Open");
            List<Fencer> fencers = java.util.stream.IntStream.range(0, 4)
                    .mapToObj(index -> service.addFencer("Fencer " + (index + 1))).toList();
            service.seedFencers(fencers.stream().map(Fencer::id).toList());
            service.generatePools();
            var pool = service.pools().getFirst();
            pool.bouts().forEach(bout -> service.recordPoolBoutResult(pool.id(), bout.id(), new BoutScore(5, 0)));

            TournamentView view = new TournamentView();
            new TournamentController(service, view);

            assertFalse(view.generateEliminationButton().isDisable());
            view.generateEliminationButton().fire();

            assertNotNull(service.currentTournament().orElseThrow().eliminationBracket());
            assertSame(view.eliminationTab(), view.tabs().getSelectionModel().getSelectedItem());
        });
    }

    @Test
    void openingTournamentSelectsTheTabForItsCurrentPhase() throws Exception {
        onJavaFxThread(() -> {
            TournamentView view = new TournamentView();

            view.selectTabForPhase(TournamentPhase.REGISTRATION);
            assertSame(view.tabs().getTabs().getFirst(), view.tabs().getSelectionModel().getSelectedItem());
            view.selectTabForPhase(TournamentPhase.POOL_PHASE);
            assertSame(view.poolsTab(), view.tabs().getSelectionModel().getSelectedItem());
            view.selectTabForPhase(TournamentPhase.ELIMINATION_PHASE);
            assertSame(view.eliminationTab(), view.tabs().getSelectionModel().getSelectedItem());
            view.selectTabForPhase(TournamentPhase.COMPLETE);
            assertSame(view.finalResultsTab(), view.tabs().getSelectionModel().getSelectedItem());
        });
    }

    @Test
    void stageNavigationSelectsTheCorrespondingStage() throws Exception {
        onJavaFxThread(() -> {
            TournamentView view = new TournamentView();

            view.selectPoolsTab();

            assertSame(view.poolsTab(), view.tabs().getSelectionModel().getSelectedItem());
        });
    }

    @Test
    void stageProgressDistinguishesCompletedCurrentAndLockedStages() throws Exception {
        onJavaFxThread(() -> {
            TournamentView view = new TournamentView();

            view.setPhaseControls(TournamentPhase.POOL_PHASE, true, false, false);

            assertEquals(1, view.lookupAll(".stage-marker-complete").size());
            assertTrue(view.poolsNavigationButton().getStyleClass().contains("stage-marker-current"));
            assertTrue(view.poolsNavigationButton().getStyleClass().contains("button"));
            assertEquals(3, view.lookupAll(".stage-marker-locked").size());
            assertTrue(view.setupNavigationButton().getText().isEmpty());
            assertTrue(view.poolsNavigationButton().getText().isEmpty());
            assertEquals(2, view.lookupAll(".stage-progress-step-accessible").size());
            assertEquals(3, view.lookupAll(".stage-progress-step-locked").size());
        });
    }

    @Test
    void stageProgressRendersVisibleCircularMarkersAndLabels() throws Exception {
        onJavaFxThread(() -> {
            TournamentView view = new TournamentView();
            view.showTournamentName("Friday Epee");
            view.setPhaseControls(TournamentPhase.POOL_PHASE, true, false, false);
            view.resize(1280, 800);
            view.scene().getRoot().applyCss();
            view.layout();

            List<Node> markers = view.lookupAll(".stage-marker").stream().toList();
            List<Node> labels = view.lookupAll(".stage-progress-label").stream().toList();
            assertEquals(5, markers.size());
            assertEquals(5, labels.size());
            assertTrue(markers.stream().allMatch(Node::isVisible));
            assertTrue(labels.stream().allMatch(Node::isVisible));
            assertTrue(markers.stream().allMatch(marker -> marker.getBoundsInParent().getHeight() >= 21));
            assertTrue(view.poolsNavigationButton().getBoundsInParent().getHeight() >= 24);
            assertTrue(labels.stream().allMatch(label -> label.getBoundsInParent().getHeight() > 0));
        });
    }

    @Test
    void scoreValidationIsInlineAndClearsWhenTheInvalidFieldIsCorrected() throws Exception {
        onJavaFxThread(() -> {
            TournamentView view = new TournamentView();

            view.showEliminationValidationError("DE scores must not exceed 15.", true, false);

            assertTrue(view.eliminationValidationErrorLabel().isVisible());
            assertEquals("DE scores must not exceed 15.", view.eliminationValidationErrorLabel().getText());
            assertTrue(view.eliminationFirstScoreField().getStyleClass().contains("input-invalid"));
            assertFalse(view.eliminationSecondScoreField().getStyleClass().contains("input-invalid"));

            view.eliminationFirstScoreField().setText("15");

            assertFalse(view.eliminationValidationErrorLabel().isManaged());
            assertFalse(view.eliminationFirstScoreField().getStyleClass().contains("input-invalid"));
        });
    }

    @Test
    void tournamentAndFencerNameValidationIsShownInline() throws Exception {
        onJavaFxThread(() -> {
            TournamentService service = new TournamentService(new InMemoryRepository());
            service.createTournament("Club Open");
            TournamentView view = new TournamentView();
            new TournamentController(service, view);

            view.tournamentNameField().setText("   ");
            view.createButton().fire();
            assertEquals("Enter a tournament name.", view.tournamentNameValidationErrorLabel().getText());
            assertTrue(view.tournamentNameValidationErrorLabel().isVisible());
            assertTrue(view.tournamentNameField().getStyleClass().contains("input-invalid"));

            view.tournamentNameField().setText("club open");
            view.createButton().fire();
            assertEquals("A tournament with this name already exists.", view.tournamentNameValidationErrorLabel().getText());

            view.fencerNameField().setText(" ");
            view.addFencerButton().fire();
            assertEquals("Enter a fencer name.", view.fencerValidationErrorLabel().getText());
            assertTrue(view.fencerNameField().getStyleClass().contains("input-invalid"));

            view.fencerNameField().setText("Alice");
            view.addFencerButton().fire();
            view.fencerNameField().setText("Alice");
            view.addFencerButton().fire();
            assertEquals("A fencer with this name is already registered.", view.fencerValidationErrorLabel().getText());
        });
    }

    @Test
    void viewingAnEarlierStageDoesNotChangeTheTournamentProgressMarker() throws Exception {
        onJavaFxThread(() -> {
            TournamentView view = new TournamentView();
            view.setPhaseControls(TournamentPhase.ELIMINATION_PHASE, true, true, true);
            view.selectEliminationTab();

            assertEquals(1, view.lookupAll(".stage-progress-viewing").size());

            List<Button> markers = view.lookupAll(".stage-marker").stream()
                    .map(Button.class::cast).toList();
            markers.get(2).fire();

            assertTrue(markers.get(3).getStyleClass().contains("stage-marker-current"));
            assertFalse(markers.get(2).getStyleClass().contains("stage-marker-current"));
            assertEquals(1, view.lookupAll(".stage-progress-viewing").size());
        });
    }

    @Test
    void headerShowsTournamentContextOnlyWhenATournamentIsOpen() throws Exception {
        onJavaFxThread(() -> {
            TournamentView view = new TournamentView();
            Node context = view.lookup(".tournament-context");

            assertNotNull(context);
            assertFalse(context.isManaged());

            view.showTournamentName("Friday Epee");
            view.showPhase(TournamentPhase.POOL_PHASE, null);

            assertTrue(context.isManaged());
        });
    }

    @Test
    void workspaceAttachesTheSelectedThemeToItsScene() throws Exception {
        onJavaFxThread(() -> {
            TournamentView view = new TournamentView();
            Scene scene = view.scene();

            assertTrue(view.getStyleClass().contains(UiTheme.selectedTheme().styleClass()));
            assertTrue(scene.getStylesheets().stream().anyMatch(stylesheet -> stylesheet.endsWith("tournament.css")));
        });
    }

    @Test
    void sidebarThemeSwatchesApplyAndRememberTheSelectedTheme() throws Exception {
        onJavaFxThread(() -> {
            UiTheme.Theme originalTheme = UiTheme.selectedTheme();
            try {
                TournamentView view = new TournamentView();
                Scene scene = view.scene();
                scene.getRoot().applyCss();

                List<Button> swatches = view.lookupAll(".theme-swatch").stream().map(Button.class::cast).toList();
                assertEquals(UiTheme.Theme.values().length, swatches.size());
                swatches.get(2).fire();

                assertEquals(UiTheme.Theme.ROYAL_PURPLE_VIOLET, UiTheme.selectedTheme());
                assertTrue(view.getStyleClass().contains(UiTheme.Theme.ROYAL_PURPLE_VIOLET.styleClass()));
                assertEquals(1, view.lookupAll(".theme-swatch-selected").size());
            } finally {
                UiTheme.selectTheme(originalTheme);
            }
        });
    }

    @Test
    void appearanceSwitchesIndependentlyFromTheColourTheme() throws Exception {
        onJavaFxThread(() -> {
            UiTheme.Appearance originalAppearance = UiTheme.selectedAppearance();
            try {
                TournamentView view = new TournamentView();
                javafx.scene.control.ToggleButton toggle = (javafx.scene.control.ToggleButton) view.lookup(".appearance-switch-control");
                assertNotNull(toggle);
                assertEquals(2, UiTheme.Appearance.values().length);

                if (toggle.isSelected()) toggle.fire();
                toggle.fire();
                assertEquals(UiTheme.Appearance.DARK, UiTheme.selectedAppearance());
                assertTrue(view.getStyleClass().contains(UiTheme.Appearance.DARK.styleClass()));

                toggle.fire();
                assertEquals(UiTheme.Appearance.LIGHT, UiTheme.selectedAppearance());
                assertTrue(view.getStyleClass().contains(UiTheme.Appearance.LIGHT.styleClass()));
            } finally {
                UiTheme.selectAppearance(originalAppearance);
            }
        });
    }

    @Test
    void laterEliminationRoundIsAvailableWhenBothCompetitorsAreKnown() {
        EliminationMatchRow availableQuarterFinal = new EliminationMatchRow(UUID.randomUUID(), 2, 0,
                new EliminationParticipant(1, "Jee Ken", "", false, false, false),
                new EliminationParticipant(8, "Tom", "", false, false, false),
                false, false, false);
        EliminationMatchRow unresolvedQuarterFinal = new EliminationMatchRow(UUID.randomUUID(), 2, 1,
                new EliminationParticipant(0, "Awaiting opponent", "", false, false, true),
                new EliminationParticipant(5, "Alex", "", false, false, false),
                false, false, false);

        assertTrue(TournamentView.isAvailableEliminationBout(availableQuarterFinal));
        assertFalse(TournamentView.isAvailableEliminationBout(unresolvedQuarterFinal));
    }

    @Test
    void poolResultStatusesRetainDistinctSuccessAndDangerColours() throws Exception {
        onJavaFxThread(() -> {
            TournamentView view = new TournamentView();
            view.renderOverallSeeding(List.of(
                    new OverallSeedingRow("Advanced fencer", 1, 4, 4, 1.0, 20, 4, 16, 1),
                    new OverallSeedingRow("Eliminated fencer", 17, 1, 4, 0.25, 8, 18, -10, 17)));
            view.selectPoolResultTab();
            Scene scene = view.scene();
            scene.getRoot().applyCss();

            Label advanced = (Label) scene.getRoot().lookup(".pool-status-advanced");
            Label eliminated = (Label) scene.getRoot().lookup(".pool-status-eliminated");
            assertNotNull(advanced);
            assertNotNull(eliminated);
            assertFalse(advanced.getTextFill().equals(eliminated.getTextFill()));
        });
    }

    @Test
    void tournamentHomeUsesOverflowMenusForLowEmphasisDeletion() throws Exception {
        onJavaFxThread(() -> {
            TournamentService service = new TournamentService(new InMemoryRepository());
            service.createTournament("Friday Open");
            service.createTournament("Saturday Open");
            TournamentView view = new TournamentView();
            view.showHome();
            view.renderTournamentList(service.listTournaments());
            Scene scene = view.scene();
            scene.getRoot().applyCss();
            scene.getRoot().layout();

            List<Node> menus = scene.getRoot().lookupAll(".home-overflow-menu").stream().toList();
            assertEquals(2, menus.size());
            javafx.scene.control.Button firstMenu = (javafx.scene.control.Button) menus.getFirst();
            assertEquals("…", firstMenu.getText());
            assertEquals("More actions", firstMenu.getAccessibleText());
        });
    }

    @Test
    void tournamentHomeSuppressesScrollbarsForTwoOrFewerTournaments() throws Exception {
        onJavaFxThread(() -> {
            TournamentService service = new TournamentService(new InMemoryRepository());
            service.createTournament("Friday Open");
            service.createTournament("Saturday Open");
            TournamentView view = new TournamentView();
            view.showHome();
            view.renderTournamentList(service.listTournaments());
            Scene scene = view.scene();
            javafx.scene.control.ScrollPane scroll = (javafx.scene.control.ScrollPane) scene.getRoot().lookup(".home-tournament-scroll");

            assertEquals(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER, scroll.getHbarPolicy());
            assertEquals(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER, scroll.getVbarPolicy());

            service.createTournament("Sunday Open");
            view.renderTournamentList(service.listTournaments());
            assertEquals(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED, scroll.getVbarPolicy());
        });
    }

    @Test
    void poolsDashboardRendersAllPoolPanelsTogether() throws Exception {
        onJavaFxThread(() -> {
            TournamentView view = new TournamentView();
            UUID alice = UUID.randomUUID();
            UUID ben = UUID.randomUUID();
            UUID firstPool = UUID.randomUUID();
            LinkedHashMap<UUID, String> aliceCells = new LinkedHashMap<>();
            aliceCells.put(alice, "—"); aliceCells.put(ben, "5");
            LinkedHashMap<UUID, String> benCells = new LinkedHashMap<>();
            benCells.put(alice, "1"); benCells.put(ben, "—");
            List<PoolMatrixRow> rows = List.of(
                    new PoolMatrixRow(alice, "Alice", aliceCells),
                    new PoolMatrixRow(ben, "Ben", benCells));
            AtomicReference<PoolMatrixSelection> selection = new AtomicReference<>();
            view.setMatrixCellHandler(selection::set);

            view.renderPoolDashboard(List.of(
                    new PoolDashboardPanel(firstPool, "POOL #1", 2, 1, 1, rows),
                    new PoolDashboardPanel(UUID.randomUUID(), "POOL #2", 2, 0, 1, rows)));

            assertEquals(1, view.poolDashboard().getChildren().size());
            javafx.scene.layout.HBox twoPoolRow = (javafx.scene.layout.HBox) view.poolDashboard().getChildren().getFirst();
            assertEquals(2, twoPoolRow.getChildren().size());
            VBox firstPanel = (VBox) twoPoolRow.getChildren().getFirst();
            GridPane matrix = (GridPane) firstPanel.getChildren().get(1);
            Label firstResult = matrix.getChildren().stream().filter(Label.class::isInstance)
                    .map(Label.class::cast).filter(label -> "V5".equals(label.getText())).findFirst().orElseThrow();
            firstResult.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0,
                    javafx.scene.input.MouseButton.PRIMARY, 1, false, false, false, false, true,
                    false, false, true, false, false, null));
            assertEquals(new PoolMatrixSelection(firstPool, alice, ben), selection.get());
        });
    }

    @Test
    void lowerHalfPoolMatrixSelectionKeepsItsFencerAndScoreOrientationAfterSaving() throws Exception {
        onJavaFxThread(() -> {
            TournamentService service = new TournamentService(new InMemoryRepository());
            service.createTournament("Club Open");
            Fencer jackie = service.addFencer("Jackie");
            Fencer charlie = service.addFencer("Charlie");
            service.seedFencers(List.of(jackie.id(), charlie.id()));
            service.generatePools();

            TournamentView view = new TournamentView();
            new TournamentController(service, view);
            VBox panel = (VBox) view.poolDashboard().getChildren().getFirst();
            GridPane matrix = (GridPane) panel.getChildren().get(1);
            Label charlieVsJackie = matrix.getChildren().stream().filter(Label.class::isInstance).map(Label.class::cast)
                    .filter(cell -> Integer.valueOf(2).equals(GridPane.getRowIndex(cell))
                            && Integer.valueOf(1).equals(GridPane.getColumnIndex(cell)))
                    .findFirst().orElseThrow();

            charlieVsJackie.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0,
                    javafx.scene.input.MouseButton.PRIMARY, 1, false, false, false, false, true,
                    false, false, true, false, false, null));
            view.firstScoreField().setText("5");
            view.secondScoreField().setText("2");
            view.recordResultButton().fire();

            assertEquals(new BoutScore(2, 5), service.pools().getFirst().bouts().getFirst().score());
            view.editPoolResultButton().fire();
            assertEquals("5", view.firstScoreField().getText());
            assertEquals("2", view.secondScoreField().getText());
        });
    }

    @Test
    void twoPoolBoardUsesTheCompactSideBySideLayoutMode() throws Exception {
        onJavaFxThread(() -> {
            TournamentView view = new TournamentView();
            List<PoolMatrixRow> rows = eightPersonMatrixRows();

            view.renderPoolDashboard(List.of(
                    new PoolDashboardPanel(UUID.randomUUID(), "POOL #1", 8, 0, 28, rows),
                    new PoolDashboardPanel(UUID.randomUUID(), "POOL #2", 8, 0, 28, rows)));

            assertTrue(view.poolDashboard().getStyleClass().contains("two-pool-dashboard"));
            assertEquals(12.0, view.poolDashboard().getHgap());
            assertEquals(1, view.poolDashboard().getChildren().size());
            assertEquals(2, ((javafx.scene.layout.HBox) view.poolDashboard().getChildren().getFirst()).getChildren().size());
        });
    }

    @Test
    void poolBoardExpandsItsWrapWidthToTheDesktopViewport() throws Exception {
        onJavaFxThread(() -> {
            TournamentView view = new TournamentView();
            view.selectPoolsTab();
            view.scene();
            view.resize(1440, 900);
            view.applyCss();
            view.layout();
            view.renderPoolDashboard(List.of(
                    new PoolDashboardPanel(UUID.randomUUID(), "POOL #1", 8, 0, 28, eightPersonMatrixRows()),
                    new PoolDashboardPanel(UUID.randomUUID(), "POOL #2", 8, 0, 28, eightPersonMatrixRows())));
            view.layout();

            assertTrue(view.poolDashboard().getPrefWidth() > 1_000);
        });
    }

    @Test
    void escapeDismissesTheSelectedPoolBoutOnce() throws Exception {
        onJavaFxThread(() -> {
            TournamentView view = new TournamentView();
            java.util.concurrent.atomic.AtomicInteger dismissals = new java.util.concurrent.atomic.AtomicInteger();
            UUID poolId = UUID.randomUUID();
            UUID rowId = UUID.randomUUID();
            UUID opponentId = UUID.randomUUID();
            view.setPoolSelectionDismissHandler(() -> {
                dismissals.incrementAndGet();
                view.clearSelectedMatrixCell();
            });
            view.markSelectedMatrixCell(poolId, rowId, opponentId);
            VBox poolsContent = (VBox) ((javafx.scene.layout.BorderPane) view.poolsTab().getContent()).getCenter();

            poolsContent.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE,
                    false, false, false, false));
            poolsContent.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE,
                    false, false, false, false));

            assertEquals(1, dismissals.get());
        });
    }

    @Test
    void registrationRosterUsesCompactHeightForSmallFields() throws Exception {
        onJavaFxThread(() -> {
            TournamentView view = new TournamentView();
            List<Fencer> fencers = List.of(
                    new Fencer(UUID.randomUUID(), "Alice"),
                    new Fencer(UUID.randomUUID(), "Ben"),
                    new Fencer(UUID.randomUUID(), "Chloe"));

            view.renderFencers(fencers, fencers);

            assertEquals(122.0, view.fencerList().getPrefHeight());
        });
    }

    @Test
    void seedingControlsOfferEightAsAMaximumPoolSize() throws Exception {
        onJavaFxThread(() -> {
            TournamentView view = new TournamentView();

            assertTrue(view.maximumPoolSizeChoice().getItems().contains(8));
        });
    }

    @Test
    void movedSeedOrderIsUsedWhenGeneratingPools() throws Exception {
        onJavaFxThread(() -> {
            TournamentService service = new TournamentService(new InMemoryRepository());
            service.createTournament("Club Open");
            Fencer alice = service.addFencer("Alice");
            Fencer ben = service.addFencer("Ben");
            service.seedFencers(List.of(alice.id(), ben.id()));
            TournamentView view = new TournamentView();
            new TournamentController(service, view);

            view.seedList().getSelectionModel().select(ben);
            view.moveSeedUpButton().fire();
            view.generatePoolsButton().fire();

            assertEquals(List.of(ben.id(), alice.id()), service.currentTournament().orElseThrow().seeding().fencerIds());
            assertEquals(TournamentPhase.POOL_PHASE, service.currentPhase());
        });
    }

    private static void onJavaFxThread(ThrowingRunnable action) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch completed = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                completed.countDown();
            }
        });
        if (!completed.await(10, TimeUnit.SECONDS)) fail("JavaFX action did not complete.");
        if (failure.get() instanceof Exception exception) throw exception;
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    private static List<PoolMatrixRow> eightPersonMatrixRows() {
        List<UUID> ids = java.util.stream.IntStream.range(0, 8).mapToObj(ignored -> UUID.randomUUID()).toList();
        return ids.stream().map(id -> {
            LinkedHashMap<UUID, String> cells = new LinkedHashMap<>();
            ids.forEach(opponent -> cells.put(opponent, id.equals(opponent) ? "—" : ""));
            return new PoolMatrixRow(id, "Fencer", cells);
        }).toList();
    }

    private interface ThrowingRunnable { void run() throws Exception; }

    private static final class InMemoryRepository implements TournamentRepository {
        @Override
        public Optional<Tournament> load(Path path) { return Optional.empty(); }

        @Override
        public void save(Tournament tournament, Path path) throws IOException { }

        @Override
        public void delete(Path path) throws IOException { }
    }
}
