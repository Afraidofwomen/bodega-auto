package com.bodega.wms.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Inventory {
    private final Map<String, Slot> slots;

    public Inventory(Map<String, Slot> slots) {
        Map<String, Slot> copy = new LinkedHashMap<>();
        for (String id : SlotId.all()) {
            Slot slot = slots.get(id);
            copy.put(id, slot != null ? slot : Slot.empty(id));
        }
        this.slots = Collections.unmodifiableMap(copy);
    }

    public static Inventory empty() {
        return new Inventory(Map.of());
    }

    public Slot get(String slotId) {
        return slots.getOrDefault(slotId, Slot.empty(slotId));
    }

    public List<Slot> all() {
        return List.copyOf(slots.values());
    }

    public int occupiedCount() {
        int n = 0;
        for (Slot slot : slots.values()) {
            if (slot.occupied()) {
                n++;
            }
        }
        return n;
    }

    public double occupancyRatio() {
        return occupiedCount() / (double) SlotId.capacity();
    }

    public Inventory with(Slot slot) {
        Map<String, Slot> next = new LinkedHashMap<>(slots);
        next.put(slot.slotId(), slot);
        return new Inventory(next);
    }
}
