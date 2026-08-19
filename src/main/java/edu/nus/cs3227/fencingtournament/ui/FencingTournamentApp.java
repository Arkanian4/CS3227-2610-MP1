package edu.nus.cs3227.fencingtournament.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/** Initial JavaFX shell. Tournament views will be introduced in later increments. */
public final class FencingTournamentApp extends Application {
    @Override
    public void start(Stage stage) {
        Label message = new Label("Fencing Tournament Manager\nProject skeleton");
        StackPane root = new StackPane(message);

        stage.setTitle("Fencing Tournament Manager");
        stage.setScene(new Scene(root, 720, 480));
        stage.show();
    }
}
