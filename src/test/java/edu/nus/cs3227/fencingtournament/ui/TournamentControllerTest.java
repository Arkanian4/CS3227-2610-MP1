package edu.nus.cs3227.fencingtournament.ui;

import edu.nus.cs3227.fencingtournament.application.TournamentRepository;
import edu.nus.cs3227.fencingtournament.application.TournamentService;
import edu.nus.cs3227.fencingtournament.domain.Fencer;
import edu.nus.cs3227.fencingtournament.domain.Tournament;
import edu.nus.cs3227.fencingtournament.domain.pool.BoutScore;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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

    private interface ThrowingRunnable { void run() throws Exception; }

    private static final class InMemoryRepository implements TournamentRepository {
        @Override
        public Optional<Tournament> load(Path path) { return Optional.empty(); }

        @Override
        public void save(Tournament tournament, Path path) throws IOException { }
    }
}
