package com.applab.applab_backend.reaction.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.applab.applab_backend.message.enums.ContextType;
import com.applab.applab_backend.reaction.dto.ReactionContextCountResponse;
import com.applab.applab_backend.reaction.model.ReactionModel;

@Repository
public interface ReactionRepository extends JpaRepository<ReactionModel, Long> {
    List<ReactionModel> findByContextIdAndContextTypeOrderByIdDesc(Long contextId, ContextType contextType);

    Optional<ReactionModel> findByContextIdAndContextTypeAndUserId(Long contextId, ContextType contextType,
            Long userId);

    Optional<ReactionModel> findByContextIdAndContextTypeAndGuestSessionId(Long contextId, ContextType contextType,
            Long guestSessionId);

    List<ReactionModel> findByContextIdInAndContextTypeAndUserId(List<Long> contextIds, ContextType contextType,
            Long userId);

    List<ReactionModel> findByContextIdInAndContextTypeAndGuestSessionId(List<Long> contextIds,
            ContextType contextType, Long guestSessionId);

    @Query("""
                SELECT new com.applab.applab_backend.reaction.dto.ReactionContextCountResponse(
                    r.contextId,
                    r.emoji,
                    COUNT(r.id)
                )
                FROM ReactionModel r
                WHERE r.contextId IN :contextIds
                AND r.contextType = :contextType
                GROUP BY r.contextId, r.emoji
            """)
    List<ReactionContextCountResponse> countReactionsByContextIds(List<Long> contextIds, ContextType contextType);

    @Query("""
                SELECT r FROM ReactionModel r
                WHERE r.contextId = :contextId
                AND r.contextType = :contextType
                AND (:emoji IS NULL OR r.emoji = :emoji)
                AND (:cursor IS NULL OR r.id <= :cursor)
                ORDER BY r.id DESC
            """)
    List<ReactionModel> findReactionsByCursor(Long contextId, ContextType contextType, String emoji, Long cursor,
            Pageable pageable);
}
