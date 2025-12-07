package com.applab.applab_backend.todo.dto;

import com.applab.applab_backend.todo.validation.TodoValidation;

import lombok.Data;

@Data
public class TodoRequest implements TodoValidation.TitleValidation, TodoValidation.DescriptionValidation {
    private String title;
    private String description;
}
