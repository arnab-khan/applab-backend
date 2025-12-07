package com.applab.applab_backend.todo.dto;

import com.applab.applab_backend.todo.validation.TodoValidation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OptionalTodoRequest implements TodoValidation.OptionalTitleValidation, TodoValidation.OptionalDescriptionValidation {
    private String title;
    private String description;
    
    @NotNull(message = "Id is required")
    private Long id;
}
