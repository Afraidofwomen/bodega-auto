package com.bodega.wms.ui.components;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class KpiCard extends VBox {
    private final Label value = new Label("—");
    private final Label hint = new Label("");

    public KpiCard(String title) {
        getStyleClass().add("card");
        Label heading = new Label(title);
        heading.getStyleClass().add("card-title");
        value.getStyleClass().add("kpi-value");
        hint.getStyleClass().add("micro");
        getChildren().addAll(heading, value, hint);
    }

    public void setValue(String text) {
        value.setText(text);
    }

    public void setHint(String text) {
        hint.setText(text);
    }

    public void setTone(String tone) {
        value.getStyleClass().removeIf(c -> c.startsWith("tone-"));
        if (tone != null) {
            value.getStyleClass().add("tone-" + tone);
        }
    }
}
