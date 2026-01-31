package com.applab.applab_backend.auth.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.applab.applab_backend.auth.dto.LoginRequest;
import com.applab.applab_backend.auth.dto.SignupRequest;
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
    public UserModel createUser(SignupRequest userDetails, HttpServletRequest request) {
        UserModel user = new UserModel();
        user.setName(userDetails.getName());
        user.setUsername(userDetails.getUsername());
        user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        userRepository.save(user);
        return apiResponse(user, request);
    }

    public UserModel loginUser(LoginRequest loginDetails, HttpServletRequest request) {

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

    private UserModel apiResponse(UserModel user, HttpServletRequest request) {

        HttpSession existingSession = request.getSession(false); // Get existing session if exists
        if (existingSession != null) {
            existingSession.invalidate(); // Invalidate existing session
        }
        // Generating session
        HttpSession session = request.getSession(true);
        session.setAttribute("userId", user.getId()); // Store the logged-in user's ID in the session

        // Create Authentication object for Spring Security
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getId(), // Use user ID or username (not full object)
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        SecurityContextHolder.getContext().setAuthentication(auth);
        session.setAttribute("securityContext", SecurityContextHolder.getContext());

        // Return success response with user details and session info
        return user;
    }

    public boolean isUsernameExist(String username) {
        return userRepository.existsByUsername(username);
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