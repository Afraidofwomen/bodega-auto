package com.bodega.wms.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public record AlertItem(Instant at, Severity severity, String title, String detail) {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public enum Severity {
        INFO, PROCESS, SUCCESS, WARNING, ERROR
    }

    public String clock() {
        return TIME.format(at);
    }
}
