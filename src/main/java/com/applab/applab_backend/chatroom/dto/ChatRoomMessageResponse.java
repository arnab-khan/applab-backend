package com.applab.applab_backend.chatroom.dto;

import java.util.List;

import com.applab.applab_backend.message.dto.ChatMessageResponse;
import com.applab.applab_backend.message.dto.MessageAuthorResponse;
import com.applab.applab_backend.message.model.MessageModel;
import com.applab.applab_backend.reaction.dto.ReactionCountResponse;

import lombok.Getter;

@Getter
public class ChatRoomMessageResponse extends ChatMessageResponse {
    private final Long chatRoomId;
    private final List<ReactionCountResponse> reactions;

    public ChatRoomMessageResponse(Long chatRoomId, MessageModel message, MessageAuthorResponse author,
            List<ReactionCountResponse> reactions, boolean canEdit, boolean canDelete) {
        super(message, author, canEdit, canDelete);
        this.chatRoomId = chatRoomId;
        this.reactions = reactions;
    }
}
