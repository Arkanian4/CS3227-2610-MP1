package edu.nus.cs3227.fencingtournament.ui;

import edu.nus.cs3227.fencingtournament.application.TournamentService;
import edu.nus.cs3227.fencingtournament.persistence.JsonTournamentRepository;
import javafx.application.Application;
import javafx.stage.Stage;
import java.nio.file.Path;
import java.io.IOException;

/** JavaFX composition root for the local tournament-management application. */
public final class FencingTournamentApp extends Application {
    @Override
    public void start(Stage stage) {
        TournamentView view = new TournamentView();
        TournamentService service = new TournamentService(new JsonTournamentRepository(), Path.of("tournaments"));
        try {
            service.loadAll(Path.of("tournaments"));
        } catch (IOException exception) {
            view.showStatus("Some saved tournaments could not be loaded: " + exception.getMessage());
        }
        new TournamentController(service, view);

        stage.setTitle("Fencing Tournament Manager");
        stage.setMinWidth(960);
        stage.setMinHeight(680);
        stage.setScene(view.scene());
        stage.show();
    }
}
