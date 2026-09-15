package com.applab.applab_backend.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiPageSelectionRequest(
        @NotBlank @Size(max = 250) String message,
        @Size(max = 500) String currentRoute,
        @Size(max = 3000) String history) {
}
