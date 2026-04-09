package com.applab.applab_backend.auth.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.applab.applab_backend.auth.model.UserModel;

@Repository
public interface UserRepository extends JpaRepository<UserModel, Long> {

    @Query("""
                SELECT u FROM UserModel u
                WHERE (
                    :keyword IS NULL OR
                    LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
            """)
    Page<UserModel> searchUsers(String keyword, Pageable pageable);

    // Check if username exists
    boolean existsByUsername(String username);

    // Find user by username
    UserModel findByUsername(String username);
    
}
