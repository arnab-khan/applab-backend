package com.applab.applab_backend.ai.dto;

import java.time.Instant;

public interface AiChatSessionResponse {
    String getAiSessionId();

    String getAiModel();

    Long getChatCount();

    Instant getFirstMessageAt();

    Instant getLastMessageAt();
}
