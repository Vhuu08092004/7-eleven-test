package com.seveneleven.service.tempfile;

import com.seveneleven.entity.TempFileMetadata;
import com.seveneleven.exception.BadRequestException;
import com.seveneleven.exception.ResourceNotFoundException;
import com.seveneleven.repository.TempFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TempFileService {

    private final TempFileRepository tempFileRepository;

    @Value("${app.upload.temp-dir:uploads/temp}")
    private String tempUploadDir;

    @Value("${app.upload.temp-ttl-minutes:5}")
    private int tempTtlMinutes;

    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".webp"};
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * Store a file temporarily with a unique key. The file is saved to the temp directory
     * and metadata is recorded in the database. After TTL expires, the file will be
     * cleaned up unless explicitly promoted.
     *
     * @param file       the uploaded file
     * @param uploaderIp IP address of the uploader
     * @param ownerId    ID of the user who uploaded (can be null for anonymous)
     * @return TempFileMetadata containing the file key and metadata
     */
    @Transactional
    public TempFileMetadata storeTempFile(MultipartFile file, String uploaderIp, Long ownerId) {
        validateFile(file);

        String fileKey = generateFileKey(file);
        String extension = getFileExtension(file.getOriginalFilename()).toLowerCase();
        String storedFilename = fileKey + extension;

        Path tempDir = Paths.get(tempUploadDir);
        Path destPath = tempDir.resolve(storedFilename);

        try {
            Files.createDirectories(tempDir);
            Files.copy(file.getInputStream(), destPath);

            Instant expiresAt = Instant.now().plus(tempTtlMinutes, ChronoUnit.MINUTES);

            TempFileMetadata metadata = TempFileMetadata.builder()
                    .fileKey(fileKey)
                    .originalFilename(file.getOriginalFilename())
                    .storedFilename(storedFilename)
                    .filePath(destPath.toString())
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .uploaderIp(uploaderIp)
                    .ownerId(ownerId)
                    .expiresAt(expiresAt)
                    .build();

            TempFileMetadata saved = tempFileRepository.save(metadata);
            log.info("Temp file stored: key={}, expiresAt={}, uploaderIp={}, ownerId={}",
                    fileKey, expiresAt, uploaderIp, ownerId);

            return saved;
        } catch (IOException e) {
            log.error("Failed to store temp file: {}", fileKey, e);
            throw new RuntimeException("Failed to store uploaded file", e);
        }
    }

    /**
     * Retrieve temp file metadata by its key. Returns null if the file is expired or deleted.
     */
    @Transactional(readOnly = true)
    public Optional<TempFileMetadata> getTempFile(String fileKey) {
        return tempFileRepository.findByFileKeyAndIsDeletedFalse(fileKey)
                .filter(meta -> !meta.isExpired());
    }

    /**
     * Retrieve temp file metadata by its key, throwing if not found or expired.
     */
    @Transactional(readOnly = true)
    public TempFileMetadata getTempFileOrThrow(String fileKey) {
        return getTempFile(fileKey)
                .orElseThrow(() -> new ResourceNotFoundException("Temp file not found: " + fileKey));
    }

    /**
     * Promote a temp file: mark it as permanent and move it to the final destination.
     * Called when a product (or any entity) is saved with this file.
     *
     * @param fileKey       the temp file key
     * @param finalPath     the final directory (e.g., "uploads/products")
     * @param finalFilename the desired final filename (e.g., UUID + extension)
     * @return the final relative URL path (e.g., "/uploads/products/uuid.jpg")
     */
    @Transactional
    public String promoteTempFile(String fileKey, String finalPath, String finalFilename) {
        TempFileMetadata metadata = getTempFileOrThrow(fileKey);

        if (metadata.isPromoted()) {
            throw new BadRequestException("File has already been promoted: " + fileKey);
        }

        Path source = Paths.get(metadata.getFilePath());
        Path targetDir = Paths.get(finalPath);
        Path target = targetDir.resolve(finalFilename);

        try {
            Files.createDirectories(targetDir);
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

            metadata.setFilePath(target.toString());
            metadata.setIsPromoted(true);
            tempFileRepository.save(metadata);

            log.info("Temp file promoted: {} -> {}", fileKey, target);
            return "/" + finalPath.replace("\\", "/") + "/" + finalFilename;
        } catch (IOException e) {
            log.error("Failed to promote temp file: {}", fileKey, e);
            throw new RuntimeException("Failed to promote file", e);
        }
    }

    /**
     * Delete a specific temp file immediately (e.g., when user removes an image
     * from the form before saving).
     */
    @Transactional
    public void deleteTempFile(String fileKey) {
        tempFileRepository.findByFileKeyAndIsDeletedFalse(fileKey)
                .ifPresent(metadata -> {
                    try {
                        Path path = Paths.get(metadata.getFilePath());
                        Files.deleteIfExists(path);
                        metadata.setIsDeleted(true);
                        tempFileRepository.save(metadata);
                        log.info("Temp file deleted: {}", fileKey);
                    } catch (IOException e) {
                        log.warn("Failed to delete temp file physical path: {}", fileKey, e);
                    }
                });
    }

    /**
     * Scheduled job to clean up expired temp files every 1 minute.
     * Only deletes files that have NOT been promoted (i.e., not yet saved to a product).
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupExpiredTempFiles() {
        List<TempFileMetadata> expired = tempFileRepository.findAllExpiredNotPromoted(Instant.now());

        if (expired.isEmpty()) {
            return;
        }

        int deletedCount = 0;
        for (TempFileMetadata metadata : expired) {
            try {
                Path path = Paths.get(metadata.getFilePath());
                Files.deleteIfExists(path);

                metadata.setIsDeleted(true);
                tempFileRepository.save(metadata);

                deletedCount++;
                log.debug("Cleaned up expired temp file: {} (uploaded by {}, expired {})",
                        metadata.getFileKey(), metadata.getUploaderIp(), metadata.getUploadedAt());
            } catch (IOException e) {
                log.warn("Failed to delete expired temp file: {} at {}",
                        metadata.getFileKey(), metadata.getFilePath(), e);
            }
        }

        log.info("Cleanup complete: {} expired temp files deleted", deletedCount);
    }

    // ---------- Private helpers ----------

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty. Please select a file to upload.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds 5MB limit.");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BadRequestException("Invalid file name.");
        }
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!isAllowedExtension(extension)) {
            throw new BadRequestException("Only JPG, JPEG, PNG, GIF, and WEBP files are allowed.");
        }
    }

    private String generateFileKey(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String input = file.getOriginalFilename() + "-" + file.getSize() + "-" + System.nanoTime();
            byte[] hash = md.digest(input.getBytes());
            return HexFormat.of().formatHex(hash).substring(0, 24);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1) return "";
        return filename.substring(dotIndex);
    }

    private boolean isAllowedExtension(String extension) {
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.equals(extension)) return true;
        }
        return false;
    }
}
