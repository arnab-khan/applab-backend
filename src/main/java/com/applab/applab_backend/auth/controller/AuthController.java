package com.applab.applab_backend.auth.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.applab.applab_backend.auth.model.UserModel;
import com.applab.applab_backend.auth.repository.UserRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserRepository userRepository;

    public AuthController(UserRepository myRepository) {
        this.userRepository = myRepository;
    }

    @PostMapping("/signup")
    public UserModel addUser(@Valid @RequestBody UserModel userDetails) {
        return userRepository.save(userDetails);
    }
}