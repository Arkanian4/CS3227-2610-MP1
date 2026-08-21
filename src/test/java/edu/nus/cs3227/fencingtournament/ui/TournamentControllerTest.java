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
    void sidebarNavigationSelectsTheCorrespondingStage() throws Exception {
        onJavaFxThread(() -> {
            TournamentView view = new TournamentView();

            view.selectPoolsTab();

            assertSame(view.poolsTab(), view.tabs().getSelectionModel().getSelectedItem());
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
    void workspaceAttachesTheEmeraldTealThemeToItsScene() throws Exception {
        onJavaFxThread(() -> {
            TournamentView view = new TournamentView();
            Scene scene = view.scene();

            assertTrue(view.getStyleClass().contains(UiTheme.EMERALD_TEAL));
            assertTrue(scene.getStylesheets().stream().anyMatch(stylesheet -> stylesheet.endsWith("tournament.css")));
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
