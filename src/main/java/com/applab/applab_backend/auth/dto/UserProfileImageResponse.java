package com.applab.applab_backend.auth.dto;

public record UserProfileImageResponse(
        Long id,
        Long userId,
        String fileName,
        String fileType,
        byte[] fileData,
        byte[] compressedFileData) {
}
