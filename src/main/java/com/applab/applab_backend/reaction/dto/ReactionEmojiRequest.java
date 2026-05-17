package com.applab.applab_backend.reaction.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReactionEmojiRequest {
    @NotNull(message = "Emoji is required")
    private String emoji;
}
