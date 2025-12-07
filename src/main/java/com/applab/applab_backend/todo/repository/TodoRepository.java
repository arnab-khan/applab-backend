package com.applab.applab_backend.todo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.applab.applab_backend.todo.model.TodoModel;

@Repository
public interface TodoRepository extends JpaRepository<TodoModel, Long> {
    List<TodoModel> findByUserId(long userId);
}
