package com.applab.applab_backend.telemetry.dto;

import java.time.Instant;

import com.applab.applab_backend.telemetry.enums.TelemetryIdentityType;

public interface TelemetryLocalSessionResponse {
    String getLocalSessionId();

    TelemetryIdentityType getIdentityType();

    Long getIdentityId();

    String getBrowser();

    String getPlatform();

    Long getActivityCount();

    Instant getFirstSeenAt();

    Instant getLastSeenAt();
}
