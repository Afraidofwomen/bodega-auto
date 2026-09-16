package com.bodega.wms.ui.views;

import com.bodega.wms.model.Inventory;
import com.bodega.wms.model.SrmStatus;
import com.bodega.wms.state.AppStore;
import com.bodega.wms.ui.components.AlertFeed;
import com.bodega.wms.ui.components.KpiCard;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;

public final class DashboardView extends VBox {
    private final KpiCard occupancy = new KpiCard("Ocupación");
    private final KpiCard pending = new KpiCard("Órdenes / misión");
    private final KpiCard docks = new KpiCard("Andén I/O");
    private final KpiCard srm = new KpiCard("Estado SRM");
    private final Label position = new Label("X 0  Y 0  Z 0");
    private final Label sensors = new Label("—");
    private final XYChart.Series<String, Number> series = new XYChart.Series<>();

    public DashboardView(AppStore store) {
        getStyleClass().add("page");
        Label title = new Label("Dashboard de control");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("KPIs en tiempo real · mini AS/RS 32 slots");
        subtitle.getStyleClass().add("page-subtitle");

        HBox kpis = new HBox(12, occupancy, pending, docks, srm);
        kpis.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        occupancy.setMaxWidth(Double.MAX_VALUE);
        pending.setMaxWidth(Double.MAX_VALUE);
        docks.setMaxWidth(Double.MAX_VALUE);
        srm.setMaxWidth(Double.MAX_VALUE);

        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis(0, 16, 4);
        y.setTickUnit(4);
        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setTitle("Ocupación por zona");
        chart.getStyleClass().add("occupancy-chart");
        series.setName("Ocupados");
        series.getData().add(new XYChart.Data<>("Rack A", 0));
        series.getData().add(new XYChart.Data<>("Rack B", 0));
        series.getData().add(new XYChart.Data<>("I/O", 0));
        chart.getData().add(series);
        VBox chartCard = new VBox(chart);
        chartCard.getStyleClass().add("card");
        VBox.setVgrow(chart, Priority.ALWAYS);

        AlertFeed feed = new AlertFeed(store.alerts());

        GridPane mid = new GridPane();
        mid.setHgap(12);
        mid.setVgap(12);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(62);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(38);
        mid.getColumnConstraints().addAll(c1, c2);
        RowConstraints row = new RowConstraints();
        row.setVgrow(Priority.ALWAYS);
        mid.getRowConstraints().add(row);
        mid.add(chartCard, 0, 0);
        mid.add(feed, 1, 0);
        GridPane.setHgrow(chartCard, Priority.ALWAYS);
        GridPane.setVgrow(chartCard, Priority.ALWAYS);
        GridPane.setVgrow(feed, Priority.ALWAYS);
        VBox.setVgrow(mid, Priority.ALWAYS);

        position.getStyleClass().addAll("mono-num", "telemetry-pos");
        sensors.getStyleClass().add("telemetry-sensors");
        Label telTitle = new Label("Telemetría");
        telTitle.getStyleClass().add("card-title");
        HBox telBody = new HBox(24, position, sensors);
        telBody.setAlignment(Pos.CENTER_LEFT);
        VBox telemetry = new VBox(8, telTitle, telBody);
        telemetry.getStyleClass().add("card");

        getChildren().addAll(title, subtitle, kpis, mid, telemetry);
        setPadding(new Insets(8, 4, 4, 4));
        setSpacing(14);

        store.statusProperty().addListener((obs, o, n) -> bindStatus(n));
        store.inventoryProperty().addListener((obs, o, n) -> bindInventory(n, store.status()));
        bindStatus(store.status());
        bindInventory(store.inventory(), store.status());
    }

    private void bindStatus(SrmStatus status) {
        pending.setValue(status.state().busy() ? "1" : "0");
        pending.setHint(status.mission() == null ? "sin misión" : status.mission());
        pending.setTone(status.state().busy() ? "process" : "ok");

        docks.setValue(status.ioOccupied() ? "OCUPADO" : "LIBRE");
        docks.setHint(status.forkOccupied() ? "horquilla con bin" : "horquilla vacía");
        docks.setTone(status.ioOccupied() ? "warn" : "ok");

        srm.setValue(status.state().wire().toUpperCase());
        srm.setHint(status.homed() ? "homed" : "sin home");
        srm.setTone(switch (status.state()) {
            case FAULT -> "err";
            case IDLE -> "ok";
            case UNKNOWN -> null;
            default -> "process";
        });
        if (status.estop()) {
            srm.setValue("ESTOP");
            srm.setTone("err");
        }

        position.setText(status.position().compact() + " mm");
        sensors.setText(String.format(
                "home %s   fork %s   I/O %s   e-stop %s   cmd %s",
                status.homed() ? "OK" : "NO",
                status.forkOccupied() ? "BIN" : "vacío",
                status.ioOccupied() ? "BIN" : "vacío",
                status.estop() ? "ABIERTO" : "OK",
                status.cmdId() == null ? "—" : status.cmdId().substring(0, Math.min(8, status.cmdId().length()))
        ));
        bindInventoryBars(null, status);
    }

    private void bindInventory(Inventory inventory, SrmStatus status) {
        int used = inventory.occupiedCount();
        int pct = (int) Math.round(inventory.occupancyRatio() * 100);
        occupancy.setValue(pct + "%");
        occupancy.setHint(used + " / 32 slots");
        occupancy.setTone(pct >= 90 ? "err" : pct >= 70 ? "warn" : "ok");
        bindInventoryBars(inventory, status);
    }

    private void bindInventoryBars(Inventory inventory, SrmStatus status) {
        if (!series.getData().isEmpty() && inventory != null) {
            series.getData().get(0).setYValue(inventory.occupiedOnFace('A'));
            series.getData().get(1).setYValue(inventory.occupiedOnFace('B'));
        }
        if (!series.getData().isEmpty() && series.getData().size() > 2 && status != null) {
            series.getData().get(2).setYValue(status.ioOccupied() ? 1 : 0);
        }
    }
}
