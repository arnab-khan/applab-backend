package com.applab.applab_backend.auth.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.applab.applab_backend.auth.model.UserModel;
import com.applab.applab_backend.auth.repository.UserRepository;

@Service
public class UserService {

    private final PasswordEncoder passwordEncoder; // Used for hashing passwords
    private final UserRepository userRepository; // Used to save user to DB

    // Injects PasswordEncoder and UserRepository via Constructor
    public UserService(PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    // This method hashes the password and then saves the user in the database
    public UserModel createUser(UserModel user) {

        // Hash the plain password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Save the user to the database
        return userRepository.save(user);
    }

    public UserModel loginUser(String username, String rawPassword) {

        // 1. Find user by username
        UserModel user = userRepository.findByUsername(username);

        if (user == null) {
            // User not found → return 404 error
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found");
        }

        // 2. Validate password 
        boolean passwordMatches = passwordEncoder.matches(rawPassword, user.getPassword()); // Compare raw password with hashed password

        if (!passwordMatches) {
            // Wrong password → return 401 Unauthorized
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Incorrect password");
        }

        // 3. Login successful
        return user;
    }

}