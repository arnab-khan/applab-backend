package com.applab.applab_backend.telemetry.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.applab.applab_backend.telemetry.converter.JsonNodeConverter;
import com.applab.applab_backend.telemetry.enums.TelemetryActivityType;
import com.applab.applab_backend.telemetry.enums.TelemetryIdentityType;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "telemetry", indexes = {
        @Index(name = "idx_telemetry_type_id", columnList = "type, id"),
        @Index(name = "idx_telemetry_local_session_id", columnList = "local_session_id, id"),
        @Index(name = "idx_telemetry_local_session_type_id", columnList = "local_session_id, type, id")
})
public class TelemetryModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TelemetryActivityType type;

    @Convert(converter = JsonNodeConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private JsonNode activity;

    @Column(nullable = false)
    private String localSessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TelemetryIdentityType identityType;

    private Long identityId;

    @Column(nullable = false)
    private String route;

    @Column(nullable = false)
    private String browser;

    @Column(nullable = false)
    private String platform;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
