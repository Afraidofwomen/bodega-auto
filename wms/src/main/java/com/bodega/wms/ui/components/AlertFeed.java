package com.bodega.wms.ui.components;

import com.bodega.wms.model.AlertItem;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class AlertFeed extends VBox {
    private final VBox list = new VBox(6);

    public AlertFeed(ObservableList<AlertItem> alerts) {
        getStyleClass().add("card");
        Label title = new Label("Feed de alertas");
        title.getStyleClass().add("card-title");
        list.getStyleClass().add("alert-list");
        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("alert-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().addAll(title, scroll);
        render(alerts);
        alerts.addListener((ListChangeListener<AlertItem>) c -> render(alerts));
    }

    private void render(ObservableList<AlertItem> alerts) {
        list.getChildren().clear();
        int limit = Math.min(alerts.size(), 24);
        for (int i = 0; i < limit; i++) {
            list.getChildren().add(row(alerts.get(i)));
        }
    }

    private static HBox row(AlertItem item) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("alert-row", "alert-" + item.severity().name().toLowerCase());
        Label time = new Label(item.clock());
        time.getStyleClass().add("mono-num");
        Label title = new Label(item.title());
        title.getStyleClass().add("alert-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label detail = new Label(item.detail());
        detail.getStyleClass().add("micro");
        row.getChildren().addAll(time, title, spacer, detail);
        return row;
    }
}
