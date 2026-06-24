package com.applab.applab_backend.chatroom.dto;

import com.applab.applab_backend.message.dto.MessageAuthorResponse;
import com.applab.applab_backend.message.dto.MessageWithAuthorResponse;
import com.applab.applab_backend.message.model.MessageModel;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatRoomMessageSharedResponse {
    private final Long chatRoomId;
    private final MessageModel message;
    private final MessageAuthorResponse author;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final MessageWithAuthorResponse quotedMessage;
}
