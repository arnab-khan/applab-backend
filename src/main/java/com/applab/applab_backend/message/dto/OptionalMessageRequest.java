package com.applab.applab_backend.message.dto;

import com.applab.applab_backend.message.enums.ContextType;
import com.applab.applab_backend.message.validation.MessageValidation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OptionalMessageRequest implements MessageValidation.OptionalParentIdValidation,
        MessageValidation.OptionalContextIdValidation, MessageValidation.OptionalContextTypeValidation,
        MessageValidation.OptionalUserIdValidation, MessageValidation.OptionalGuestSessionIdValidation,
        MessageValidation.OptionalContentValidation {
    private Long parentId;
    private Long contextId;
    private ContextType contextType;
    private Long userId;
    private Long guestSessionId;
    private String content;

    @NotNull(message = "Id is required")
    private Long id;
}
