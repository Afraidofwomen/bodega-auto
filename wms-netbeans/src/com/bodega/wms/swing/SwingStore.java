package com.bodega.wms.swing;

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
import com.bodega.wms.state.SessionSettings;

import javax.swing.SwingUtilities;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class SwingStore implements GatewayListener {
    private final SessionSettings settings = new SessionSettings();
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final List<AlertItem> alerts = new ArrayList<>();

    private SrmStatus status = SrmStatus.disconnected();
    private Inventory inventory = Inventory.empty();
    private ConnectionState connection = ConnectionState.OFFLINE;
    private String connectionDetail = "Sin enlace";
    private String selectedSlot;
    private SrmGateway gateway;

    SessionSettings settings() {
        return settings;
    }

    SrmStatus status() {
        return status;
    }

    Inventory inventory() {
        return inventory;
    }

    ConnectionState connection() {
        return connection;
    }

    String connectionDetail() {
        return connectionDetail;
    }

    String selectedSlot() {
        return selectedSlot;
    }

    void setSelectedSlot(String slotId) {
        String old = selectedSlot;
        selectedSlot = slotId;
        fire("selectedSlot", old, slotId);
    }

    List<AlertItem> alerts() {
        return List.copyOf(alerts);
    }

    void addListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    void connect() throws GatewayException {
        disconnect();
        gateway = GatewayFactory.create(settings);
        gateway.addListener(this);
        gateway.connect();
    }

    void disconnect() {
        if (gateway != null) {
            gateway.disconnect();
            gateway.removeListener(this);
            gateway = null;
        }
        ui(() -> {
            status = SrmStatus.disconnected();
            connection = ConnectionState.OFFLINE;
            fire("status", null, status);
            fire("connection", null, connection);
        });
    }

    String store(String slotId, String sku) throws GatewayException {
        requireGateway();
        return gateway.store(slotId, sku);
    }

    String retrieve(String slotId) throws GatewayException {
        requireGateway();
        return gateway.retrieve(slotId);
    }

    String abort(String action) throws GatewayException {
        requireGateway();
        return gateway.abort(action);
    }

    void placeDemoBinAtIo() {
        if (gateway instanceof SimulatedSrmGateway sim) {
            sim.placeBinAtIo();
        }
    }

    boolean simulated() {
        return gateway instanceof SimulatedSrmGateway;
    }

    @Override
    public void onConnection(ConnectionState state, String detail) {
        ui(() -> {
            connection = state;
            connectionDetail = detail == null ? "" : detail;
            push(state == ConnectionState.ONLINE ? AlertItem.Severity.SUCCESS : AlertItem.Severity.WARNING,
                    "Enlace", detail);
            fire("connection", null, state);
        });
    }

    @Override
    public void onStatus(SrmStatus next) {
        ui(() -> {
            status = next;
            fire("status", null, next);
        });
    }

    @Override
    public void onInventory(Inventory next) {
        ui(() -> {
            inventory = next;
            fire("inventory", null, next);
        });
    }

    @Override
    public void onEvent(SrmEvent event) {
        ui(() -> {
            AlertItem.Severity severity = event.failure()
                    ? AlertItem.Severity.ERROR
                    : event.success() ? AlertItem.Severity.SUCCESS : AlertItem.Severity.INFO;
            push(severity, event.title(), event.error() != null ? event.error() : event.slotId());
            fire("event", null, event);
        });
    }

    private void push(AlertItem.Severity severity, String title, String detail) {
        alerts.add(0, new AlertItem(Instant.now(), severity, title, detail == null ? "" : detail));
        if (alerts.size() > 80) {
            alerts.subList(80, alerts.size()).clear();
        }
        fire("alerts", null, alerts());
    }

    private void requireGateway() throws GatewayException {
        if (gateway == null || !gateway.isConnected()) {
            throw new GatewayException("Sin conexión con el SRM");
        }
    }

    private void fire(String name, Object old, Object value) {
        pcs.firePropertyChange(name, old, value);
    }

    private static void ui(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }
}
