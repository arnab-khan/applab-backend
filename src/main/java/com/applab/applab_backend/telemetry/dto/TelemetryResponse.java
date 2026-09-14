package com.applab.applab_backend.telemetry.dto;

import com.applab.applab_backend.auth.dto.UserListItemResponse;
import com.applab.applab_backend.telemetry.model.TelemetryModel;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record TelemetryResponse(
        @JsonUnwrapped TelemetryModel telemetry,
        UserListItemResponse user) {
}
