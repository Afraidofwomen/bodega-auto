package com.bodega.wms.ui.components;

import com.bodega.wms.model.SrmState;
import javafx.scene.control.Label;

public final class StatusBadge extends Label {
    public StatusBadge() {
        getStyleClass().add("status-badge");
        setState(SrmState.UNKNOWN);
    }

    public void setState(SrmState state) {
        getStyleClass().removeIf(c -> c.startsWith("state-"));
        getStyleClass().add("state-" + state.wire());
        setText(switch (state) {
            case HOMING -> "HOMING";
            case IDLE -> "IDLE";
            case MOVING -> "MOVING";
            case PICKING -> "PICKING";
            case PLACING -> "PLACING";
            case FAULT -> "FAULT";
            case UNKNOWN -> "OFF";
        });
    }
}
