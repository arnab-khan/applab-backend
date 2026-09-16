package com.applab.applab_backend.ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import com.applab.applab_backend.ai.dto.AiChatSessionResponse;
import com.applab.applab_backend.ai.model.AiChatModel;

public interface AiChatRepository extends JpaRepository<AiChatModel, Long> {

    @Query("""
                SELECT a FROM AiChatModel a
                WHERE (:aiSessionId IS NULL OR a.aiSessionId = :aiSessionId)
            """)
    Page<AiChatModel> findChats(String aiSessionId, Pageable pageable);

    @Query("""
                SELECT
                    a.aiSessionId AS aiSessionId,
                    MAX(a.aiModel) AS aiModel,
                    COUNT(a.id) AS chatCount,
                    MIN(a.createdAt) AS firstMessageAt,
                    MAX(a.createdAt) AS lastMessageAt
                FROM AiChatModel a
                GROUP BY a.aiSessionId
            """)
    Page<AiChatSessionResponse> findChatSessions(Pageable pageable);
}
