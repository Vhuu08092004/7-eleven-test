package com.seveneleven.repository;

import com.seveneleven.entity.TempFileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TempFileRepository extends JpaRepository<TempFileMetadata, Long> {

    Optional<TempFileMetadata> findByFileKeyAndIsDeletedFalse(String fileKey);

    @Query("SELECT t FROM TempFileMetadata t WHERE t.expiresAt < :now AND t.isDeleted = false")
    List<TempFileMetadata> findAllExpired(@Param("now") Instant now);

    @Query("SELECT t FROM TempFileMetadata t WHERE t.expiresAt < :now AND t.isDeleted = false AND t.isPromoted = false")
    List<TempFileMetadata> findAllExpiredNotPromoted(@Param("now") Instant now);

    @Modifying
    @Query("UPDATE TempFileMetadata t SET t.isDeleted = true WHERE t.expiresAt < :now AND t.isDeleted = false")
    int softDeleteExpired(@Param("now") Instant now);

    List<TempFileMetadata> findByOwnerIdAndIsDeletedFalse(Long ownerId);

    @Modifying
    @Query("UPDATE TempFileMetadata t SET t.isPromoted = true WHERE t.fileKey = :fileKey")
    void markAsPromoted(@Param("fileKey") String fileKey);
}
