package com.bodega.wms.ui.views;

import com.bodega.wms.gateway.GatewayException;
import com.bodega.wms.model.Slot;
import com.bodega.wms.model.SlotId;
import com.bodega.wms.model.SrmStatus;
import com.bodega.wms.state.AppStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class OperatorView extends VBox {
    private final AppStore store;
    private final Label location = new Label("—");
    private final Label human = new Label("Selecciona un slot en el mapa");
    private final Label skuView = new Label("SKU  —");
    private final Label qty = new Label("1 BIN");
    private final TextField skuField = new TextField();
    private final Label feedback = new Label("");
    private final Label confirmHint = new Label("Retrieve exige doble confirmación");

    private boolean retrieveArmed;

    public OperatorView(AppStore store) {
        this.store = store;
        getStyleClass().add("page");
        setAlignment(Pos.TOP_CENTER);
        setSpacing(16);
        setPadding(new Insets(8, 12, 12, 12));
        setMaxWidth(560);

        Label title = new Label("Módulo operario");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Una columna · botones grandes · confirmación de picking");
        subtitle.getStyleClass().add("page-subtitle");

        location.getStyleClass().add("pick-location");
        human.getStyleClass().add("pick-human");
        skuView.getStyleClass().addAll("pick-sku", "mono-num");
        qty.getStyleClass().add("pick-qty");

        VBox pickCard = new VBox(8, location, human, skuView, qty);
        pickCard.getStyleClass().addAll("card", "pick-card");
        pickCard.setAlignment(Pos.CENTER);

        skuField.setPromptText("SKU del bin (store)");
        skuField.getStyleClass().add("giant-field");
        skuField.setPrefHeight(52);

        Button storeBtn = giant("STORE  ·  I/O → slot", "btn-primary");
        Button retrieveBtn = giant("RETRIEVE  ·  slot → I/O", "btn-process");
        Button abortBtn = giant("ABORT", "btn-danger");
        Button homeBtn = giant("HOME", "btn-ghost");
        Button resetBtn = giant("RESET (fault)", "btn-ghost");
        Button ioBtn = giant("Simular bin en I/O", "btn-ghost");

        storeBtn.setOnAction(e -> doStore());
        retrieveBtn.setOnAction(e -> doRetrieve());
        abortBtn.setOnAction(e -> run("abort", () -> store.abort("abort")));
        homeBtn.setOnAction(e -> run("home", () -> store.abort("home")));
        resetBtn.setOnAction(e -> run("reset", () -> store.abort("reset")));
        ioBtn.setOnAction(e -> {
            store.placeDemoBinAtIo();
            feedback.setText("Bin de demo colocado en I/O");
        });
        ioBtn.managedProperty().bind(ioBtn.visibleProperty());
        store.connectionProperty().addListener((obs, o, n) -> ioBtn.setVisible(store.simulated()));
        ioBtn.setVisible(store.simulated());

        HBox safety = new HBox(10, abortBtn, homeBtn, resetBtn);
        safety.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        abortBtn.setMaxWidth(Double.MAX_VALUE);
        homeBtn.setMaxWidth(Double.MAX_VALUE);
        resetBtn.setMaxWidth(Double.MAX_VALUE);

        feedback.getStyleClass().add("feedback");
        confirmHint.getStyleClass().add("micro");

        getChildren().addAll(
                title, subtitle, pickCard, skuField,
                storeBtn, retrieveBtn, confirmHint, safety, ioBtn, feedback
        );

        store.selectedSlotProperty().addListener((obs, o, n) -> refresh());
        store.inventoryProperty().addListener((obs, o, n) -> refresh());
        store.statusProperty().addListener((obs, o, n) -> refreshStatus(n));
        refresh();
        refreshStatus(store.status());
    }

    private static Button giant(String text, String extra) {
        Button button = new Button(text);
        button.getStyleClass().addAll("giant-btn", extra);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(58);
        return button;
    }

    private void refresh() {
        String raw = store.selectedSlotProperty().get();
        retrieveArmed = false;
        confirmHint.setText("Retrieve exige doble confirmación");
        if (raw == null || !SlotId.isValid(raw)) {
            location.setText("—");
            human.setText("Selecciona un slot en el mapa");
            skuView.setText("SKU  —");
            return;
        }
        SlotId id = SlotId.parse(raw);
        Slot slot = store.inventory().get(raw);
        location.setText(id.aisleLabel());
        human.setText(id.human());
        skuView.setText("SKU  " + slot.displaySku());
        qty.setText(slot.occupied() ? "1 BIN  ·  ocupado" : "1 BIN  ·  destino libre");
    }

    private void refreshStatus(SrmStatus status) {
        if (status.estop()) {
            feedback.setText("E-STOP activo. Reset + home cuando el lazo cierre.");
        }
    }

    private void doStore() {
        String slot = store.selectedSlotProperty().get();
        if (!SlotId.isValid(slot)) {
            fail("Elige un slot destino");
            return;
        }
        run("store", () -> store.store(slot, skuField.getText()));
    }

    private void doRetrieve() {
        String slot = store.selectedSlotProperty().get();
        if (!SlotId.isValid(slot)) {
            fail("Elige el slot a extraer");
            return;
        }
        if (!retrieveArmed) {
            retrieveArmed = true;
            confirmHint.setText("Vuelve a pulsar RETRIEVE para confirmar " + slot);
            return;
        }
        retrieveArmed = false;
        confirmHint.setText("Retrieve exige doble confirmación");
        run("retrieve", () -> store.retrieve(slot));
    }

    private void run(String name, Command command) {
        try {
            String cmdId = command.execute();
            feedback.setText(name + " enviado · " + cmdId.substring(0, 8));
        } catch (GatewayException e) {
            fail(e.getMessage());
        }
    }

    private void fail(String message) {
        feedback.setText(message);
        store.flash(false);
    }

    @FunctionalInterface
    private interface Command {
        String execute() throws GatewayException;
    }
}
