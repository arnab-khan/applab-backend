package com.applab.applab_backend.telemetry.service;

import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.applab.applab_backend.auth.dto.UserListItemResponse;
import com.applab.applab_backend.auth.repository.UserRepository;
import com.applab.applab_backend.telemetry.dto.TelemetryResponse;

import com.applab.applab_backend.telemetry.dto.TelemetryRequest;
import com.applab.applab_backend.telemetry.dto.TelemetryLocalSessionResponse;
import com.applab.applab_backend.telemetry.enums.TelemetryActivityType;
import com.applab.applab_backend.telemetry.enums.TelemetryIdentityType;
import com.applab.applab_backend.telemetry.model.TelemetryModel;
import com.applab.applab_backend.telemetry.repository.TelemetryRepository;

@Service
public class TelemetryService {
    private final TelemetryRepository telemetryRepository;
    private final UserRepository userRepository;

    public TelemetryService(TelemetryRepository telemetryRepository, UserRepository userRepository) {
        this.telemetryRepository = telemetryRepository;
        this.userRepository = userRepository;
    }

    public List<TelemetryModel> addTelemetry(List<TelemetryRequest> telemetry) {
        return telemetryRepository.saveAll(telemetry.stream()
                .map(this::toTelemetryModel)
                .toList());
    }

    private TelemetryModel toTelemetryModel(TelemetryRequest telemetry) {
        TelemetryModel telemetryModel = new TelemetryModel();
        telemetryModel.setName(telemetry.getName());
        String type = telemetry.getType();
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Telemetry type is required. Allowed types: "
                    + Arrays.toString(TelemetryActivityType.values()) + ".");
        }
        try {
            telemetryModel.setType(TelemetryActivityType.valueOf(type));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid telemetry type '" + type + "'. Allowed types: "
                    + Arrays.toString(TelemetryActivityType.values()) + ".");
        }
        telemetryModel.setActivity(telemetry.getActivity());
        telemetryModel.setLocalSessionId(telemetry.getLocalSessionId());
        telemetryModel.setIdentityType(telemetry.getIdentityType());
        telemetryModel.setIdentityId(telemetry.getIdentityId());
        telemetryModel.setRoute(telemetry.getRoute());
        telemetryModel.setBrowser(telemetry.getBrowser());
        telemetryModel.setPlatform(telemetry.getPlatform());
        return telemetryModel;
    }

    @Transactional(readOnly = true)
    public Page<TelemetryResponse> getAll(
            TelemetryActivityType type,
            String localSessionId,
            Boolean success,
            Pageable pageable) {
        List<String> allowedSorts = List.of("id", "createdAt", "updatedAt", "name", "type", "identityType", "route");
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedSorts.contains(order.getProperty())) {
                throw new IllegalArgumentException(
                        "Invalid sort field: " + order.getProperty() +
                                ". Allowed fields: " + allowedSorts);
            }
        }
        Page<TelemetryModel> telemetry = telemetryRepository.searchTelemetry(
                type,
                localSessionId,
                success != null ? success.toString() : null,
                pageable);
        Map<Long, UserListItemResponse> users = getUsers(telemetry.getContent().stream()
                .filter(item -> item.getIdentityType() == TelemetryIdentityType.USER)
                .map(TelemetryModel::getIdentityId).toList());
        return telemetry.map(item -> new TelemetryResponse(item,
                item.getIdentityType() == TelemetryIdentityType.USER ? users.get(item.getIdentityId()) : null));
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

    private Map<Long, UserListItemResponse> getUsers(List<Long> identityIds) {
        List<Long> userIds = identityIds.stream().filter(Objects::nonNull).distinct().toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream().collect(Collectors.toMap(
                user -> user.getId(),
                user -> new UserListItemResponse(user.getId(), user.getName(), user.getUsername(),
                        user.getBio(), user.getCreatedAt(), user.getUpdatedAt(),
                        user.getProfileImageUrl(), user.getCompressedProfileImageUrl())));
    }
}
