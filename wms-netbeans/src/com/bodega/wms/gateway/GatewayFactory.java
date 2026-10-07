package com.bodega.wms.gateway;

import com.bodega.wms.state.SessionSettings;

public final class GatewayFactory {
    private GatewayFactory() {
    }

    public static SrmGateway create(SessionSettings settings) {
        return switch (settings.getKind()) {
            case MQTT -> new MqttSrmGateway(settings);
            case SIMULATED -> new SimulatedSrmGateway();
            case NATIVE -> new NativeSrmGateway();
        };
    }
}
