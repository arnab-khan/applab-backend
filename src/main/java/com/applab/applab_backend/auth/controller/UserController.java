package com.applab.applab_backend.auth.controller;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.DeleteMapping;

import com.applab.applab_backend.auth.dto.ProfileCredentialsUpdateRequest;
import com.applab.applab_backend.auth.dto.ProfileBasicsUpdateRequest;
import com.applab.applab_backend.auth.model.UserModel;
import com.applab.applab_backend.auth.service.UserService;
import com.applab.applab_backend.auth.dto.UserProfileImageResponse;
import com.applab.applab_backend.common.views.SerializationJsonViews;
import com.fasterxml.jackson.annotation.JsonView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

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

    // Endpoint to handle profile basics update
    @PatchMapping("/update-profile-basics")
    @JsonView(SerializationJsonViews.MyClass.class)
    public UserModel updateProfileBasics(@Valid @RequestBody ProfileBasicsUpdateRequest userDetails,
            HttpServletRequest request) {
        return userService.updateProfileBasics(userDetails, request);
    }

    @PatchMapping("/update-credentials")
    @JsonView(SerializationJsonViews.MyClass.class)
    public UserModel updateCredentials(@Valid @RequestBody ProfileCredentialsUpdateRequest userDetails,
            HttpServletRequest request) {
        return userService.updateCredentials(userDetails, request);
    }

    // check if username is taken
    @GetMapping("/public/is-username-exist")
    @JsonView(SerializationJsonViews.MyClass.class)
    public Map<String, Boolean> isUsernameExist(@RequestParam String username) {
        boolean exists = userService.isUsernameExist(username);
        return Map.of("exists", exists);
    }

    // Endpoint to handle profile image upload
    @PatchMapping("/update-profile-image")
    public UserProfileImageResponse updateProfileImage(@RequestBody MultipartFile profileImage, HttpServletRequest request) {
        return userService.updateProfileImage(profileImage, request);
    }

    // Endpoint to get profile image
    @GetMapping("/profile-image")
    public UserProfileImageResponse getProfileImage(
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") boolean fullImage) {
        return userService.getProfileImage(request, fullImage);
    }

    // Endpoint to delete profile image
    @DeleteMapping("/profile-image")
    public void deleteProfileImage(HttpServletRequest request) {
        userService.deleteProfileImage(request);
    }
}
