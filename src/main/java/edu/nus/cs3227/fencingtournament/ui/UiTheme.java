package edu.nus.cs3227.fencingtournament.ui;

import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.DialogPane;

import java.net.URL;
import java.util.Arrays;
import java.util.prefs.Preferences;

/** Applies the selected application theme to regular windows and JavaFX dialogs. */
public final class UiTheme {
    public static final String EMERALD_TEAL = "theme-emerald-teal";
    private static final String THEME_PREFERENCE_KEY = "selected-theme";
    private static final String APPEARANCE_PREFERENCE_KEY = "selected-appearance";
    private static final String STYLESHEET_RESOURCE = "tournament.css";
    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(UiTheme.class);
    private static Theme selectedTheme = loadSelectedTheme();
    private static Appearance selectedAppearance = loadSelectedAppearance();

    private UiTheme() {
    }

    public static void apply(Scene scene) {
        if (scene == null) return;
        apply(scene.getRoot());
        stylesheetUrl().map(URL::toExternalForm).filter(url -> !scene.getStylesheets().contains(url))
                .ifPresent(scene.getStylesheets()::add);
    }

    public static void apply(DialogPane dialogPane) {
        if (dialogPane == null) return;
        apply((Node) dialogPane);
        stylesheetUrl().map(URL::toExternalForm).filter(url -> !dialogPane.getStylesheets().contains(url))
                .ifPresent(dialogPane.getStylesheets()::add);
    }

    /** Applies the selected theme tokens to a visible root node. */
    public static void apply(Node node) {
        if (node == null) return;
        node.getStyleClass().removeAll(Arrays.stream(Theme.values()).map(Theme::styleClass).toList());
        node.getStyleClass().removeAll(Arrays.stream(Appearance.values()).map(Appearance::styleClass).toList());
        node.getStyleClass().add(selectedTheme.styleClass());
        node.getStyleClass().add(selectedAppearance.styleClass());
    }

    public static Theme selectedTheme() {
        return selectedTheme;
    }

    /** Stores an appearance preference only; it never changes tournament data. */
    public static void selectTheme(Theme theme) {
        selectedTheme = theme == null ? Theme.EMERALD_TEAL : theme;
        PREFERENCES.put(THEME_PREFERENCE_KEY, selectedTheme.name());
    }

    public static Appearance selectedAppearance() {
        return selectedAppearance;
    }

    /** Persists the requested appearance independently from the selected colour family. */
    public static void selectAppearance(Appearance appearance) {
        selectedAppearance = appearance == null ? Appearance.LIGHT : appearance;
        PREFERENCES.put(APPEARANCE_PREFERENCE_KEY, selectedAppearance.name());
    }

    public enum Theme {
        EMERALD_TEAL("Emerald & Teal", "theme-emerald-teal"),
        DEEP_NAVY_ELECTRIC_BLUE("Deep Navy & Electric Blue", "theme-deep-navy-electric-blue"),
        ROYAL_PURPLE_VIOLET("Royal Purple & Violet", "theme-royal-purple-violet"),
        ICE_BLUE_CERULEAN("Ice Blue & Cerulean", "theme-ice-blue-cerulean");

        private final String displayName;
        private final String styleClass;

        Theme(String displayName, String styleClass) {
            this.displayName = displayName;
            this.styleClass = styleClass;
        }

        public String displayName() {
            return displayName;
        }

        public String styleClass() {
            return styleClass;
        }
    }

    public enum Appearance {
        LIGHT("Light", "appearance-light"),
        DARK("Dark", "appearance-dark");

        private final String displayName;
        private final String styleClass;

        Appearance(String displayName, String styleClass) {
            this.displayName = displayName;
            this.styleClass = styleClass;
        }

        public String displayName() {
            return displayName;
        }

        public String styleClass() {
            return styleClass;
        }
    }

    private static Theme loadSelectedTheme() {
        String savedName = PREFERENCES.get(THEME_PREFERENCE_KEY, Theme.EMERALD_TEAL.name());
        try {
            return Theme.valueOf(savedName);
        } catch (IllegalArgumentException exception) {
            return Theme.EMERALD_TEAL;
        }
    }

    private static Appearance loadSelectedAppearance() {
        String savedName = PREFERENCES.get(APPEARANCE_PREFERENCE_KEY, Appearance.LIGHT.name());
        try {
            return Appearance.valueOf(savedName);
        } catch (IllegalArgumentException exception) {
            return Appearance.LIGHT;
        }
    }

    private static java.util.Optional<URL> stylesheetUrl() {
        return java.util.Optional.ofNullable(UiTheme.class.getResource(STYLESHEET_RESOURCE));
    }
}
