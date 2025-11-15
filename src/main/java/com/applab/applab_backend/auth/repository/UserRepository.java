package com.applab.applab_backend.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.applab.applab_backend.auth.model.UserModel;

public interface UserRepository extends JpaRepository<UserModel, Long> {

    // Check if username exists (optional)
    boolean existsByUsername(String username);

    // Find user by username
    UserModel findByUsername(String username);
    
}
