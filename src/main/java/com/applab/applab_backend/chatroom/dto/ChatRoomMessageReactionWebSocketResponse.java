package com.applab.applab_backend.chatroom.dto;

import java.util.List;

import com.applab.applab_backend.reaction.dto.ReactionCountResponse;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class ChatRoomMessageReactionWebSocketResponse {
    private final Long chatRoomId;
    private final Message message;
    private final List<ReactionCountResponse> reactions;

    public ChatRoomMessageReactionWebSocketResponse(Long chatRoomId, Long messageId,
            List<ReactionCountResponse> reactions) {
        this.chatRoomId = chatRoomId;
        this.message = new Message(messageId);
        this.reactions = reactions;
    }

    @Getter
    @AllArgsConstructor
    public static class Message {
        private final Long id;
    }
}
