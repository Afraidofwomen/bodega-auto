package com.bodega.wms.model;

public enum GatewayKind {
    SIMULATED("Simulador (sin ESP32)"),
    MQTT("MQTT → ESP32"),
    NATIVE("Nativo C++ (JNI)");

    private final String label;

    GatewayKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
