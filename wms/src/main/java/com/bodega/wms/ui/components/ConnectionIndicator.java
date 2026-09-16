package com.bodega.wms.ui.components;

import com.bodega.wms.model.ConnectionState;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

public final class ConnectionIndicator extends HBox {
    private final Region dot = new Region();
    private final Label label = new Label();

    public ConnectionIndicator() {
        getStyleClass().add("connection-indicator");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);
        dot.getStyleClass().addAll("conn-dot", "conn-offline");
        label.getStyleClass().add("conn-label");
        getChildren().addAll(dot, label);
        setState(ConnectionState.OFFLINE, "Sin enlace");
    }

    public void setState(ConnectionState state, String detail) {
        dot.getStyleClass().removeIf(c -> c.startsWith("conn-") && !"conn-dot".equals(c));
        dot.getStyleClass().add("conn-" + state.name().toLowerCase());
        String extra = detail == null || detail.isBlank() ? "" : " · " + detail;
        label.setText(state.label() + extra);
    }
}
