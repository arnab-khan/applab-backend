package com.applab.applab_backend.message.dto;

import java.util.List;

import com.applab.applab_backend.message.model.MessageModel;
import com.applab.applab_backend.reaction.dto.ReactionCountResponse;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MessageWithAuthorAndReactionsResponse {
    private final MessageModel message;
    private final MessageAuthorResponse author;
    private final List<ReactionCountResponse> reactions;
}
