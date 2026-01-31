package com.applab.applab_backend.auth.controller;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.applab.applab_backend.auth.model.UserModel;
import com.applab.applab_backend.auth.service.UserService;
import com.applab.applab_backend.common.views.SerializationJsonViews;
import com.fasterxml.jackson.annotation.JsonView;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Endpoint to handle update user
    @PatchMapping("/update")
    @JsonView(SerializationJsonViews.MyClass.class)
    public UserModel updateUser(@RequestBody UserModel userDetails, HttpServletRequest request) {
        return userService.updateUser(userDetails, request);
    }

    // check if username is taken
    @GetMapping("/public/is-username-exist")
    @JsonView(SerializationJsonViews.MyClass.class)
    public Map<String, Boolean> isUsernameExist(@RequestParam String username) {
        boolean exists = userService.isUsernameExist(username);
        return Map.of("exists", exists);
    }

}
