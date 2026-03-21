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

    private static final double COMPRESSED_IMAGE_SIZE_TOLERANCE = 1.25;

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

    public FileEntityModel storeImage(MultipartFile image, long maxFileSizeKb, long compressedMaxFileSizeKb) {
        validateImage(image);
        try {
            FileEntityModel fileEntity = new FileEntityModel();
            mapMultipartToEntity(image, maxFileSizeKb, compressedMaxFileSizeKb, fileEntity);
            return fileRepository.save(fileEntity);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store image", e);
        }
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

    public FileEntityModel updateImage(Long id, MultipartFile image, long maxFileSizeKb, long compressedMaxFileSizeKb) {
        validateImage(image);
        try {
            FileEntityModel existing = fileRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("File not found"));
            mapMultipartToEntity(image, maxFileSizeKb, compressedMaxFileSizeKb, existing);
            return fileRepository.save(existing);
        } catch (IOException e) {
            throw new RuntimeException("Failed to update image", e);
        }
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
            long allowedCompressedBytes = Math.round(maxBytes * COMPRESSED_IMAGE_SIZE_TOLERANCE);
            if (compressed.length > allowedCompressedBytes) {
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
        mapMultipartToEntity(file, maxFileSizeKb, null, target);
    }

    private void mapMultipartToEntity(MultipartFile file, long maxFileSizeKb, Long compressedMaxFileSizeKb,
            FileEntityModel target) throws IOException {
        byte[] data = validateFile(file, maxFileSizeKb);
        target.setFileName(UUID.randomUUID().toString());
        target.setFileType(file.getContentType());
        target.setData(data);
        target.setCompressedData(
                compressedMaxFileSizeKb == null ? null : compressImage(file, compressedMaxFileSizeKb));
    }

    public byte[] compressImage(MultipartFile file, long targetKB) throws IOException {
        long maxBytes = targetKB * 1024;

        if (file.getSize() <= maxBytes) {
            return file.getBytes();
        }

        byte[] originalBytes = file.getBytes();
        byte[] bestOutput = null;
        int[][] dimensionAttempts = {
                { 1200, 1200 },
                { 1000, 1000 },
                { 800, 800 },
                { 600, 600 },
                { 400, 400 },
                { 200, 200 },
                { 100, 100 },
                { 50, 50 },
                { 25, 25 },
                { 10, 10 },
                { 5, 5 }
        };

        for (int[] dimensions : dimensionAttempts) {
            byte[] compressed = compressAtSize(originalBytes, dimensions[0], dimensions[1], maxBytes);
            if (compressed.length <= maxBytes) {
                return compressed;
            }

            if (bestOutput == null || compressed.length < bestOutput.length) {
                bestOutput = compressed;
            }
        }

        return bestOutput;
    }

    private byte[] compressAtSize(byte[] originalBytes, int width, int height, long maxBytes) throws IOException {
        byte[] bestOutput = null;

        double low = 0.1;
        double high = 1.0;

        for (int i = 0; i < 10; i++) {
            double mid = (low + high) / 2;
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            Thumbnails.of(new java.io.ByteArrayInputStream(originalBytes))
                    .size(width, height)
                    .outputFormat("jpeg")
                    .outputQuality(mid)
                    .toOutputStream(os);

            byte[] compressed = os.toByteArray();
            if (bestOutput == null || compressed.length < bestOutput.length) {
                bestOutput = compressed;
            }

            if (compressed.length > maxBytes) {
                high = mid;
            } else {
                bestOutput = compressed;
                low = mid;
            }
        }

        return bestOutput;
    }
}
