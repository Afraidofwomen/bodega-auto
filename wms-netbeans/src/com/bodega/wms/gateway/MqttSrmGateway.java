package com.bodega.wms.gateway;

import com.bodega.wms.model.ConnectionState;
import com.bodega.wms.model.GatewayKind;
import com.bodega.wms.protocol.JsonCodec;
import com.bodega.wms.protocol.Topics;
import com.bodega.wms.state.SessionSettings;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class MqttSrmGateway implements SrmGateway {
    private final SessionSettings settings;
    private final List<GatewayListener> listeners = new CopyOnWriteArrayList<>();
    private volatile MqttClient client;

    public MqttSrmGateway(SessionSettings settings) {
        this.settings = settings;
    }

    @Override
    public GatewayKind kind() {
        return GatewayKind.MQTT;
    }

    @Override
    public synchronized void connect() throws GatewayException {
        disconnect();
        notifyConnection(ConnectionState.CONNECTING, "Abriendo " + settings.brokerUri());
        try {
            String id = settings.getClientId() + "-" + System.currentTimeMillis();
            MqttClient mqtt = new MqttClient(settings.brokerUri(), id, new MemoryPersistence());
            mqtt.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    try {
                        subscribe(mqtt);
                        notifyConnection(ConnectionState.ONLINE, serverURI);
                    } catch (MqttException e) {
                        notifyConnection(ConnectionState.DEGRADED, e.getMessage());
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    notifyConnection(ConnectionState.OFFLINE, cause == null ? "Conexión perdida" : cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                    dispatch(topic, payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(8);
            options.setKeepAliveInterval(20);
            if (settings.getUsername() != null && !settings.getUsername().isBlank()) {
                options.setUserName(settings.getUsername());
                options.setPassword(settings.getPassword().toCharArray());
            }
            mqtt.connect(options);
            this.client = mqtt;
        } catch (MqttException e) {
            notifyConnection(ConnectionState.OFFLINE, e.getMessage());
            throw new GatewayException("No se pudo conectar al broker MQTT", e);
        }
    }

    private void subscribe(MqttClient mqtt) throws MqttException {
        String device = settings.getDeviceId();
        mqtt.subscribe(Topics.status(device), 1);
        mqtt.subscribe(Topics.event(device), 1);
        mqtt.subscribe(Topics.inventory(device), 1);
    }

    private void dispatch(String topic, String payload) {
        String device = settings.getDeviceId();
        try {
            if (Topics.status(device).equals(topic)) {
                var status = JsonCodec.status(payload);
                listeners.forEach(l -> l.onStatus(status));
            } else if (Topics.event(device).equals(topic)) {
                var event = JsonCodec.event(payload);
                listeners.forEach(l -> l.onEvent(event));
            } else if (Topics.inventory(device).equals(topic)) {
                var inventory = JsonCodec.inventory(payload);
                listeners.forEach(l -> l.onInventory(inventory));
            }
        } catch (RuntimeException e) {
            notifyConnection(ConnectionState.DEGRADED, "JSON inválido: " + e.getMessage());
        }
    }

    @Override
    public synchronized void disconnect() {
        MqttClient mqtt = this.client;
        this.client = null;
        if (mqtt != null) {
            try {
                if (mqtt.isConnected()) {
                    mqtt.disconnect();
                }
                mqtt.close();
            } catch (MqttException ignored) {
            }
        }
        notifyConnection(ConnectionState.OFFLINE, "Desconectado");
    }

    @Override
    public boolean isConnected() {
        MqttClient mqtt = this.client;
        return mqtt != null && mqtt.isConnected();
    }

    @Override
    public String store(String slotId, String sku) throws GatewayException {
        String cmdId = JsonCodec.newCmdId();
        publish(Topics.store(settings.getDeviceId()), JsonCodec.store(cmdId, slotId, sku));
        return cmdId;
    }

    @Override
    public String retrieve(String slotId) throws GatewayException {
        String cmdId = JsonCodec.newCmdId();
        publish(Topics.retrieve(settings.getDeviceId()), JsonCodec.retrieve(cmdId, slotId));
        return cmdId;
    }

    @Override
    public String abort(String action) throws GatewayException {
        String cmdId = JsonCodec.newCmdId();
        publish(Topics.abort(settings.getDeviceId()), JsonCodec.abort(cmdId, action));
        return cmdId;
    }

    private void publish(String topic, String json) throws GatewayException {
        MqttClient mqtt = this.client;
        if (mqtt == null || !mqtt.isConnected()) {
            throw new GatewayException("ESP32 / broker offline");
        }
        try {
            MqttMessage message = new MqttMessage(json.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);
            message.setRetained(false);
            mqtt.publish(topic, message);
        } catch (MqttException e) {
            throw new GatewayException("No se pudo publicar " + topic, e);
        }
    }

    @Override
    public void addListener(GatewayListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(GatewayListener listener) {
        listeners.remove(listener);
    }

    private void notifyConnection(ConnectionState state, String detail) {
        listeners.forEach(l -> l.onConnection(state, detail));
    }
}
