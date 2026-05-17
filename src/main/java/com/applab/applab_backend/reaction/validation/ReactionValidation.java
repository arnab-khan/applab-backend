package com.applab.applab_backend.reaction.validation;

import com.applab.applab_backend.message.enums.ContextType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public interface ReactionValidation {
    public interface ContextIdValidation extends OptionalContextIdValidation {
        @NotNull(message = "Context id is required")
        Long getContextId();
    }

    public interface OptionalContextIdValidation {
        Long getContextId();
    }

    public interface ContextTypeValidation extends OptionalContextTypeValidation {
        @NotNull(message = "Context type is required")
        ContextType getContextType();
    }

    public interface OptionalContextTypeValidation {
        ContextType getContextType();
    }

    public interface UserIdValidation extends OptionalUserIdValidation {
        @NotNull(message = "User id is required")
        Long getUserId();
    }

    public interface OptionalUserIdValidation {
        Long getUserId();
    }

    public interface GuestSessionIdValidation extends OptionalGuestSessionIdValidation {
        @NotNull(message = "Guest session id is required")
        Long getGuestSessionId();
    }

    public interface OptionalGuestSessionIdValidation {
        Long getGuestSessionId();
    }

    public interface EmojiValidation extends OptionalEmojiValidation {
        @NotBlank(message = "Emoji is required")
        String getEmoji();
    }

    public interface OptionalEmojiValidation {
        @Size(min = 1, max = 100, message = "Emoji must be 1-100 characters")
        String getEmoji();
    }
}
