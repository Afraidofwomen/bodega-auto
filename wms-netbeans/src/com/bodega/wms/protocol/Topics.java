package com.bodega.wms.protocol;

public final class Topics {
    private Topics() {
    }

    public static String prefix(String deviceId) {
        return "asrs/" + deviceId + "/";
    }

    public static String store(String deviceId) {
        return prefix(deviceId) + "cmd/store";
    }

    public static String retrieve(String deviceId) {
        return prefix(deviceId) + "cmd/retrieve";
    }

    public static String abort(String deviceId) {
        return prefix(deviceId) + "cmd/abort";
    }

    public static String status(String deviceId) {
        return prefix(deviceId) + "status";
    }

    public static String event(String deviceId) {
        return prefix(deviceId) + "event";
    }

    public static String inventory(String deviceId) {
        return prefix(deviceId) + "inventory";
    }
}
