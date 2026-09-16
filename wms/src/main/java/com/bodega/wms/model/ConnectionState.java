package com.bodega.wms.model;

public enum ConnectionState {
    OFFLINE,
    CONNECTING,
    ONLINE,
    DEGRADED;

    public String label() {
        return switch (this) {
            case OFFLINE -> "Offline";
            case CONNECTING -> "Conectando";
            case ONLINE -> "Online";
            case DEGRADED -> "Degradado";
        };
    }
}
