package com.applab.applab_backend.chatroom.dto;

import com.applab.applab_backend.message.dto.ChatMessageResponse;
import com.applab.applab_backend.message.dto.MessageAuthorResponse;
import com.applab.applab_backend.message.model.MessageModel;

import lombok.Getter;

@Getter
public class ChatRoomMessageResponse extends ChatMessageResponse {
    private final Long chatRoomId;

    public ChatRoomMessageResponse(Long chatRoomId, MessageModel message, MessageAuthorResponse author,
            boolean canEdit, boolean canDelete) {
        super(message, author, canEdit, canDelete);
        this.chatRoomId = chatRoomId;
    }
}
