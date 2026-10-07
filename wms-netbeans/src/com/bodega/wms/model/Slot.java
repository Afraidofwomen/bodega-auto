package com.bodega.wms.model;

public record Slot(String slotId, boolean occupied, String sku) {
    public static Slot empty(String slotId) {
        return new Slot(slotId, false, null);
    }

    public String displaySku() {
        if (!occupied) {
            return "—";
        }
        if (sku == null || sku.isBlank()) {
            return "SIN-SKU";
        }
        return sku;
    }
}
