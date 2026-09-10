package com.applab.applab_backend.chatroom.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

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

    Optional<ChatRoomModel> findByFirstUserIdAndSecondUserId(Long firstUserId, Long secondUserId);

    boolean existsByRoomType(RoomType roomType);

    Optional<ChatRoomModel> findByRoomType(RoomType roomType);
}
