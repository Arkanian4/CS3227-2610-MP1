package edu.nus.cs3227.fencingtournament;

import edu.nus.cs3227.fencingtournament.ui.FencingTournamentApp;
import javafx.application.Application;

/** Launches the JavaFX application without placing JavaFX bootstrap logic in the UI class. */
public final class AppLauncher {
    private AppLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(FencingTournamentApp.class, args);
    }
}
