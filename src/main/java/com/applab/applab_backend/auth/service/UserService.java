package com.applab.applab_backend.auth.service;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.applab.applab_backend.auth.model.UserModel;
import com.applab.applab_backend.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public UserService(PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    // Hashes the password and then saves the user in the database
    public Map<String, Object> createUser(UserModel user, HttpServletRequest request) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return apiResponse(user, request);
    }

    public Map<String, Object> loginUser(UserModel loginDetails, HttpServletRequest request) {

        String username = loginDetails.getUsername();
        String rawPassword = loginDetails.getPassword();

        // Find user by username
        UserModel user = userRepository.findByUsername(username);
        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found");
        }

        // Validate password
        boolean passwordMatches = passwordEncoder.matches(rawPassword, user.getPassword());
        if (!passwordMatches) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Incorrect password");
        }

        // Successful login, create session and return response
        return apiResponse(user, request);
    }

    private Map<String, Object> apiResponse(UserModel user, HttpServletRequest request) {
        // Invalidate existing session for security
        // Without this same session ID for different users if same browser/client
        // Sessions are tied to the browser, not the user
        // The session gets overwritten with the new user's data
        // So to fix that, we invalidate any existing session first and create a new one
        HttpSession existingSession = request.getSession(false); // Get existing session if exists
        if (existingSession != null) {
            existingSession.invalidate(); // Invalidate existing session
        }

        // Generating session
        HttpSession session = request.getSession(true); // Create new session object. If Spring Session with Redis is configured, the session will be automatically persisted to Redis. When we create it also content getId() in it
        session.setAttribute("userId", user.getId()); // Store the logged-in user's ID in the session
        session.setAttribute("roles", List.of("ROLE_USER")); // Store the user's roles in the session

        // Create Authentication object for Spring Security
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getId(), // Use user ID or username (not full object)
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        
        // SecurityContextHolder is a Spring Security class that holds the security context for the current thread.
        // getContext() returns the current SecurityContext, which is basically a container for the current authenticated user.
        // setAuthentication(auth) sets the Authentication object representing the logged-in user in the current thread.
        // and telling Spring Security that this request is authenticated as this user"
        SecurityContextHolder.getContext().setAuthentication(auth);
        session.setAttribute("securityContext", SecurityContextHolder.getContext()); // Store the current SecurityContext in the session under a custom key "securityContext". This will be used later by the HeaderSessionAuthFilter.java to restore authentication

        // Return success response with user details and session info
        return Map.of(
                "message", "Successful",
                "user", user,
                "roles", session.getAttribute("roles"),
                "sessionId", session.getId());
    }

    public UserModel getUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    public UserModel getUserBySession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return null;
        }
        return getUserById(userId);
    }

    @Transactional
    public UserModel updateUser(UserModel updatedDetails, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }

        UserModel existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Update only provided fields
        if (updatedDetails.getUsername() != null && !updatedDetails.getUsername().trim().isEmpty()) {
            if (!existingUser.getUsername().equals(updatedDetails.getUsername())
                    && userRepository.existsByUsername(updatedDetails.getUsername())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already taken");
            }
            existingUser.setUsername(updatedDetails.getUsername());
        }

        if (updatedDetails.getName() != null && !updatedDetails.getName().trim().isEmpty()) {
            existingUser.setName(updatedDetails.getName());
        }

        return userRepository.save(existingUser);
    }

}