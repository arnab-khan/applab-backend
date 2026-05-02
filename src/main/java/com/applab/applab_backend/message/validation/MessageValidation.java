package com.applab.applab_backend.message.validation;

import com.applab.applab_backend.message.enums.ContextType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public interface MessageValidation {
    public interface ParentIdValidation extends OptionalParentIdValidation {
    }

    public interface OptionalParentIdValidation {
        Long getParentId();
    }

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

    public interface ContentValidation extends OptionalContentValidation {
        @NotBlank(message = "Content is required")
        String getContent();
    }

    public interface OptionalContentValidation {
        @Size(min = 1, max = 5000, message = "Content must be 1-5000 characters")
        String getContent();
    }
}
