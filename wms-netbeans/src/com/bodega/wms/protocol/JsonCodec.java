package com.bodega.wms.protocol;

import com.bodega.wms.model.Inventory;
import com.bodega.wms.model.Position;
import com.bodega.wms.model.Slot;
import com.bodega.wms.model.SrmEvent;
import com.bodega.wms.model.SrmState;
import com.bodega.wms.model.SrmStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class JsonCodec {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private JsonCodec() {
    }

    public static String newCmdId() {
        return UUID.randomUUID().toString();
    }

    public static String store(String cmdId, String slotId, String sku) {
        return write(new StoreCmd(cmdId, slotId, sku == null ? "" : sku));
    }

    public static String retrieve(String cmdId, String slotId) {
        return write(new RetrieveCmd(cmdId, slotId));
    }

    public static String abort(String cmdId, String action) {
        return write(new AbortCmd(cmdId, action));
    }

    public static SrmStatus status(String json) {
        StatusDto dto = read(json, StatusDto.class);
        Position position = dto.position == null
                ? Position.origin()
                : new Position(dto.position.xMm, dto.position.yMm, dto.position.zMm);
        return new SrmStatus(
                dto.deviceId == null ? "srm1" : dto.deviceId,
                SrmState.fromWire(dto.state),
                dto.homed,
                dto.cmdId,
                dto.mission,
                position,
                dto.forkOccupied,
                dto.ioOccupied,
                dto.estop,
                dto.error
        );
    }

    public static SrmEvent event(String json) {
        EventDto dto = read(json, EventDto.class);
        return new SrmEvent(dto.cmdId, dto.type, dto.mission, dto.slotId, dto.error, dto.state);
    }

    public static Inventory inventory(String json) {
        InventoryDto dto = read(json, InventoryDto.class);
        Map<String, Slot> map = new LinkedHashMap<>();
        if (dto.slots != null) {
            for (SlotDto slot : dto.slots) {
                if (slot.slotId != null) {
                    map.put(slot.slotId, new Slot(slot.slotId, slot.occupied, slot.sku));
                }
            }
        }
        return new Inventory(map);
    }

    public static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo serializar JSON", e);
        }
    }

    private static <T> T read(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON inválido", e);
        }
    }

    public record StoreCmd(
            @JsonProperty("cmd_id") String cmdId,
            @JsonProperty("slot_id") String slotId,
            String sku
    ) {
    }

    public record RetrieveCmd(
            @JsonProperty("cmd_id") String cmdId,
            @JsonProperty("slot_id") String slotId
    ) {
    }

    public record AbortCmd(
            @JsonProperty("cmd_id") String cmdId,
            String action
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatusDto(
            @JsonProperty("device_id") String deviceId,
            String state,
            boolean homed,
            @JsonProperty("cmd_id") String cmdId,
            String mission,
            PositionDto position,
            @JsonProperty("fork_occupied") boolean forkOccupied,
            @JsonProperty("io_occupied") boolean ioOccupied,
            boolean estop,
            String error
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PositionDto(
            @JsonProperty("x_mm") double xMm,
            @JsonProperty("y_mm") double yMm,
            @JsonProperty("z_mm") double zMm
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventDto(
            @JsonProperty("cmd_id") String cmdId,
            String type,
            String mission,
            @JsonProperty("slot_id") String slotId,
            String error,
            String state
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InventoryDto(List<SlotDto> slots) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SlotDto(
            @JsonProperty("slot_id") String slotId,
            boolean occupied,
            String sku
    ) {
    }
}
