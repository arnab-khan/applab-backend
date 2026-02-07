package com.applab.applab_backend.todo.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    public TodoModel updateTodo(OptionalTodoRequest todo, HttpSession session) {
        Long id = todo.getId();
        String title = todo.getTitle();
        String description = todo.getDescription();
        TodoModel existingTodo = validateTodoOwnership(id, session);
        if (title != null) {
            existingTodo.setTitle(title);
        }
        if (description != null) {
            existingTodo.setDescription(description);
        }
        return todoRepository.save(existingTodo);
    }

    public Page<TodoModel> getAll(Pageable pageable, HttpSession session) {

        List<String> allowedSorts = List.of("createdAt", "updatedAt", "title"); // Allowed sort fields list
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedSorts.contains(order.getProperty())) {
                throw new IllegalArgumentException(
                        "Invalid sort field: " + order.getProperty() +
                                ". Allowed fields: " + allowedSorts);
            }
        }

        Long userId = (Long) session.getAttribute("userId");
        return todoRepository.findByUserId(userId, pageable);
    }

    public TodoModel markAsComplete(Long id, boolean completed, HttpSession session) {
        TodoModel existingTodo = validateTodoOwnership(id, session);
        existingTodo.setCompleted(completed);
        return todoRepository.save(existingTodo);
    }

    private TodoModel validateTodoOwnership(Long id, HttpSession session) {
        TodoModel existingTodo = todoRepository.findById(id).orElseThrow(() -> new RuntimeException("Todo not found"));
        Long existingTodoUserId = existingTodo.getUserId();
        Long userId = (Long) session.getAttribute("userId");
        if (!existingTodoUserId.equals(userId)) {
            throw new RuntimeException("Unauthorized, you can only update your own todos");
        }
        return existingTodo;
    }
}
