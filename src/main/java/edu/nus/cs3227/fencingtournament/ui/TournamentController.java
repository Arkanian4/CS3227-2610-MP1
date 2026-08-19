package edu.nus.cs3227.fencingtournament.ui;

import edu.nus.cs3227.fencingtournament.application.TournamentService;
import edu.nus.cs3227.fencingtournament.domain.Fencer;
import edu.nus.cs3227.fencingtournament.domain.Tournament;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/** Connects JavaFX events to the application service. */
public final class TournamentController {
    private final TournamentService service;
    private final TournamentView view;
    private Path currentFile;

    public TournamentController(TournamentService service, TournamentView view) {
        this.service = service;
        this.view = view;
        wireActions();
    }

    private void wireActions() {
        view.createButton().setOnAction(event -> createTournament());
        view.loadButton().setOnAction(event -> loadTournament());
        view.saveButton().setOnAction(event -> saveTournament());
        view.addFencerButton().setOnAction(event -> addFencer());
        view.removeFencerButton().setOnAction(event -> removeSelectedFencer());
    }

    private void createTournament() {
        try {
            Tournament tournament = service.createTournament(view.tournamentNameField().getText());
            currentFile = null;
            view.showTournament(tournament);
            view.tournamentNameField().clear();
            view.showStatus("Tournament created. Add fencers to begin registration.");
        } catch (IllegalArgumentException exception) {
            view.showStatus(exception.getMessage());
        }
    }

    private void loadTournament() {
        FileChooser chooser = jsonFileChooser("Open tournament");
        var selectedFile = chooser.showOpenDialog(view.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        try {
            Optional<Tournament> loaded = service.loadTournament(selectedFile.toPath());
            if (loaded.isEmpty()) {
                view.showStatus("The selected tournament file does not exist.");
                return;
            }
            currentFile = selectedFile.toPath();
            view.showTournament(loaded.orElseThrow());
            view.showStatus("Tournament loaded.");
        } catch (IOException | IllegalArgumentException exception) {
            view.showStatus("Could not load tournament: " + exception.getMessage());
        }
    }

    private void saveTournament() {
        Path path = currentFile;
        if (path == null) {
            FileChooser chooser = jsonFileChooser("Save tournament");
            var selectedFile = chooser.showSaveDialog(view.getScene().getWindow());
            if (selectedFile == null) {
                return;
            }
            path = selectedFile.toPath();
        }

        try {
            service.saveTournament(path);
            currentFile = path;
            view.showStatus("Tournament saved.");
        } catch (IOException | IllegalStateException | IllegalArgumentException exception) {
            view.showStatus("Could not save tournament: " + exception.getMessage());
        }
    }

    private void addFencer() {
        try {
            service.addFencer(view.fencerNameField().getText());
            refreshTournament();
            view.fencerNameField().clear();
            view.showStatus("Fencer added.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            view.showStatus(exception.getMessage());
        }
    }

    private void removeSelectedFencer() {
        Fencer selected = view.fencerList().getSelectionModel().getSelectedItem();
        if (selected == null) {
            view.showStatus("Select a fencer to remove.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove " + selected.name() + " from this tournament?",
                ButtonType.CANCEL, ButtonType.OK);
        confirmation.setTitle("Remove fencer");
        confirmation.setHeaderText(null);
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            service.removeFencer(selected.id());
            refreshTournament();
            view.showStatus("Fencer removed.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            view.showStatus(exception.getMessage());
        }
    }

    private void refreshTournament() {
        service.currentTournament().ifPresent(view::showTournament);
    }

    private static FileChooser jsonFileChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Tournament JSON (*.json)", "*.json"));
        return chooser;
    }
}
