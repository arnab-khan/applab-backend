package com.applab.applab_backend.chatroom.dto;

import com.applab.applab_backend.chatroom.model.ChatRoomModel;
import com.applab.applab_backend.message.dto.MessageAuthorResponse;

public record ChatRoomConversationResponse(
        ChatRoomModel chatRoom,
        MessageAuthorResponse user,
        long unreadCount) {
}
