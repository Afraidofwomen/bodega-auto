package com.bodega.wms.model;

public record SrmEvent(
        String cmdId,
        String type,
        String mission,
        String slotId,
        String error,
        String state
) {
    public boolean success() {
        return "completed".equals(type) || "accepted".equals(type);
    }

    public boolean failure() {
        return "rejected".equals(type) || "failed".equals(type);
    }

    public String title() {
        return switch (type == null ? "" : type) {
            case "completed" -> "Misión completada";
            case "accepted" -> "Misión aceptada";
            case "rejected" -> "Comando rechazado";
            case "failed" -> "Fallo de misión";
            default -> type == null || type.isBlank() ? "Evento" : type;
        };
    }
}
