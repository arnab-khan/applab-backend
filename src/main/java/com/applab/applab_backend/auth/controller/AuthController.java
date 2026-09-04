package com.applab.applab_backend.auth.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.applab.applab_backend.auth.dto.LoginRequest;
import com.applab.applab_backend.auth.dto.ForgotPasswordOtpRequest;
import com.applab.applab_backend.auth.dto.ForgotPasswordOtpVerificationRequest;
import com.applab.applab_backend.auth.dto.PasswordResetRequest;
import com.applab.applab_backend.auth.dto.PasswordResetTokenResponse;
import com.applab.applab_backend.auth.dto.SignupRequest;
import com.applab.applab_backend.auth.model.UserModel;
import com.applab.applab_backend.auth.service.UserService;
import com.applab.applab_backend.common.views.SerializationJsonViews;
import com.applab.applab_backend.email.dto.EmailOtpResponse;
import com.fasterxml.jackson.annotation.JsonView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

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

    @PostMapping("/forgot-password/send-otp")
    public EmailOtpResponse sendForgotPasswordOtp(
            @Valid @RequestBody ForgotPasswordOtpRequest request)
            throws com.resend.core.exception.ResendException {
        return userService.sendForgotPasswordOtp(request);
    }

    @PostMapping("/forgot-password/verify-otp")
    public PasswordResetTokenResponse verifyForgotPasswordOtp(
            @Valid @RequestBody ForgotPasswordOtpVerificationRequest request) {
        return userService.verifyForgotPasswordOtp(request);
    }

    @PostMapping("/forgot-password/reset")
    public java.util.Map<String, String> resetForgottenPassword(
            @Valid @RequestBody PasswordResetRequest request) {
        return userService.resetForgottenPassword(request);
    }

    // Me endpoint (auth check)
    @GetMapping("/me")
    @JsonView(SerializationJsonViews.MyClass.class)
    public UserModel getMe(HttpServletRequest request) {
        UserModel user = userService.getUserBySession(request);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        return user;
    }

    // Logout
    @PostMapping("/logout")
    public ResponseEntity<String> logoutUser(HttpServletRequest request) {
        if (request.getSession(false) != null) {
            request.getSession().invalidate(); // clear the session
        }
        return ResponseEntity.ok("Logged out successfully");
    }
}
