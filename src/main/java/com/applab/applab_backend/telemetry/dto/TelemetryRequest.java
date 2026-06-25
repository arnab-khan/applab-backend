package com.applab.applab_backend.telemetry.dto;

import com.applab.applab_backend.telemetry.enums.TelemetryActivityType;
import com.applab.applab_backend.telemetry.enums.TelemetryIdentityType;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TelemetryRequest {
    @NotBlank
    private String name;

    @NotNull
    private TelemetryActivityType type;

    @NotNull
    private JsonNode activity;

    @NotBlank
    private String localSessionId;

    @NotNull
    private TelemetryIdentityType identityType;

    private Long identityId;

    @NotBlank
    private String route;

    @NotBlank
    private String browser;

    @NotBlank
    private String platform;
}
