package com.applab.applab_backend.telemetry.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.applab.applab_backend.telemetry.dto.TelemetryRequest;
import com.applab.applab_backend.telemetry.dto.TelemetryLocalSessionResponse;
import com.applab.applab_backend.telemetry.enums.TelemetryActivityType;
import com.applab.applab_backend.telemetry.enums.TelemetryIdentityType;
import com.applab.applab_backend.telemetry.model.TelemetryModel;
import com.applab.applab_backend.telemetry.service.TelemetryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/telemetry")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @GetMapping("/all")
    public Page<TelemetryModel> getAll(
            @RequestParam(required = false) TelemetryActivityType type,
            @RequestParam(required = false) String localSessionId,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return telemetryService.getAll(type, localSessionId, pageable);
    }

    @GetMapping("/local-sessions")
    public Page<TelemetryLocalSessionResponse> getLocalSessions(
            @PageableDefault(sort = "lastSeenAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return telemetryService.getLocalSessions(pageable);
    }

    @PostMapping("/add")
    public List<TelemetryModel> addTelemetry(@Valid @RequestBody List<TelemetryRequest> telemetry) {
        return telemetryService.addTelemetry(telemetry);
    }
}
