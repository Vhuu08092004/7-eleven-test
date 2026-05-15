package com.seveneleven.controller.upload;

import com.seveneleven.dto.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".webp"};
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<String>> uploadProductImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(BaseResponse.<String>error("File is empty. Please select a file to upload."));
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest()
                    .body(BaseResponse.<String>error("File size exceeds 5MB limit. Please choose a smaller image."));
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(BaseResponse.<String>error("Invalid file name."));
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!isAllowedExtension(extension)) {
            return ResponseEntity.badRequest()
                    .body(BaseResponse.<String>error("Only JPG, JPEG, PNG, GIF, and WEBP files are allowed."));
        }

        try {
            String newFilename = UUID.randomUUID() + extension;
            Path uploadPath = Paths.get(uploadDir, "products");
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath);

            String imageUrl = "/uploads/products/" + newFilename;
            log.info("File uploaded successfully: {} (original: {}, size: {} bytes)",
                    imageUrl, originalFilename, file.getSize());

            return ResponseEntity.ok(BaseResponse.<String>success("File uploaded successfully", imageUrl));
        } catch (IOException e) {
            log.error("Failed to upload file: {}", originalFilename, e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.<String>error("Failed to upload file. Please try again."));
        }
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1) {
            return "";
        }
        return filename.substring(dotIndex);
    }

    private boolean isAllowedExtension(String extension) {
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.equals(extension)) {
                return true;
            }
        }
        return false;
    }
}
