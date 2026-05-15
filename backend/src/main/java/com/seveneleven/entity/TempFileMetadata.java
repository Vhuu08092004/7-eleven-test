package com.seveneleven.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "temp_file_metadata")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TempFileMetadata extends BaseEntity {

    @Column(name = "file_key", nullable = false, unique = true, length = 64)
    private String fileKey;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false)
    private String storedFilename;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "uploader_ip", length = 45)
    private String uploaderIp;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "is_promoted", nullable = false)
    @Builder.Default
    private Boolean isPromoted = false;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isPromoted() {
        return Boolean.TRUE.equals(isPromoted);
    }
}
