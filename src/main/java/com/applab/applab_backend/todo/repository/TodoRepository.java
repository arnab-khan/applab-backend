package com.applab.applab_backend.todo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.applab.applab_backend.todo.model.TodoModel;

@Repository
public interface TodoRepository extends JpaRepository<TodoModel, Long> {
    Page<TodoModel> findByUserId(long userId, Pageable pageable);

    @Query("""
                SELECT t FROM TodoModel t
                WHERE t.userId = :userId
                AND (:completed IS NULL OR t.completed = :completed)
                AND (
                    :keyword IS NULL OR
                    LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
            """)
    Page<TodoModel> searchTodos(Long userId, String keyword, Boolean completed, Pageable pageable);
}
