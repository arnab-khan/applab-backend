package com.applab.applab_backend.storage.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.applab.applab_backend.storage.model.FileEntityModel;
import com.applab.applab_backend.storage.repository.FileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final FileRepository fileRepository;

    public FileEntityModel storeFile(MultipartFile file, long maxFileSizeMb) {
        try {
            FileEntityModel fileEntity = new FileEntityModel();
            mapMultipartToEntity(file, maxFileSizeMb, fileEntity);
            return fileRepository.save(fileEntity);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public FileEntityModel storeImage(MultipartFile image, long maxFileSizeMb) {
        validateImage(image);
        return storeFile(image, maxFileSizeMb);
    }

    public List<FileEntityModel> storeFiles(MultipartFile[] files, long maxFileSizeMb) {
        if (files == null || files.length == 0) {
            throw new RuntimeException("No files provided");
        }
        List<FileEntityModel> savedFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            FileEntityModel saved = storeFile(file, maxFileSizeMb);
            savedFiles.add(saved);
        }
        return savedFiles;
    }

    public FileEntityModel updateFile(Long id, MultipartFile file, long maxFileSizeMb) {
        try {
            FileEntityModel existing = fileRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("File not found"));
            mapMultipartToEntity(file, maxFileSizeMb, existing);
            return fileRepository.save(existing);
        } catch (IOException e) {
            throw new RuntimeException("Failed to update file", e);
        }
    }

    public FileEntityModel updateImage(Long id, MultipartFile image, long maxFileSizeMb) {
        validateImage(image);
        return updateFile(id, image, maxFileSizeMb);
    }

    public void deleteFile(Long id) {
        FileEntityModel existing = fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));
        fileRepository.delete(existing);
    }

    private void validateFile(MultipartFile file, long maxFileSizeMb) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }
        if (file.getSize() > maxFileSizeMb * 1024 * 1024) {
            throw new RuntimeException(
                    "File size exceeds limit of " + maxFileSizeMb + " MB");
        }
    }

    private void validateImage(MultipartFile image) {
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Uploaded file is not an image");
        }
    }

    private void mapMultipartToEntity(MultipartFile file, long maxFileSizeMb, FileEntityModel target)
            throws IOException {
        validateFile(file, maxFileSizeMb);
        target.setFileName(UUID.randomUUID().toString());
        target.setFileType(file.getContentType());
        target.setData(file.getBytes());
    }
}