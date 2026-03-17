package com.applab.applab_backend.storage.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.applab.applab_backend.storage.model.FileEntityModel;
import com.applab.applab_backend.storage.service.StorageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
public class StorageController {
    private final StorageService storageService;

    @PostMapping("/public/upload-file")
    public ResponseEntity<FileEntityModel> uploadFile(
            @RequestParam MultipartFile file) {
        return ResponseEntity.ok(storageService.storeFile(file, 100));
    }

    @PostMapping("/public/upload-files")
    public ResponseEntity<List<FileEntityModel>> uploadFiles(
            @RequestParam MultipartFile[] files) {
        return ResponseEntity.ok(storageService.storeFiles(files, 100));
    }

    @PostMapping("/public/upload-image")
    public ResponseEntity<FileEntityModel> uploadImage(
            @RequestParam MultipartFile image) {
        return ResponseEntity.ok(storageService.storeImage(image, 100));
    }

    @PatchMapping("/public/update-file/{id}")
    public ResponseEntity<FileEntityModel> updateFile(
            @PathVariable Long id,
            @RequestParam MultipartFile file) {
        return ResponseEntity.ok(storageService.updateFile(id, file, 100));
    }

    @PatchMapping("/public/update-image/{id}")
    public ResponseEntity<FileEntityModel> updateImage(
            @PathVariable Long id,
            @RequestParam MultipartFile image) {
        return ResponseEntity.ok(storageService.updateImage(id, image, 100));
    }

    @DeleteMapping("/public/delete-file/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long id) {
        storageService.deleteFile(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
