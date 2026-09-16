package com.bodega.wms.state;

import com.bodega.wms.gateway.GatewayException;
import com.bodega.wms.gateway.GatewayFactory;
import com.bodega.wms.gateway.GatewayListener;
import com.bodega.wms.gateway.SimulatedSrmGateway;
import com.bodega.wms.gateway.SrmGateway;
import com.bodega.wms.model.AlertItem;
import com.bodega.wms.model.ConnectionState;
import com.bodega.wms.model.Inventory;
import com.bodega.wms.model.SrmEvent;
import com.bodega.wms.model.SrmStatus;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.Instant;

public final class AppStore implements GatewayListener {
    private final SessionSettings settings = new SessionSettings();
    private final ObjectProperty<SrmStatus> status = new SimpleObjectProperty<>(SrmStatus.disconnected());
    private final ObjectProperty<Inventory> inventory = new SimpleObjectProperty<>(Inventory.empty());
    private final ObjectProperty<ConnectionState> connection = new SimpleObjectProperty<>(ConnectionState.OFFLINE);
    private final StringProperty connectionDetail = new SimpleStringProperty("Sin enlace");
    private final StringProperty selectedSlot = new SimpleStringProperty(null);
    private final StringProperty flashKind = new SimpleStringProperty(null);
    private final BooleanProperty darkTheme = new SimpleBooleanProperty(true);
    private final ObservableList<AlertItem> alerts = FXCollections.observableArrayList();

    private SrmGateway gateway;

    public SessionSettings settings() {
        return settings;
    }

    public ObjectProperty<SrmStatus> statusProperty() {
        return status;
    }

    public SrmStatus status() {
        return status.get();
    }

    public ObjectProperty<Inventory> inventoryProperty() {
        return inventory;
    }

    public Inventory inventory() {
        return inventory.get();
    }

    public ObjectProperty<ConnectionState> connectionProperty() {
        return connection;
    }

    public StringProperty connectionDetailProperty() {
        return connectionDetail;
    }

    public StringProperty selectedSlotProperty() {
        return selectedSlot;
    }

    public StringProperty flashKindProperty() {
        return flashKind;
    }

    public BooleanProperty darkThemeProperty() {
        return darkTheme;
    }

    public ObservableList<AlertItem> alerts() {
        return alerts;
    }

    public boolean online() {
        return connection.get() == ConnectionState.ONLINE || connection.get() == ConnectionState.DEGRADED;
    }

    public void connect() throws GatewayException {
        disconnect();
        gateway = GatewayFactory.create(settings);
        gateway.addListener(this);
        gateway.connect();
    }

    public void disconnect() {
        if (gateway != null) {
            gateway.disconnect();
            gateway.removeListener(this);
            gateway = null;
        }
        ui(() -> {
            status.set(SrmStatus.disconnected());
            connection.set(ConnectionState.OFFLINE);
        });
    }

    public String store(String slotId, String sku) throws GatewayException {
        requireGateway();
        return gateway.store(slotId, sku);
    }

    public String retrieve(String slotId) throws GatewayException {
        requireGateway();
        return gateway.retrieve(slotId);
    }

    public String abort(String action) throws GatewayException {
        requireGateway();
        return gateway.abort(action);
    }

    public void placeDemoBinAtIo() {
        if (gateway instanceof SimulatedSrmGateway sim) {
            sim.placeBinAtIo();
        }
    }

    public boolean simulated() {
        return gateway instanceof SimulatedSrmGateway;
    }

    public void flash(boolean ok) {
        ui(() -> flashKind.set(ok ? "ok" : "err"));
        new Thread(() -> {
            try {
                Thread.sleep(280);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            ui(() -> flashKind.set(null));
        }, "flash").start();
    }

    @Override
    public void onConnection(ConnectionState state, String detail) {
        ui(() -> {
            connection.set(state);
            connectionDetail.set(detail == null ? "" : detail);
            if (state == ConnectionState.OFFLINE) {
                push(AlertItem.Severity.WARNING, "Enlace", detail);
            } else if (state == ConnectionState.ONLINE) {
                push(AlertItem.Severity.SUCCESS, "Enlace", detail);
            }
        });
    }

    @Override
    public void onStatus(SrmStatus next) {
        ui(() -> {
            SrmStatus prev = status.get();
            status.set(next);
            if (next.estop() && (prev == null || !prev.estop())) {
                push(AlertItem.Severity.ERROR, "E-stop", "Lazo de emergencia abierto");
            }
            if (next.state() != prev.state()) {
                AlertItem.Severity severity = switch (next.state()) {
                    case FAULT -> AlertItem.Severity.ERROR;
                    case MOVING, PICKING, PLACING, HOMING -> AlertItem.Severity.PROCESS;
                    case IDLE -> AlertItem.Severity.SUCCESS;
                    default -> AlertItem.Severity.INFO;
                };
                push(severity, "SRM " + next.state().wire(), next.error() == null ? next.mission() : next.error());
            }
        });
    }

    @Override
    public void onInventory(Inventory next) {
        ui(() -> inventory.set(next));
    }

    @Override
    public void onEvent(SrmEvent event) {
        ui(() -> {
            AlertItem.Severity severity = event.failure()
                    ? AlertItem.Severity.ERROR
                    : event.success() ? AlertItem.Severity.SUCCESS : AlertItem.Severity.INFO;
            String detail = event.error() != null ? event.error() : event.slotId();
            push(severity, event.title(), detail);
            if (event.failure()) {
                flash(false);
            } else if ("completed".equals(event.type())) {
                flash(true);
            }
        });
    }

    private void push(AlertItem.Severity severity, String title, String detail) {
        alerts.add(0, new AlertItem(Instant.now(), severity, title, detail == null ? "" : detail));
        if (alerts.size() > 80) {
            alerts.remove(80, alerts.size());
        }
    }

    private void requireGateway() throws GatewayException {
        if (gateway == null || !gateway.isConnected()) {
            throw new GatewayException("Sin conexión con el SRM");
        }
    }

    private static void ui(Runnable task) {
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }
}
