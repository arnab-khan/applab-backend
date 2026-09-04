package com.applab.applab_backend.email.dto;

import java.time.Instant;

public record EmailOtpResponse(
        String message,
        String requestId,
        String sentTo,
        Instant expiresAt,
        long expiresInSeconds,
        int otpDigits,
        long resendCooldownSeconds,
        Instant resendAvailableAt,
        int remainingResends) {
}
