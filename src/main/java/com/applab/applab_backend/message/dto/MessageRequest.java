package com.applab.applab_backend.message.dto;

import com.applab.applab_backend.message.enums.ContextType;
import com.applab.applab_backend.message.validation.MessageValidation;

import lombok.Data;

@Data
public class MessageRequest implements MessageValidation.ParentIdValidation, MessageValidation.ContextIdValidation,
        MessageValidation.ContextTypeValidation, MessageValidation.OptionalUserIdValidation,
        MessageValidation.ContentValidation {
    private Long parentId;
    private Long contextId;
    private ContextType contextType;
    private Long userId;
    private String content;
}
