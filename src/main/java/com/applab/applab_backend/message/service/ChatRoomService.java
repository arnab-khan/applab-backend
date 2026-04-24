package com.applab.applab_backend.message.service;

import org.springframework.stereotype.Service;

import com.applab.applab_backend.message.dto.ChatRoomRequest;
import com.applab.applab_backend.message.enums.RoomType;
import com.applab.applab_backend.message.model.ChatRoomModel;
import com.applab.applab_backend.message.repository.ChatRoomRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;

    public ChatRoomModel createChatRoom(ChatRoomRequest chatRoom) {
        if (chatRoom.getRoomType() == RoomType.GLOBAL && chatRoomRepository.existsByRoomType(RoomType.GLOBAL)) {
            throw new RuntimeException("Global chat room already exists");
        }

        ChatRoomModel chatRoomModel = new ChatRoomModel();
        chatRoomModel.setName(chatRoom.getName());
        chatRoomModel.setRoomType(chatRoom.getRoomType());
        return chatRoomRepository.save(chatRoomModel);
    }
}
