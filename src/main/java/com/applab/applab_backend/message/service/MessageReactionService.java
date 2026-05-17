package com.applab.applab_backend.message.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.applab.applab_backend.message.dto.MessageWithAuthorResponse;
import com.applab.applab_backend.message.dto.MessageWithAuthorAndReactionsResponse;
import com.applab.applab_backend.message.enums.ContextType;
import com.applab.applab_backend.message.model.MessageModel;
import com.applab.applab_backend.reaction.dto.ReactionCountResponse;
import com.applab.applab_backend.reaction.service.ReactionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageReactionService {
    private final MessageAuthorService messageAuthorService;
    private final ReactionService reactionService;

    // Returns messages with author details and grouped reaction counts.
    public List<MessageWithAuthorAndReactionsResponse> getMessageResponsesWithAuthorsAndReactions(
            List<MessageModel> messages, ContextType reactionContextType) {
        List<MessageWithAuthorResponse> messageResponses = messageAuthorService.getMessageResponsesWithAuthors(messages);
        Map<Long, List<ReactionCountResponse>> reactionCountsByMessageId = reactionService
                .getReactionCountsByContextIds(messages.stream().map(MessageModel::getId).toList(),
                        reactionContextType);

        return messageResponses.stream()
                .map(messageResponse -> new MessageWithAuthorAndReactionsResponse(
                        messageResponse.getMessage(),
                        messageResponse.getAuthor(),
                        reactionCountsByMessageId.getOrDefault(messageResponse.getMessage().getId(), List.of())))
                .toList();
    }

    // Reuses the list response builder for one message.
    public MessageWithAuthorAndReactionsResponse getMessageResponseWithAuthorAndReactions(MessageModel message,
            ContextType reactionContextType) {
        return getMessageResponsesWithAuthorsAndReactions(List.of(message), reactionContextType).get(0);
    }
}
