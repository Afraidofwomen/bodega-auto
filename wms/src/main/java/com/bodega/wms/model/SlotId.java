package com.bodega.wms.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public record SlotId(char face, int bay, int level) {
    private static final Pattern PATTERN = Pattern.compile("^([ABab])([1-4])([1-4])$");

    public SlotId {
        face = Character.toUpperCase(face);
        if (face != 'A' && face != 'B') {
            throw new IllegalArgumentException("Cara inválida: " + face);
        }
        if (bay < 1 || bay > 4 || level < 1 || level > 4) {
            throw new IllegalArgumentException("Bahía/nivel fuera de 1–4");
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
        return new SlotId(
                matcher.group(1).charAt(0),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))
        );
    }

    public static boolean isValid(String raw) {
        return raw != null && PATTERN.matcher(raw.trim()).matches();
    }

    public static List<String> all() {
        List<String> ids = new ArrayList<>(32);
        for (char face : new char[]{'A', 'B'}) {
            for (int bay = 1; bay <= 4; bay++) {
                for (int level = 1; level <= 4; level++) {
                    ids.add("" + face + bay + level);
                }
            }
        }
        return ids;
    }

    public String wire() {
        return "" + face + bay + level;
    }

    public String aisleLabel() {
        return face + String.format("%02d", bay) + "-N" + level;
    }

    public String human() {
        return "Cara " + face + " · Bahía " + bay + " · Nivel " + level;
    }
}
