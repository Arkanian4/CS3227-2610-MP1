package edu.nus.cs3227.fencingtournament.ui;

import edu.nus.cs3227.fencingtournament.application.TournamentService;
import edu.nus.cs3227.fencingtournament.persistence.JsonTournamentRepository;
import javafx.application.Application;
import javafx.stage.Stage;

/** JavaFX composition root for the registration workflow. */
public final class FencingTournamentApp extends Application {
    @Override
    public void start(Stage stage) {
        TournamentView view = new TournamentView();
        TournamentService service = new TournamentService(new JsonTournamentRepository());
        new TournamentController(service, view);

        stage.setTitle("Fencing Tournament Manager");
        stage.setMinWidth(620);
        stage.setMinHeight(440);
        stage.setScene(view.scene());
        stage.show();
    }
}
