package com.applab.applab_backend.auth.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.applab.applab_backend.auth.model.UserModel;
import com.applab.applab_backend.auth.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService; // injects UserService via constructor
    }

    // Endpoint to handle user signup
    @PostMapping("/signup")
    public UserModel addUser(@Valid @RequestBody UserModel userDetails) {
        return userService.createUser(userDetails); // Call service to hash password & save user in database
    }

    // Endpoint to handle user login
    @PostMapping("/login")
    public UserModel loginUser(@RequestBody UserModel loginDetails) {
        return userService.loginUser(loginDetails.getUsername(), loginDetails.getPassword()); // Call service to login user
    }
}