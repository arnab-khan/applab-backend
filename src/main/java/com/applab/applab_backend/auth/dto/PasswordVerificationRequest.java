package com.applab.applab_backend.auth.dto;

import com.applab.applab_backend.auth.enums.PasswordVerificationPurpose;
import com.applab.applab_backend.auth.validation.UserValidation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PasswordVerificationRequest implements UserValidation.CurrentPasswordValidation {
    private String currentPassword;

    @NotNull(message = "Password verification purpose is required")
    private PasswordVerificationPurpose purpose;
}
