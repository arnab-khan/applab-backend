package com.applab.applab_backend.reaction.dto;

import com.applab.applab_backend.message.enums.ContextType;
import com.applab.applab_backend.reaction.validation.ReactionValidation;

import lombok.Data;

@Data
public class ReactionRequest implements ReactionValidation.ContextIdValidation,
        ReactionValidation.ContextTypeValidation, ReactionValidation.OptionalUserIdValidation,
        ReactionValidation.OptionalGuestSessionIdValidation, ReactionValidation.EmojiValidation {
    private Long contextId;
    private ContextType contextType;
    private Long userId;
    private Long guestSessionId;
    private String emoji;
}
