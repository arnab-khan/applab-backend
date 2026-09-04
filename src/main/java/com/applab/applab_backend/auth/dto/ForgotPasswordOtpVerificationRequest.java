package com.applab.applab_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ForgotPasswordOtpVerificationRequest {
    @NotBlank(message = "Request ID is required")
    private String requestId;

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP must contain 6 digits")
    private String otp;
}
