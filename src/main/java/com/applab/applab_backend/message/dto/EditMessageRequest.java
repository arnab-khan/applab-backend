package com.applab.applab_backend.message.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EditMessageRequest {
    @NotNull(message = "Id is required")
    private Long id;
    private String content;
    private Boolean removeQuotedMessage;
}
