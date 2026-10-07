package com.bodega.wms.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public record SlotId(int level) {
    private static final Pattern PATTERN = Pattern.compile("^[Ss]([1-5])$");

    public SlotId {
        if (level < 1 || level > 5) {
            throw new IllegalArgumentException("Nivel fuera de 1–5");
        }
    }

    public static SlotId parse(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("slot_id vacío");
        }
        var matcher = PATTERN.matcher(raw.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("slot_id inválido: " + raw);
        }
        return new SlotId(Integer.parseInt(matcher.group(1)));
    }

    public static boolean isValid(String raw) {
        return raw != null && PATTERN.matcher(raw.trim()).matches();
    }

    public static List<String> all() {
        List<String> ids = new ArrayList<>(5);
        for (int level = 1; level <= 5; level++) {
            ids.add("S" + level);
        }
        return ids;
    }

    public static int capacity() {
        return 5;
    }

    public String wire() {
        return "S" + level;
    }

    public String aisleLabel() {
        return "N" + level;
    }

    public String human() {
        return "Nivel " + level + " · columna única";
    }

    public double yMm() {
        return level * 44.0;
    }
}
