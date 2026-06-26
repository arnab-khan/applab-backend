package com.applab.applab_backend.chatroom.dto;

import java.util.List;

import com.applab.applab_backend.reaction.service.ReactionAuthorService.ReactionWithAuthorResponse;

public record ChatRoomReactionPageResponse(
        List<ReactionWithAuthorResponse> items,
        Long nextCursor,
        boolean hasNext) {
}
