package com.bodega.wms.model;

public record SrmStatus(
        String deviceId,
        SrmState state,
        boolean homed,
        String cmdId,
        String mission,
        Position position,
        boolean forkOccupied,
        boolean ioOccupied,
        boolean estop,
        String error
) {
    public static SrmStatus disconnected() {
        return new SrmStatus(
                "—",
                SrmState.UNKNOWN,
                false,
                null,
                null,
                Position.origin(),
                false,
                false,
                false,
                null
        );
    }

    public boolean canAcceptMission() {
        return state == SrmState.IDLE && homed && !estop;
    }
}
