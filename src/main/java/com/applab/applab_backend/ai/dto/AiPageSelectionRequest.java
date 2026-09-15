package com.applab.applab_backend.ai.dto;

import java.util.List;
import jakarta.validation.constraints.NotBlank;

public record AiPageSelectionRequest(
        @NotBlank String message,
        String currentRoute,
        UserType userType,
        List<ChatMessage> history) {

    public enum UserType {
        LOGGED_IN,
        GUEST
    }

    public enum Role {
        USER,
        ASSISTANT
    }

    public record ChatMessage(
            Role role,
            String message) {
    }
}
