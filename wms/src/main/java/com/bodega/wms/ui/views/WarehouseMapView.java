package com.bodega.wms.ui.views;

import com.bodega.wms.model.Inventory;
import com.bodega.wms.model.Slot;
import com.bodega.wms.model.SrmStatus;
import com.bodega.wms.state.AppStore;
import com.bodega.wms.ui.components.SlotCell;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

public final class WarehouseMapView extends VBox {
    private final AppStore store;
    private final Map<String, SlotCell> cells = new LinkedHashMap<>();
    private final Label ioLabel = new Label("I/O");
    private final Label ioSku = new Label("vacío");
    private final Label crane = new Label("SRM");
    private final Label selectedInfo = new Label("Selecciona un slot");
    private Predicate<Slot> filter = slot -> true;

    public WarehouseMapView(AppStore store) {
        this.store = store;
        getStyleClass().add("page");
        Label title = new Label("Layout del almacén");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Mapa de calor 2D · Rojo = ocupado · Verde = vacío");
        subtitle.getStyleClass().add("page-subtitle");

        ToggleGroup filters = new ToggleGroup();
        HBox filterBar = new HBox(8,
                chip(filters, "Todas", slot -> true, true),
                chip(filters, "Rack A", slot -> slot.slotId().startsWith("A"), false),
                chip(filters, "Rack B", slot -> slot.slotId().startsWith("B"), false),
                chip(filters, "Ocupados", Slot::occupied, false),
                chip(filters, "Libres", slot -> !slot.occupied(), false)
        );
        filterBar.setAlignment(Pos.CENTER_LEFT);

        VBox io = new VBox(4, ioLabel, ioSku);
        io.getStyleClass().addAll("io-station");
        io.setAlignment(Pos.CENTER);
        io.setMinSize(88, 200);
        ioLabel.getStyleClass().addAll("slot-id", "mono-num");
        ioSku.getStyleClass().add("slot-sku");

        GridPane rackA = rack('A');
        GridPane rackB = rack('B');
        Label rackATitle = new Label("RACK A  ·  Z+");
        rackATitle.getStyleClass().add("rack-title");
        Label rackBTitle = new Label("RACK B  ·  Z−");
        rackBTitle.getStyleClass().add("rack-title");

        crane.getStyleClass().add("crane-marker");
        HBox aisle = new HBox(crane);
        aisle.setAlignment(Pos.CENTER_LEFT);
        aisle.getStyleClass().add("aisle");
        HBox.setHgrow(aisle, Priority.ALWAYS);

        VBox racks = new VBox(10, rackATitle, rackA, aisle, rackBTitle, rackB);
        HBox.setHgrow(racks, Priority.ALWAYS);

        HBox map = new HBox(16, io, racks);
        map.getStyleClass().add("card");
        map.setPadding(new Insets(16));
        HBox.setHgrow(racks, Priority.ALWAYS);

        selectedInfo.getStyleClass().add("selected-slot-info");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label legend = new Label("Clic = destino store/retrieve   ·   I/O a X=0, nivel 1");
        legend.getStyleClass().add("micro");
        HBox footer = new HBox(12, selectedInfo, spacer, legend);
        footer.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(title, subtitle, filterBar, map, footer);
        VBox.setVgrow(map, Priority.ALWAYS);

        store.inventoryProperty().addListener((obs, o, n) -> refresh());
        store.statusProperty().addListener((obs, o, n) -> refreshIo(n));
        store.selectedSlotProperty().addListener((obs, o, n) -> refresh());
        refresh();
        refreshIo(store.status());
    }

    private ToggleButton chip(ToggleGroup group, String text, Predicate<Slot> next, boolean selected) {
        ToggleButton button = new ToggleButton(text);
        button.getStyleClass().add("chip");
        button.setToggleGroup(group);
        button.setSelected(selected);
        button.setOnAction(e -> {
            filter = next;
            refresh();
        });
        return button;
    }

    private GridPane rack(char face) {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setPercentWidth(25);
            grid.getColumnConstraints().add(cc);
        }
        for (int level = 4; level >= 1; level--) {
            for (int bay = 1; bay <= 4; bay++) {
                String id = "" + face + bay + level;
                SlotCell cell = new SlotCell();
                cell.setOnMouseClicked(e -> store.selectedSlotProperty().set(id));
                cell.setMaxWidth(Double.MAX_VALUE);
                cells.put(id, cell);
                grid.add(cell, bay - 1, 4 - level);
                GridPane.setHgrow(cell, Priority.ALWAYS);
            }
        }
        return grid;
    }

    private void refresh() {
        Inventory inventory = store.inventory();
        String selected = store.selectedSlotProperty().get();
        cells.forEach((id, cell) -> {
            Slot slot = inventory.get(id);
            boolean visible = filter.test(slot);
            cell.setVisible(visible);
            cell.setManaged(true);
            cell.setOpacity(visible ? 1 : 0.28);
            cell.bind(slot, id.equals(selected));
        });
        if (selected == null) {
            selectedInfo.setText("Selecciona un slot");
        } else {
            Slot slot = inventory.get(selected);
            selectedInfo.setText(selected + "  ·  " + (slot.occupied() ? slot.displaySku() : "libre"));
        }
        refreshIo(store.status());
    }

    private void refreshIo(SrmStatus status) {
        ioSku.setText(status.ioOccupied() ? "BIN" : "vacío");
        ioLabel.getParent().getStyleClass().removeAll("slot-full", "slot-empty");
        ioLabel.getParent().getStyleClass().add(status.ioOccupied() ? "slot-full" : "slot-empty");
        double ratio = Math.max(0, Math.min(1, status.position().xMm() / 480.0));
        StackPane.setAlignment(crane, Pos.CENTER_LEFT);
        crane.setTranslateX(24 + ratio * 360);
        crane.setText(String.format("SRM  %s  %s", status.state().wire(), status.position().compact()));
    }
}
