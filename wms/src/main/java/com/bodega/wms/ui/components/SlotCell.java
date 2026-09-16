package com.bodega.wms.ui.components;

import com.bodega.wms.model.Slot;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class SlotCell extends VBox {
    private final Label idLabel = new Label();
    private final Label skuLabel = new Label();
    private String slotId;

    public SlotCell() {
        getStyleClass().add("slot-cell");
        idLabel.getStyleClass().addAll("slot-id", "mono-num");
        skuLabel.getStyleClass().add("slot-sku");
        getChildren().addAll(idLabel, skuLabel);
        setMinSize(72, 64);
        setPrefSize(88, 72);
        bind(Slot.empty("A11"), false);
    }

    public String slotId() {
        return slotId;
    }

    public void bind(Slot slot, boolean selected) {
        this.slotId = slot.slotId();
        idLabel.setText(slot.slotId());
        skuLabel.setText(slot.occupied() ? slot.displaySku() : "libre");
        getStyleClass().removeAll("slot-empty", "slot-full", "slot-selected");
        getStyleClass().add(slot.occupied() ? "slot-full" : "slot-empty");
        if (selected) {
            getStyleClass().add("slot-selected");
        }
    }
}
