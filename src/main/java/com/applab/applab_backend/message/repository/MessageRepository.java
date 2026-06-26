package com.applab.applab_backend.message.repository;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.applab.applab_backend.message.enums.ContextType;
import com.applab.applab_backend.message.model.MessageModel;

@Repository
public interface MessageRepository extends JpaRepository<MessageModel, Long> {
    @Query("""
                SELECT m FROM MessageModel m
                WHERE m.contextId = :contextId
                AND m.contextType = :contextType
                AND (:parentId IS NULL OR m.parentId = :parentId)
                AND (:deleted IS NULL OR m.deleted = :deleted)
                AND (:cursor IS NULL OR m.id <= :cursor)
                AND (:uptoId IS NULL OR m.id >= :uptoId)
                ORDER BY m.id DESC
            """)
    List<MessageModel> findOlderMessagesByCursor(Long contextId, ContextType contextType, Long parentId,
            Boolean deleted, Long cursor, Long uptoId, Pageable pageable);

    @Query("""
                SELECT m FROM MessageModel m
                WHERE m.contextId = :contextId
                AND m.contextType = :contextType
                AND (:parentId IS NULL OR m.parentId = :parentId)
                AND (:deleted IS NULL OR m.deleted = :deleted)
                AND (:cursor IS NULL OR m.id >= :cursor)
                AND (:uptoId IS NULL OR m.id <= :uptoId)
                ORDER BY m.id ASC
            """)
    List<MessageModel> findNewerMessagesByCursor(Long contextId, ContextType contextType, Long parentId, Boolean deleted,
            Long cursor, Long uptoId, Pageable pageable);
}
