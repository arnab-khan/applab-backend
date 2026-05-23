package com.applab.applab_backend.chatroom.dto;

import java.util.List;

import com.applab.applab_backend.message.dto.ChatMessageResponse;
import com.applab.applab_backend.message.dto.MessageAuthorResponse;
import com.applab.applab_backend.message.dto.MessageWithAuthorResponse;
import com.applab.applab_backend.message.model.MessageModel;
import com.applab.applab_backend.reaction.dto.ReactionCountResponse;
import com.applab.applab_backend.reaction.model.ReactionModel;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

@Getter
public class ChatRoomMessageResponse extends ChatMessageResponse {
    private final Long chatRoomId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final MessageWithAuthorResponse quotedMessage;
    private final List<ReactionCountResponse> reactions;
    private final ReactionModel myReaction;

    public ChatRoomMessageResponse(Long chatRoomId, MessageModel message, MessageAuthorResponse author,
            List<ReactionCountResponse> reactions, ReactionModel myReaction, boolean canEdit, boolean canDelete) {
        this(chatRoomId, message, author, null, reactions, myReaction, canEdit, canDelete);
    }

    public ChatRoomMessageResponse(Long chatRoomId, MessageModel message, MessageAuthorResponse author,
            MessageWithAuthorResponse quotedMessage, List<ReactionCountResponse> reactions, ReactionModel myReaction,
            boolean canEdit, boolean canDelete) {
        super(message, author, canEdit, canDelete);
        this.chatRoomId = chatRoomId;
        this.quotedMessage = quotedMessage;
        this.reactions = reactions;
        this.myReaction = myReaction;
    }
}
