package com.bodega.wms.gateway;

import com.bodega.wms.model.GatewayKind;

/**
 * Contrato único hacia el SRM. Hoy: MQTT (ESP32) o simulador.
 * Mañana: implementación JNI que llama al C++ de movimiento.
 */
public interface SrmGateway extends AutoCloseable {
    GatewayKind kind();

    void connect() throws GatewayException;

    void disconnect();

    boolean isConnected();

    String store(String slotId, String sku) throws GatewayException;

    String retrieve(String slotId) throws GatewayException;

    String abort(String action) throws GatewayException;

    void addListener(GatewayListener listener);

    void removeListener(GatewayListener listener);

    @Override
    default void close() {
        disconnect();
    }
}
