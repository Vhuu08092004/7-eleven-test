package com.seveneleven.controller.upload;

import com.seveneleven.dto.BaseResponse;
import com.seveneleven.entity.TempFileMetadata;
import com.seveneleven.service.tempfile.TempFileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final TempFileService tempFileService;

    /**
     * Upload a temporary image file. The file is stored for a limited TTL (default 5 minutes)
     * and will be auto-deleted unless explicitly promoted via product save.
     */
    @PostMapping(value = "/temp", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Map<String, Object>>> uploadTempImage(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        String uploaderIp = getClientIp(httpRequest);
        Long ownerId = resolveOwnerId(userDetails);

        TempFileMetadata tempFile = tempFileService.storeTempFile(file, uploaderIp, ownerId);

        Map<String, Object> data = new HashMap<>();
        data.put("fileKey", tempFile.getFileKey());
        data.put("filename", tempFile.getStoredFilename());
        data.put("originalFilename", tempFile.getOriginalFilename());
        data.put("fileSize", tempFile.getFileSize());
        data.put("expiresAt", tempFile.getExpiresAt().toString());

        log.info("Temp image uploaded: fileKey={}, uploaderIp={}, ownerId={}, expiresAt={}",
                tempFile.getFileKey(), uploaderIp, ownerId, tempFile.getExpiresAt());

        return ResponseEntity.ok(BaseResponse.success("File uploaded temporarily", data));
    }

    /**
     * Delete a specific temp file immediately (when user removes it from the form).
     */
    @DeleteMapping("/temp/{fileKey}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> deleteTempImage(
            @PathVariable String fileKey,
            HttpServletRequest httpRequest) {

        tempFileService.deleteTempFile(fileKey);
        log.info("Temp image deleted: fileKey={}, uploaderIp={}", fileKey, getClientIp(httpRequest));

        return ResponseEntity.ok(BaseResponse.success("Temp file deleted", null));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private Long resolveOwnerId(UserDetails userDetails) {
        if (userDetails == null) return null;
        try {
            return ((com.seveneleven.security.CustomUserDetails) userDetails).getUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
