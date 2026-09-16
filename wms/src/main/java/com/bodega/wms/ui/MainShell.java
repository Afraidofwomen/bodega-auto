package com.bodega.wms.ui;

import com.bodega.wms.model.SrmStatus;
import com.bodega.wms.state.AppStore;
import com.bodega.wms.ui.components.ConnectionIndicator;
import com.bodega.wms.ui.components.StatusBadge;
import com.bodega.wms.ui.views.ConnectionView;
import com.bodega.wms.ui.views.DashboardView;
import com.bodega.wms.ui.views.OperatorView;
import com.bodega.wms.ui.views.WarehouseMapView;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class MainShell extends BorderPane {
    public MainShell(AppStore store) {
        getStyleClass().add("shell");

        Label brand = new Label("WMS");
        brand.getStyleClass().add("brand");
        Label product = new Label("Bodega automática");
        product.getStyleClass().add("brand-sub");
        VBox brandBox = new VBox(2, brand, product);

        Button dash = nav("Dashboard");
        Button map = nav("Mapa");
        Button op = nav("Operario");
        Button link = nav("Conexión");

        VBox nav = new VBox(8, brandBox, dash, map, op, link);
        nav.getStyleClass().add("nav");

        StatusBadge badge = new StatusBadge();
        ConnectionIndicator conn = new ConnectionIndicator();
        ToggleButton theme = new ToggleButton("Oscuro");
        theme.getStyleClass().add("theme-toggle");
        theme.setSelected(store.darkThemeProperty().get());
        theme.setOnAction(e -> store.darkThemeProperty().set(theme.isSelected()));
        store.darkThemeProperty().addListener((obs, o, n) -> theme.setText(n ? "Oscuro" : "Claro"));

        Label device = new Label("srm1");
        device.getStyleClass().add("mono-num");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(16, badge, device, spacer, conn, theme);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("topbar");

        DashboardView dashboardView = new DashboardView(store);
        WarehouseMapView mapView = new WarehouseMapView(store);
        OperatorView operatorView = new OperatorView(store);
        ConnectionView connectionView = new ConnectionView(store);
        StackPane pages = new StackPane(dashboardView, mapView, operatorView, connectionView);
        pages.getStyleClass().add("pages");
        show(pages, dashboardView);

        dash.setOnAction(e -> show(pages, dashboardView));
        map.setOnAction(e -> show(pages, mapView));
        op.setOnAction(e -> show(pages, operatorView));
        link.setOnAction(e -> show(pages, connectionView));

        Region flash = new Region();
        flash.getStyleClass().add("flash-overlay");
        flash.setMouseTransparent(true);
        flash.setVisible(false);
        StackPane center = new StackPane(pages, flash);

        setLeft(nav);
        setTop(top);
        setCenter(center);

        store.statusProperty().addListener((obs, o, n) -> {
            badge.setState(n.state());
            device.setText(n.deviceId());
            applyEstop(n);
        });
        store.connectionProperty().addListener((obs, o, n) ->
                conn.setState(n, store.connectionDetailProperty().get()));
        store.connectionDetailProperty().addListener((obs, o, n) ->
                conn.setState(store.connectionProperty().get(), n));
        store.flashKindProperty().addListener((obs, o, n) -> {
            flash.getStyleClass().removeAll("flash-ok", "flash-err");
            if (n == null) {
                flash.setVisible(false);
            } else {
                flash.getStyleClass().add("ok".equals(n) ? "flash-ok" : "flash-err");
                flash.setVisible(true);
            }
        });
        conn.setState(store.connectionProperty().get(), store.connectionDetailProperty().get());
    }

    private void applyEstop(SrmStatus status) {
        getStyleClass().remove("estop-active");
        if (status.estop()) {
            getStyleClass().add("estop-active");
        }
    }

    private static Button nav(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("nav-btn");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(48);
        return button;
    }

    private static void show(StackPane pages, Region target) {
        pages.getChildren().forEach(n -> n.setVisible(n == target));
    }
}
