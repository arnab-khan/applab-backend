package com.applab.applab_backend.todo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.applab.applab_backend.todo.model.TodoModel;

@Repository
public interface TodoRepository extends JpaRepository<TodoModel, Long> {
    Page<TodoModel> findByUserId(long userId, Pageable pageable);
}
