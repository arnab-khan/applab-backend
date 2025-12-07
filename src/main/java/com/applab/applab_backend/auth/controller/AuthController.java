package com.applab.applab_backend.auth.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.applab.applab_backend.auth.dto.LoginRequest;
import com.applab.applab_backend.auth.dto.SignupRequest;
import com.applab.applab_backend.auth.model.UserModel;
import com.applab.applab_backend.auth.service.UserService;
import com.applab.applab_backend.common.views.SerializationJsonViews;
import com.fasterxml.jackson.annotation.JsonView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // Endpoint to handle user signup
    @PostMapping("/signup")
    @JsonView(SerializationJsonViews.MyClass.class)
    public UserModel addUser(@Valid @RequestBody SignupRequest userDetails, HttpServletRequest request) {
        return userService.createUser(userDetails, request);
    }

    // Endpoint to handle user login
    @PostMapping("/login")
    @JsonView(SerializationJsonViews.MyClass.class)
    public UserModel loginUser(@Valid @RequestBody LoginRequest loginDetails, HttpServletRequest request) {
        return userService.loginUser(loginDetails, request);
    }
}