package com.applab.applab_backend.chatroom.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import com.applab.applab_backend.chatroom.enums.RoomType;
import com.applab.applab_backend.chatroom.model.ChatRoomModel;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoomModel, Long> {
    @Query("""
            SELECT room FROM ChatRoomModel room
            WHERE room.roomType = :roomType
                AND (room.firstUserId = :userId OR room.secondUserId = :userId)
            """)
    Page<ChatRoomModel> findConversations(@Param("userId") Long userId,
            @Param("roomType") RoomType roomType, Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(CASE
                WHEN room.firstUserId = :userId THEN room.firstUserUnreadCount
                ELSE room.secondUserUnreadCount
            END), 0)
            FROM ChatRoomModel room
            WHERE room.roomType = :roomType
                AND (room.firstUserId = :userId OR room.secondUserId = :userId)
            """)
    long getTotalUnreadCount(@Param("userId") Long userId, @Param("roomType") RoomType roomType);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            UPDATE ChatRoomModel room SET
                room.updatedAt = :updatedAt,
                room.firstUserUnreadCount = CASE
                    WHEN room.secondUserId = :senderId THEN room.firstUserUnreadCount + 1
                    ELSE room.firstUserUnreadCount
                END,
                room.secondUserUnreadCount = CASE
                    WHEN room.firstUserId = :senderId THEN room.secondUserUnreadCount + 1
                    ELSE room.secondUserUnreadCount
                END
            WHERE room.id = :chatRoomId AND room.roomType = :roomType
            """)
    int incrementRecipientUnreadCount(@Param("chatRoomId") Long chatRoomId, @Param("senderId") Long senderId,
            @Param("roomType") RoomType roomType, @Param("updatedAt") Instant updatedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            UPDATE ChatRoomModel room SET
                room.firstUserUnreadCount = CASE
                    WHEN room.firstUserId = :userId THEN 0 ELSE room.firstUserUnreadCount
                END,
                room.secondUserUnreadCount = CASE
                    WHEN room.secondUserId = :userId THEN 0 ELSE room.secondUserUnreadCount
                END
            WHERE room.id = :chatRoomId AND room.roomType = :roomType
                AND (room.firstUserId = :userId OR room.secondUserId = :userId)
            """)
    int clearUnreadCount(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId,
            @Param("roomType") RoomType roomType);

    Optional<ChatRoomModel> findByFirstUserIdAndSecondUserId(Long firstUserId, Long secondUserId);

    boolean existsByRoomType(RoomType roomType);

    Optional<ChatRoomModel> findByRoomType(RoomType roomType);
}
