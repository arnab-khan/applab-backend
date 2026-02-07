package com.applab.applab_backend.todo.validation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public interface TodoValidation {
    public interface TitleValidation extends OptionalTitleValidation {
        @NotBlank(message = "Title is required")
        String getTitle();
    }

    public interface OptionalTitleValidation {
        @Size(min = 1, max = 100, message = "Title must be 1–100 characters")
        String getTitle();
    }

    public interface DescriptionValidation extends OptionalDescriptionValidation {
        @NotBlank(message = "Description is required")
        String getDescription();
    }

    public interface OptionalDescriptionValidation {
        @Size(min = 1, max = 500, message = "Description must be 1–500 characters")
        String getDescription();
    }
}
