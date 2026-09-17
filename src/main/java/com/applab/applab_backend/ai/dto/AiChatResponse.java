package com.applab.applab_backend.ai.dto;

import java.time.Instant;

import com.applab.applab_backend.auth.dto.UserListItemResponse;

public record AiChatResponse(
        Long id,
        String aiSessionId,
        String aiModel,
        Long userId,
        String userMessage,
        String assistantResponse,
        String historyResponse,
        String currentRoute,
        Instant createdAt,
        UserListItemResponse user) {
}
