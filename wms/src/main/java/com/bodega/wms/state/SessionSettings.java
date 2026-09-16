package com.bodega.wms.state;

import com.bodega.wms.model.GatewayKind;

public final class SessionSettings {
    private GatewayKind kind = GatewayKind.SIMULATED;
    private String brokerHost = "127.0.0.1";
    private int brokerPort = 1883;
    private String deviceId = "srm1";
    private String clientId = "wms-ui";
    private String username = "";
    private String password = "";

    public GatewayKind getKind() {
        return kind;
    }

    public void setKind(GatewayKind kind) {
        this.kind = kind;
    }

    public String getBrokerHost() {
        return brokerHost;
    }

    public void setBrokerHost(String brokerHost) {
        this.brokerHost = brokerHost;
    }

    public int getBrokerPort() {
        return brokerPort;
    }

    public void setBrokerPort(int brokerPort) {
        this.brokerPort = brokerPort;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String brokerUri() {
        return "tcp://" + brokerHost + ":" + brokerPort;
    }
}
