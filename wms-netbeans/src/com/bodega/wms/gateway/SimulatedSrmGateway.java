package com.bodega.wms.gateway;

import com.bodega.wms.model.ConnectionState;
import com.bodega.wms.model.GatewayKind;
import com.bodega.wms.model.Inventory;
import com.bodega.wms.model.Position;
import com.bodega.wms.model.Slot;
import com.bodega.wms.model.SlotId;
import com.bodega.wms.model.SrmEvent;
import com.bodega.wms.model.SrmState;
import com.bodega.wms.model.SrmStatus;
import com.bodega.wms.protocol.JsonCodec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class SimulatedSrmGateway implements SrmGateway {
    private final List<GatewayListener> listeners = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService clock = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "srm-sim");
        thread.setDaemon(true);
        return thread;
    });

    private volatile boolean connected;
    private volatile SrmStatus status = SrmStatus.disconnected();
    private volatile Inventory inventory = demoInventory();
    private volatile boolean ioOccupied;
    private volatile boolean abortRequested;

    @Override
    public GatewayKind kind() {
        return GatewayKind.SIMULATED;
    }

    @Override
    public void connect() {
        connected = true;
        ioOccupied = false;
        abortRequested = false;
        status = new SrmStatus("srm1", SrmState.IDLE, true, null, null, Position.origin(), false, false, false, null);
        notifyConnection(ConnectionState.ONLINE, "Simulador local");
        listeners.forEach(l -> l.onStatus(status));
        listeners.forEach(l -> l.onInventory(inventory));
    }

    @Override
    public void disconnect() {
        connected = false;
        status = SrmStatus.disconnected();
        notifyConnection(ConnectionState.OFFLINE, "Simulador detenido");
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public String store(String slotId, String sku) throws GatewayException {
        requireConnected();
        String cmdId = JsonCodec.newCmdId();
        if (!SlotId.isValid(slotId)) {
            reject(cmdId, "invalid_slot");
            return cmdId;
        }
        if (status.state() != SrmState.IDLE) {
            reject(cmdId, "not_idle");
            return cmdId;
        }
        if (!status.homed()) {
            reject(cmdId, "not_homed");
            return cmdId;
        }
        if (!ioOccupied) {
            reject(cmdId, "io_empty");
            return cmdId;
        }
        if (inventory.get(slotId).occupied()) {
            reject(cmdId, "slot_occupied");
            return cmdId;
        }
        runMission(cmdId, "store", slotId, sku);
        return cmdId;
    }

    @Override
    public String retrieve(String slotId) throws GatewayException {
        requireConnected();
        String cmdId = JsonCodec.newCmdId();
        if (!SlotId.isValid(slotId)) {
            reject(cmdId, "invalid_slot");
            return cmdId;
        }
        if (status.state() != SrmState.IDLE) {
            reject(cmdId, "not_idle");
            return cmdId;
        }
        if (ioOccupied) {
            reject(cmdId, "io_occupied");
            return cmdId;
        }
        if (!inventory.get(slotId).occupied()) {
            reject(cmdId, "slot_empty");
            return cmdId;
        }
        runMission(cmdId, "retrieve", slotId, inventory.get(slotId).sku());
        return cmdId;
    }

    @Override
    public String abort(String action) throws GatewayException {
        requireConnected();
        String cmdId = JsonCodec.newCmdId();
        switch (action) {
            case "abort" -> {
                if (!status.state().busy()) {
                    reject(cmdId, "not_busy");
                } else {
                    abortRequested = true;
                    emit(new SrmEvent(cmdId, "accepted", "abort", null, null, null));
                }
            }
            case "reset" -> {
                if (status.state() != SrmState.FAULT) {
                    reject(cmdId, "not_idle");
                } else {
                    runHome(cmdId);
                }
            }
            case "home" -> {
                if (status.state() != SrmState.IDLE) {
                    reject(cmdId, "not_idle");
                } else {
                    runHome(cmdId);
                }
            }
            default -> reject(cmdId, "unknown_action");
        }
        return cmdId;
    }

    public void placeBinAtIo() {
        ioOccupied = true;
        publishStatus(status.state(), status.cmdId(), status.mission(), status.position(), status.forkOccupied(), true, null);
    }

    @Override
    public void addListener(GatewayListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(GatewayListener listener) {
        listeners.remove(listener);
    }

    private void runMission(String cmdId, String mission, String slotId, String sku) {
        abortRequested = false;
        emit(new SrmEvent(cmdId, "accepted", mission, slotId, null, null));
        SlotId parsed = SlotId.parse(slotId);
        double y = parsed.yMm();

        schedule(0, () -> publishStatus(SrmState.MOVING, cmdId, mission, new Position(0, y / 2, 0), false, ioOccupied, null));
        schedule(700, () -> publishStatus(SrmState.PICKING, cmdId, mission, new Position(
                0,
                "store".equals(mission) ? 0 : y,
                90
        ), true, "store".equals(mission), null));
        schedule(1400, () -> {
            if (abortRequested) {
                fail(cmdId, "timeout");
                return;
            }
            publishStatus(SrmState.MOVING, cmdId, mission, new Position(0, y, 0), true, false, null);
        });
        schedule(2100, () -> publishStatus(SrmState.PLACING, cmdId, mission, new Position(
                0,
                "store".equals(mission) ? y : 0,
                90
        ), "retrieve".equals(mission), "retrieve".equals(mission), null));
        schedule(2800, () -> {
            if ("store".equals(mission)) {
                inventory = inventory.with(new Slot(slotId, true, sku));
                ioOccupied = false;
            } else {
                inventory = inventory.with(Slot.empty(slotId));
                ioOccupied = true;
            }
            listeners.forEach(l -> l.onInventory(inventory));
            publishStatus(SrmState.IDLE, null, null, Position.origin(), false, ioOccupied, null);
            emit(new SrmEvent(cmdId, "completed", mission, slotId, null, "idle"));
        });
    }

    private void runHome(String cmdId) {
        schedule(0, () -> publishStatus(SrmState.HOMING, cmdId, "home", Position.origin(), false, ioOccupied, null));
        schedule(1200, () -> {
            publishStatus(SrmState.IDLE, null, null, Position.origin(), false, ioOccupied, null);
            emit(new SrmEvent(cmdId, "completed", "home", null, null, "idle"));
        });
    }

    private void fail(String cmdId, String error) {
        publishStatus(SrmState.FAULT, cmdId, status.mission(), status.position(), status.forkOccupied(), ioOccupied, error);
        emit(new SrmEvent(cmdId, "failed", status.mission(), null, error, "fault"));
    }

    private void reject(String cmdId, String error) {
        emit(new SrmEvent(cmdId, "rejected", null, null, error, status.state().wire()));
    }

    private void publishStatus(
            SrmState state,
            String cmdId,
            String mission,
            Position position,
            boolean fork,
            boolean io,
            String error
    ) {
        status = new SrmStatus("srm1", state, true, cmdId, mission, position, fork, io, false, error);
        listeners.forEach(l -> l.onStatus(status));
    }

    private void emit(SrmEvent event) {
        listeners.forEach(l -> l.onEvent(event));
    }

    private void schedule(long delayMs, Runnable task) {
        clock.schedule(task, delayMs, TimeUnit.MILLISECONDS);
    }

    private void requireConnected() throws GatewayException {
        if (!connected) {
            throw new GatewayException("Simulador offline");
        }
    }

    private void notifyConnection(ConnectionState state, String detail) {
        listeners.forEach(l -> l.onConnection(state, detail));
    }

    private static Inventory demoInventory() {
        Map<String, Slot> map = new LinkedHashMap<>();
        map.put("S1", new Slot("S1", true, "TORN-M3"));
        map.put("S3", new Slot("S3", true, "ARAN-8"));
        map.put("S5", new Slot("S5", true, "NEMA-17"));
        return new Inventory(map);
    }
}
