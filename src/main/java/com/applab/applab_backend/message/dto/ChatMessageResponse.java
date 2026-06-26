package com.applab.applab_backend.message.dto;

import com.applab.applab_backend.message.model.MessageModel;

import lombok.Getter;

@Getter
public class ChatMessageResponse {
    private final MessageModel message;
    private final MessageAuthorResponse author;
    private final MessagePermissionResponse permission;

    public ChatMessageResponse(MessageModel message, MessageAuthorResponse author, boolean canEdit,
            boolean canDelete) {
        this.message = message;
        this.author = author;
        this.permission = new MessagePermissionResponse(canEdit, canDelete);
    }
}
