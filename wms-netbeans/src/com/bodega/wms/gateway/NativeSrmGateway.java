package com.bodega.wms.gateway;

import com.bodega.wms.model.ConnectionState;
import com.bodega.wms.model.GatewayKind;
import com.bodega.wms.protocol.JsonCodec;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class NativeSrmGateway implements SrmGateway {
    private final List<GatewayListener> listeners = new CopyOnWriteArrayList<>();
    private SrmNative nativeLib;
    private volatile boolean connected;

    @Override
    public GatewayKind kind() {
        return GatewayKind.NATIVE;
    }

    @Override
    public synchronized void connect() throws GatewayException {
        if (!SrmNative.available()) {
            throw new GatewayException(
                    "No está libsrm_bridge. Compila native/ (C++) e instala la librería en java.library.path."
            );
        }
        nativeLib = new SrmNative(this);
        int rc = nativeLib.start(JsonCodec.write(Map.of("device_id", "srm1")));
        if (rc != 0) {
            throw new GatewayException("srm_bridge_start falló con código " + rc);
        }
        connected = true;
        notifyConnection(ConnectionState.ONLINE, "Puente C++ JNI");
    }

    @Override
    public synchronized void disconnect() {
        if (nativeLib != null) {
            try {
                nativeLib.stop();
            } catch (UnsatisfiedLinkError ignored) {
            }
            nativeLib = null;
        }
        connected = false;
        notifyConnection(ConnectionState.OFFLINE, "Puente C++ detenido");
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public String store(String slotId, String sku) throws GatewayException {
        requireReady();
        String cmdId = JsonCodec.newCmdId();
        require(nativeLib.store(cmdId, slotId, sku == null ? "" : sku));
        return cmdId;
    }

    @Override
    public String retrieve(String slotId) throws GatewayException {
        requireReady();
        String cmdId = JsonCodec.newCmdId();
        require(nativeLib.retrieve(cmdId, slotId));
        return cmdId;
    }

    @Override
    public String abort(String action) throws GatewayException {
        requireReady();
        String cmdId = JsonCodec.newCmdId();
        require(nativeLib.abort(cmdId, action));
        return cmdId;
    }

    @Override
    public void addListener(GatewayListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(GatewayListener listener) {
        listeners.remove(listener);
    }

    void receiveStatus(String json) {
        var status = JsonCodec.status(json);
        listeners.forEach(l -> l.onStatus(status));
    }

    void receiveEvent(String json) {
        var event = JsonCodec.event(json);
        listeners.forEach(l -> l.onEvent(event));
    }

    void receiveInventory(String json) {
        var inventory = JsonCodec.inventory(json);
        listeners.forEach(l -> l.onInventory(inventory));
    }

    private void requireReady() throws GatewayException {
        if (!connected || nativeLib == null) {
            throw new GatewayException("Puente C++ offline");
        }
    }

    private void require(int rc) throws GatewayException {
        if (rc != 0) {
            throw new GatewayException("Llamada nativa falló con código " + rc);
        }
    }

    private void notifyConnection(ConnectionState state, String detail) {
        listeners.forEach(l -> l.onConnection(state, detail));
    }
}
