package com.applab.applab_backend.storage.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.applab.applab_backend.storage.model.FileEntityModel;
import com.applab.applab_backend.storage.repository.FileRepository;

import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final FileRepository fileRepository;

    public FileEntityModel storeFile(MultipartFile file, long maxFileSizeKb) {
        try {
            FileEntityModel fileEntity = new FileEntityModel();
            mapMultipartToEntity(file, maxFileSizeKb, fileEntity);
            return fileRepository.save(fileEntity);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public FileEntityModel storeImage(MultipartFile image, long maxFileSizeKb) {
        validateImage(image);
        return storeFile(image, maxFileSizeKb);
    }

    public List<FileEntityModel> storeFiles(MultipartFile[] files, long maxFileSizeKb) {
        if (files == null || files.length == 0) {
            throw new RuntimeException("No files provided");
        }
        List<FileEntityModel> savedFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            FileEntityModel saved = storeFile(file, maxFileSizeKb);
            savedFiles.add(saved);
        }
        return savedFiles;
    }

    public FileEntityModel updateFile(Long id, MultipartFile file, long maxFileSizeKb) {
        try {
            FileEntityModel existing = fileRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("File not found"));
            mapMultipartToEntity(file, maxFileSizeKb, existing);
            return fileRepository.save(existing);
        } catch (IOException e) {
            throw new RuntimeException("Failed to update file", e);
        }
    }

    public FileEntityModel updateImage(Long id, MultipartFile image, long maxFileSizeKb) {
        validateImage(image);
        return updateFile(id, image, maxFileSizeKb);
    }

    public void deleteFile(Long id) {
        FileEntityModel existing = fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));
        fileRepository.delete(existing);
    }

    private byte[] validateFile(MultipartFile file, long maxFileSizeKb) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        long maxBytes = maxFileSizeKb * 1024;
        if (file.getSize() <= maxBytes) {
            return file.getBytes();
        }

        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("image/")) {
            long targetKB = Math.max(1, maxBytes / 1024);
            byte[] compressed = compressImage(file, targetKB);
            if (compressed.length > maxBytes) {
                long compressedSizeKb = compressed.length / 1024;
                throw new RuntimeException("Image size exceeds limit of " + maxFileSizeKb
                        + " KB even after compression. Compressed size: " + compressedSizeKb + " KB");
            }
            return compressed;
        }

        throw new RuntimeException("File size exceeds limit of " + maxFileSizeKb + " KB");
    }

    private void validateImage(MultipartFile image) {
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Uploaded file is not an image");
        }
    }

    private void mapMultipartToEntity(MultipartFile file, long maxFileSizeKb, FileEntityModel target)
            throws IOException {
        byte[] data = validateFile(file, maxFileSizeKb);
        target.setFileName(UUID.randomUUID().toString());
        target.setFileType(file.getContentType());
        target.setData(data);
    }

    public byte[] compressImage(MultipartFile file, long targetKB) throws IOException {

        long maxBytes = targetKB * 1024;

        // If already small → return directly
        if (file.getSize() <= maxBytes) {
            return file.getBytes();
        }

        byte[] originalBytes = file.getBytes();

        int width = 1200;
        int height = 1200;

        byte[] bestOutput = null;

        double low = 0.1;
        double high = 1.0;

        // Binary search for best quality
        for (int i = 0; i < 10; i++) { // 10 iterations is enough
            double mid = (low + high) / 2;

            ByteArrayOutputStream os = new ByteArrayOutputStream();

            Thumbnails.of(new java.io.ByteArrayInputStream(originalBytes))
                    .size(width, height)
                    .outputFormat("jpg") // outputFormat("jpg")
                    .outputQuality(mid)
                    .toOutputStream(os);

            byte[] compressed = os.toByteArray();

            if (compressed.length > maxBytes) {
                // Too big → reduce quality
                high = mid;
            } else {
                // Acceptable → try higher quality
                bestOutput = compressed;
                low = mid;
            }
        }

        // ❗ If still null → fallback (extreme case)
        if (bestOutput == null) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            Thumbnails.of(new java.io.ByteArrayInputStream(originalBytes))
                    .size(800, 800) // 🔻 force smaller resolution
                    .outputFormat("jpg")
                    .outputQuality(0.3)
                    .toOutputStream(os);

            bestOutput = os.toByteArray();
        }

        return bestOutput;
    }
}
