package edu.nus.cs3227.fencingtournament.ui;

import javafx.scene.Scene;
import javafx.scene.control.DialogPane;

import java.net.URL;

/** Applies the selected application theme to regular windows and JavaFX dialogs. */
public final class UiTheme {
    public static final String EMERALD_TEAL = "theme-emerald-teal";
    private static final String STYLESHEET_RESOURCE = "tournament.css";

    private UiTheme() {
    }

    public static void apply(Scene scene) {
        if (scene == null) return;
        if (!scene.getRoot().getStyleClass().contains(EMERALD_TEAL)) {
            scene.getRoot().getStyleClass().add(EMERALD_TEAL);
        }
        stylesheetUrl().ifPresent(url -> scene.getStylesheets().add(url.toExternalForm()));
    }

    public static void apply(DialogPane dialogPane) {
        if (dialogPane == null) return;
        if (!dialogPane.getStyleClass().contains(EMERALD_TEAL)) {
            dialogPane.getStyleClass().add(EMERALD_TEAL);
        }
        stylesheetUrl().ifPresent(url -> dialogPane.getStylesheets().add(url.toExternalForm()));
    }

    private static java.util.Optional<URL> stylesheetUrl() {
        return java.util.Optional.ofNullable(UiTheme.class.getResource(STYLESHEET_RESOURCE));
    }
}
