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
                AND (:cursor IS NULL OR m.id < :cursor)
                ORDER BY m.id DESC
            """)
    List<MessageModel> findMessagesByCursor(Long contextId, ContextType contextType, Long parentId, Boolean deleted,
            Long cursor, Pageable pageable);
}
