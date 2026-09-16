package com.bodega.wms.ui;

import javafx.scene.Parent;
import javafx.scene.Scene;

public final class ThemeManager {
    private ThemeManager() {
    }

    public static void apply(Scene scene, boolean dark) {
        scene.getStylesheets().clear();
        scene.getStylesheets().add(resource("/css/app.css"));
        Parent root = scene.getRoot();
        root.getStyleClass().removeAll("theme-light", "theme-dark");
        root.getStyleClass().add(dark ? "theme-dark" : "theme-light");
    }

    private static String resource(String path) {
        var url = ThemeManager.class.getResource(path);
        if (url == null) {
            throw new IllegalStateException("Recurso ausente: " + path);
        }
        return url.toExternalForm();
    }
}
