package com.applab.applab_backend.todo.service;

import org.springframework.stereotype.Service;

import com.applab.applab_backend.todo.dto.OptionalTodoRequest;
import com.applab.applab_backend.todo.dto.TodoRequest;
import com.applab.applab_backend.todo.model.TodoModel;
import com.applab.applab_backend.todo.repository.TodoRepository;

import jakarta.servlet.http.HttpSession;

@Service
public class TodoService {
    private final TodoRepository todoRepository;

    public TodoService(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    public TodoModel addTodo(TodoRequest todo, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        TodoModel todoModel = new TodoModel();
        todoModel.setTitle(todo.getTitle());
        todoModel.setDescription(todo.getDescription());
        todoModel.setUserId(userId);
        return todoRepository.save(todoModel);
    }

    public TodoModel updateTodo(OptionalTodoRequest todo) {
        Long id = todo.getId();
        String title = todo.getTitle();
        String description = todo.getDescription();
        TodoModel existingtodo = todoRepository.findById(id).orElseThrow(() -> new RuntimeException("Todo not found"));
        if (title != null) {
            existingtodo.setTitle(title);
        }
        if (description != null) {
            existingtodo.setDescription(description);
        }
        return todoRepository.save(existingtodo);
    }
}
