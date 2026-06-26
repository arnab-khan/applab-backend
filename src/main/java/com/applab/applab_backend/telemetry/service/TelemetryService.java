package com.applab.applab_backend.telemetry.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.applab.applab_backend.telemetry.dto.TelemetryRequest;
import com.applab.applab_backend.telemetry.dto.TelemetryLocalSessionResponse;
import com.applab.applab_backend.telemetry.enums.TelemetryActivityType;
import com.applab.applab_backend.telemetry.enums.TelemetryIdentityType;
import com.applab.applab_backend.telemetry.model.TelemetryModel;
import com.applab.applab_backend.telemetry.repository.TelemetryRepository;

@Service
public class TelemetryService {
    private final TelemetryRepository telemetryRepository;

    public TelemetryService(TelemetryRepository telemetryRepository) {
        this.telemetryRepository = telemetryRepository;
    }

    public List<TelemetryModel> addTelemetry(List<TelemetryRequest> telemetry) {
        return telemetryRepository.saveAll(telemetry.stream()
                .map(this::toTelemetryModel)
                .toList());
    }

    private TelemetryModel toTelemetryModel(TelemetryRequest telemetry) {
        TelemetryModel telemetryModel = new TelemetryModel();
        telemetryModel.setName(telemetry.getName());
        telemetryModel.setType(telemetry.getType());
        telemetryModel.setActivity(telemetry.getActivity());
        telemetryModel.setLocalSessionId(telemetry.getLocalSessionId());
        telemetryModel.setIdentityType(telemetry.getIdentityType());
        telemetryModel.setIdentityId(telemetry.getIdentityId());
        telemetryModel.setRoute(telemetry.getRoute());
        telemetryModel.setBrowser(telemetry.getBrowser());
        telemetryModel.setPlatform(telemetry.getPlatform());
        return telemetryModel;
    }

    public Page<TelemetryModel> getAll(
            TelemetryActivityType type,
            String localSessionId,
            Pageable pageable) {
        List<String> allowedSorts = List.of("id", "createdAt", "updatedAt", "name", "type", "identityType", "route");
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedSorts.contains(order.getProperty())) {
                throw new IllegalArgumentException(
                        "Invalid sort field: " + order.getProperty() +
                                ". Allowed fields: " + allowedSorts);
            }
        }
        return telemetryRepository.searchTelemetry(type, localSessionId, pageable);
    }

    public Page<TelemetryLocalSessionResponse> getLocalSessions(Pageable pageable) {
        List<String> allowedSorts = List.of("localSessionId", "identityType", "browser", "platform", "activityCount",
                "firstSeenAt", "lastSeenAt");
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedSorts.contains(order.getProperty())) {
                throw new IllegalArgumentException(
                        "Invalid sort field: " + order.getProperty() +
                                ". Allowed fields: " + allowedSorts);
            }
        }
        return telemetryRepository.searchTelemetryLocalSessions(pageable);
    }
}
