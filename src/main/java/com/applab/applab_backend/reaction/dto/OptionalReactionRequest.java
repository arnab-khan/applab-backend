package com.applab.applab_backend.reaction.dto;

import com.applab.applab_backend.message.enums.ContextType;
import com.applab.applab_backend.reaction.validation.ReactionValidation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OptionalReactionRequest implements ReactionValidation.OptionalContextIdValidation,
        ReactionValidation.OptionalContextTypeValidation, ReactionValidation.OptionalUserIdValidation,
        ReactionValidation.OptionalGuestSessionIdValidation, ReactionValidation.OptionalEmojiValidation {
    private Long contextId;
    private ContextType contextType;
    private Long userId;
    private Long guestSessionId;
    private String emoji;

    @NotNull(message = "Id is required")
    private Long id;
}
