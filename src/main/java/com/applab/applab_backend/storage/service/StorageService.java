package com.applab.applab_backend.storage.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

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
    private static final int MAX_INITIAL_DIMENSION = 1200;
    private static final int MIN_DIMENSION = 5;

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
        BufferedImage sourceImage = ImageIO.read(new ByteArrayInputStream(originalBytes));
        if (sourceImage == null) {
            throw new RuntimeException("Unable to read image for compression");
        }

        int originalWidth = sourceImage.getWidth();
        int originalHeight = sourceImage.getHeight();
        double initialScale = Math.min(1.0,
                (double) MAX_INITIAL_DIMENSION / Math.max(originalWidth, originalHeight));
        int width = Math.max(MIN_DIMENSION, (int) Math.round(originalWidth * initialScale));
        int height = Math.max(MIN_DIMENSION, (int) Math.round(originalHeight * initialScale));
        byte[] bestOutput = null;

        while (true) {
            byte[] compressed = compressAtSize(originalBytes, width, height, maxBytes);
            if (compressed.length <= maxBytes) {
                return compressed;
            }

            if (bestOutput == null || compressed.length < bestOutput.length) {
                bestOutput = compressed;
            }

            if (width <= MIN_DIMENSION && height <= MIN_DIMENSION) {
                return bestOutput;
            }

            double dimensionScale = Math.sqrt((double) maxBytes / compressed.length);
            dimensionScale = Math.max(0.5, Math.min(0.95, dimensionScale));

            int nextWidth = Math.max(MIN_DIMENSION, (int) Math.floor(width * dimensionScale));
            int nextHeight = Math.max(MIN_DIMENSION, (int) Math.floor(height * dimensionScale));

            if (nextWidth == width && width > MIN_DIMENSION) {
                nextWidth = width - 1;
            }
            if (nextHeight == height && height > MIN_DIMENSION) {
                nextHeight = height - 1;
            }

            width = nextWidth;
            height = nextHeight;
        }  
    }

    private byte[] compressAtSize(byte[] originalBytes, int width, int height, long maxBytes) throws IOException {
        double quality = (double) maxBytes / originalBytes.length;
        quality = Math.max(0.1, Math.min(1.0, quality));
  
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        Thumbnails.of(new java.io.ByteArrayInputStream(originalBytes))
                .size(width, height)
                .outputFormat("jpeg")
                .outputQuality(quality)
                .toOutputStream(os);

        return os.toByteArray();
    }
}
