package com.bodega.wms.model;

public enum SrmState {
    HOMING,
    IDLE,
    MOVING,
    PICKING,
    PLACING,
    FAULT,
    UNKNOWN;

    public static SrmState fromWire(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNKNOWN;
        }
        try {
            return SrmState.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return UNKNOWN;
        }
    }

    public String wire() {
        return name().toLowerCase();
    }

    public boolean busy() {
        return this == HOMING || this == MOVING || this == PICKING || this == PLACING;
    }
}
