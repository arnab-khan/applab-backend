package com.applab.applab_backend.chatroom.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.applab.applab_backend.chatroom.enums.RoomType;
import com.applab.applab_backend.chatroom.model.ChatRoomModel;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoomModel, Long> {
    boolean existsByRoomType(RoomType roomType);

    Optional<ChatRoomModel> findByRoomType(RoomType roomType);
}
