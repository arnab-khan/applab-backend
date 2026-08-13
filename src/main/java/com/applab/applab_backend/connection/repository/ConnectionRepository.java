package com.applab.applab_backend.connection.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.applab.applab_backend.connection.enums.ConnectionStatus;
import com.applab.applab_backend.connection.model.ConnectionModel;

@Repository
public interface ConnectionRepository extends JpaRepository<ConnectionModel, Long> {

    boolean existsBySenderUserIdAndReceiverUserId(Long senderUserId, Long receiverUserId);

    @Query("""
                SELECT COUNT(c) > 0 FROM ConnectionModel c
                WHERE (c.senderUserId = :userId1 AND c.receiverUserId = :userId2)
                OR (c.senderUserId = :userId2 AND c.receiverUserId = :userId1)
            """)
    boolean existsConnectionBetweenUsers(Long userId1, Long userId2);

    @Query("""
                SELECT c FROM ConnectionModel c
                WHERE (c.senderUserId = :currentUserId AND c.receiverUserId = :userId)
                OR (c.senderUserId = :userId AND c.receiverUserId = :currentUserId)
            """)
    Optional<ConnectionModel> findConnectionBetweenUsers(Long currentUserId, Long userId);

    @Query("""
                SELECT c FROM ConnectionModel c
                JOIN UserModel sender ON sender.id = c.senderUserId
                JOIN UserModel receiver ON receiver.id = c.receiverUserId
                WHERE (c.senderUserId = :userId OR c.receiverUserId = :userId)
                AND (:status IS NULL OR c.status = :status)
                AND (
                    :keyword IS NULL OR
                    LOWER(CASE WHEN c.senderUserId = :userId THEN receiver.name ELSE sender.name END)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
                ORDER BY
                    CASE WHEN :sortBy = 'name' AND :sortDirection = 'asc'
                        THEN LOWER(CASE WHEN c.senderUserId = :userId THEN receiver.name ELSE sender.name END)
                    END ASC,
                    CASE WHEN :sortBy = 'name' AND :sortDirection = 'desc'
                        THEN LOWER(CASE WHEN c.senderUserId = :userId THEN receiver.name ELSE sender.name END)
                    END DESC,
                    CASE WHEN :sortBy = 'updatedAt' AND :sortDirection = 'asc' THEN c.updatedAt END ASC,
                    CASE WHEN :sortBy = 'updatedAt' AND :sortDirection = 'desc' THEN c.updatedAt END DESC,
                    c.updatedAt DESC
            """)
    Page<ConnectionModel> searchConnections(Long userId, ConnectionStatus status, String keyword, String sortBy,
            String sortDirection, Pageable pageable);
}
