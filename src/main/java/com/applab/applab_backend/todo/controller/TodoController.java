package com.applab.applab_backend.todo.controller;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.applab.applab_backend.todo.dto.OptionalTodoRequest;
import com.applab.applab_backend.todo.dto.TodoRequest;
import com.applab.applab_backend.todo.model.TodoModel;
import com.applab.applab_backend.todo.service.TodoService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/todo")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping("/all")
    public Page<TodoModel> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean completed,
            Pageable pageable,
            HttpSession session) {
        return todoService.getAll(keyword, completed, pageable, session);
    }

    @PostMapping("/add")
    public TodoModel addTodo(@Valid @RequestBody TodoRequest todo, HttpSession session) {
        return todoService.addTodo(todo, session);
    }

    @PatchMapping("/update")
    public TodoModel updateTodo(@Valid @RequestBody OptionalTodoRequest todo, HttpSession session) {
        return todoService.updateTodo(todo, session);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteTodo(@PathVariable Long id, HttpSession session) {
        todoService.deleteTodo(id, session);
    }

    @PatchMapping("/complete")
    public TodoModel markAsComplete(@RequestParam Long id, HttpSession session) {
        return todoService.markAsComplete(id, session);
    }

}
