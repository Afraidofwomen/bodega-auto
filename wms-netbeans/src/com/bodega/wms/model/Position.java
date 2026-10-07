package com.bodega.wms.model;

public record Position(double xMm, double yMm, double zMm) {
    public static Position origin() {
        return new Position(0, 0, 0);
    }

    public String compact() {
        return String.format("X %.0f  Y %.0f  Z %.0f", xMm, yMm, zMm);
    }
}
