package edu.nus.cs3227.fencingtournament.ui;

import edu.nus.cs3227.fencingtournament.domain.Fencer;
import edu.nus.cs3227.fencingtournament.domain.Tournament;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Builds and renders the registration screen without owning tournament behaviour. */
public final class TournamentView extends BorderPane {
    private final TextField tournamentNameField = new TextField();
    private final Button createButton = new Button("Create tournament");
    private final Button loadButton = new Button("Load");
    private final Button saveButton = new Button("Save");
    private final Label tournamentNameLabel = new Label("No tournament selected");
    private final TextField fencerNameField = new TextField();
    private final Button addFencerButton = new Button("Add fencer");
    private final ListView<Fencer> fencerList = new ListView<>(FXCollections.observableArrayList());
    private final Button removeFencerButton = new Button("Remove selected");
    private final Label statusLabel = new Label("Create a tournament or load a saved one to begin.");

    public TournamentView() {
        setPadding(new Insets(18));
        setTop(buildTopBar());
        setCenter(buildRegistrationPanel());
        setBottom(buildStatusBar());

        fencerList.setPlaceholder(new Label("No fencers registered yet."));
        fencerList.setCellFactory(ignored -> new ListCell<>() {
            @Override
            protected void updateItem(Fencer fencer, boolean empty) {
                super.updateItem(fencer, empty);
                setText(empty || fencer == null ? null : fencer.name());
            }
        });
        fencerList.setPrefHeight(280);
        setRegistrationEnabled(false);
    }

    public Scene scene() {
        return new Scene(this, 760, 520);
    }

    public TextField tournamentNameField() {
        return tournamentNameField;
    }

    public Button createButton() {
        return createButton;
    }

    public Button loadButton() {
        return loadButton;
    }

    public Button saveButton() {
        return saveButton;
    }

    public TextField fencerNameField() {
        return fencerNameField;
    }

    public Button addFencerButton() {
        return addFencerButton;
    }

    public ListView<Fencer> fencerList() {
        return fencerList;
    }

    public Button removeFencerButton() {
        return removeFencerButton;
    }

    public void showTournament(Tournament tournament) {
        tournamentNameLabel.setText(tournament.name());
        fencerList.getItems().setAll(tournament.fencers());
        setRegistrationEnabled(true);
    }

    public void showStatus(String message) {
        statusLabel.setText(message);
    }

    public void setRegistrationEnabled(boolean enabled) {
        fencerNameField.setDisable(!enabled);
        addFencerButton.setDisable(!enabled);
        fencerList.setDisable(!enabled);
        removeFencerButton.setDisable(!enabled);
        saveButton.setDisable(!enabled);
    }

    private VBox buildTopBar() {
        Label title = new Label("Fencing Tournament Manager");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        tournamentNameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        HBox actions = new HBox(8, loadButton, saveButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(actions, Priority.ALWAYS);

        HBox heading = new HBox(16, title, actions);
        heading.setAlignment(Pos.CENTER_LEFT);
        VBox top = new VBox(10, heading, tournamentNameLabel, new Separator());
        top.setPadding(new Insets(0, 0, 14, 0));
        return top;
    }

    private VBox buildRegistrationPanel() {
        Label nameLabel = new Label("Tournament name");
        tournamentNameField.setPromptText("e.g. Friday Internal Open");
        tournamentNameField.setOnAction(event -> createButton.fire());
        HBox createRow = new HBox(10, nameLabel, tournamentNameField, createButton);
        createRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(tournamentNameField, Priority.ALWAYS);

        Label fencerLabel = new Label("Fencer name");
        fencerNameField.setPromptText("Enter a display name");
        fencerNameField.setOnAction(event -> addFencerButton.fire());
        HBox addRow = new HBox(10, fencerLabel, fencerNameField, addFencerButton);
        addRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(fencerNameField, Priority.ALWAYS);

        Label rosterLabel = new Label("Registered fencers");
        rosterLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        VBox roster = new VBox(8, rosterLabel, fencerList, removeFencerButton);
        VBox.setVgrow(fencerList, Priority.ALWAYS);

        VBox content = new VBox(16, createRow, new Separator(), addRow, roster);
        VBox.setVgrow(roster, Priority.ALWAYS);
        return content;
    }

    private HBox buildStatusBar() {
        HBox status = new HBox(statusLabel);
        status.setPadding(new Insets(14, 0, 0, 0));
        return status;
    }
}
