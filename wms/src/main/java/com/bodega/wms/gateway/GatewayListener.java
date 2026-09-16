package com.bodega.wms.gateway;

import com.bodega.wms.model.ConnectionState;
import com.bodega.wms.model.Inventory;
import com.bodega.wms.model.SrmEvent;
import com.bodega.wms.model.SrmStatus;

public interface GatewayListener {
    void onConnection(ConnectionState state, String detail);

    void onStatus(SrmStatus status);

    void onInventory(Inventory inventory);

    void onEvent(SrmEvent event);
}
