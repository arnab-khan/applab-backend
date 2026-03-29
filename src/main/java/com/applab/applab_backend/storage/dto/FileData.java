package com.applab.applab_backend.storage.dto;

public record FileData(
                String fileName,
                String fileType,
                byte[] fileData) {
}