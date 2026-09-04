package com.applab.applab_backend.auth.dto;

import java.time.Instant;

public record PasswordResetTokenResponse(
        String resetToken,
        Instant expiresAt,
        long expiresInSeconds) {
}
