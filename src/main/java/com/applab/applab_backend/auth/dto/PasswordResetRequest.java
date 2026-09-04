package com.applab.applab_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetRequest {
    @NotBlank(message = "Reset token is required")
    private String resetToken;

    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters long and up to 100 characters")
    @Pattern(regexp = "^[^\\s]+$", message = "Password cannot contain spaces")
    private String newPassword;
}
