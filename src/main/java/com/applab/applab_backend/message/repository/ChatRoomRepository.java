package com.applab.applab_backend.message.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.applab.applab_backend.message.enums.RoomType;
import com.applab.applab_backend.message.model.ChatRoomModel;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoomModel, Long> {
    boolean existsByRoomType(RoomType roomType);
}
