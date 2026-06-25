package com.applab.applab_backend.telemetry.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.applab.applab_backend.telemetry.dto.TelemetryLocalSessionResponse;
import com.applab.applab_backend.telemetry.enums.TelemetryActivityType;
import com.applab.applab_backend.telemetry.model.TelemetryModel;

public interface TelemetryRepository extends JpaRepository<TelemetryModel, Long> {

    @Query("""
                SELECT t FROM TelemetryModel t
                WHERE (:type IS NULL OR t.type = :type)
                AND (:localSessionId IS NULL OR t.localSessionId = :localSessionId)
            """)
    Page<TelemetryModel> searchTelemetry(
            TelemetryActivityType type,
            String localSessionId,
            Pageable pageable);

    @Query("""
                SELECT
                    t.localSessionId AS localSessionId,
                    MAX(t.identityType) AS identityType,
                    MAX(t.identityId) AS identityId,
                    MAX(t.browser) AS browser,
                    MAX(t.platform) AS platform,
                    COUNT(t.id) AS activityCount,
                    MIN(t.createdAt) AS firstSeenAt,
                    MAX(t.createdAt) AS lastSeenAt
                FROM TelemetryModel t
                GROUP BY t.localSessionId
            """)
    Page<TelemetryLocalSessionResponse> searchTelemetryLocalSessions(Pageable pageable);
}
