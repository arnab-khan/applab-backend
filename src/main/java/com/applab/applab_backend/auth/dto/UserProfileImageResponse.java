package com.applab.applab_backend.auth.dto;

public record UserProfileImageResponse(
        Long userId,
        String fileName,
        String fileType,
        byte[] fileData) {
}
