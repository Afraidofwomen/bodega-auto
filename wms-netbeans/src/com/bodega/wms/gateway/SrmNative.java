package com.bodega.wms.gateway;

/**
 * JNI hacia {@code libsrm_bridge}. El C++ implementa {@code native/include/srm_bridge.h}
 * y llama de vuelta a {@link #onStatus}, {@link #onEvent} e {@link #onInventory}.
 */
public final class SrmNative {
    private static final boolean AVAILABLE;

    static {
        boolean loaded = false;
        try {
            System.loadLibrary("srm_bridge");
            loaded = true;
        } catch (UnsatisfiedLinkError ignored) {
            loaded = false;
        }
        AVAILABLE = loaded;
    }

    private final NativeSrmGateway owner;

    SrmNative(NativeSrmGateway owner) {
        this.owner = owner;
    }

    public static boolean available() {
        return AVAILABLE;
    }

    public native int start(String configJson);

    public native int store(String cmdId, String slotId, String sku);

    public native int retrieve(String cmdId, String slotId);

    public native int abort(String cmdId, String action);

    public native void stop();

    @SuppressWarnings("unused")
    private void onStatus(String json) {
        owner.receiveStatus(json);
    }

    @SuppressWarnings("unused")
    private void onEvent(String json) {
        owner.receiveEvent(json);
    }

    @SuppressWarnings("unused")
    private void onInventory(String json) {
        owner.receiveInventory(json);
    }
}
