package com.applab.applab_backend.todo.controller;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.applab.applab_backend.todo.dto.OptionalTodoRequest;
import com.applab.applab_backend.todo.dto.TodoRequest;
import com.applab.applab_backend.todo.model.TodoModel;
import com.applab.applab_backend.todo.service.TodoService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/todo")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @PostMapping("/add")
    public TodoModel addTodo(@Valid @RequestBody TodoRequest todo, HttpSession session) {
        return todoService.addTodo(todo, session);
    }

    @PatchMapping("update")
    public TodoModel updateTodo(@Valid @RequestBody OptionalTodoRequest todo) {
        return todoService.updateTodo(todo);
    }

}
