package com.applab.applab_backend.chatroom.dto;

import com.applab.applab_backend.message.dto.MessagePermissionResponse;
import com.applab.applab_backend.reaction.model.ReactionModel;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatRoomMessageViewerStateResponse {
    private final Long messageId;
    private final MessagePermissionResponse permission;
    private final ReactionModel myReaction;
}
