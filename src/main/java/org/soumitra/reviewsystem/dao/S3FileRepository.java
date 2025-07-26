package org.soumitra.reviewsystem.dao;

import org.soumitra.reviewsystem.model.S3File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface S3FileRepository extends JpaRepository<S3File, Integer> {
    
    /**
     * Find by S3 key
     */
    Optional<S3File> findByS3Key(String s3Key);
    
    /**
     * Insert or update file
     */
    default Integer insertOrUpdateFile(Integer jobRunId, String bucket, String s3Key, String status, String errorMessage, boolean isNew) {
        Optional<S3File> existing = findByS3Key(s3Key);
        
        S3File s3File;
        if (existing.isPresent()) {
            s3File = existing.get();
            s3File.setStartedAt(LocalDateTime.now());
            s3File.setStatus(status);
            s3File.setErrorMessage(errorMessage);
        } else {
            s3File = new S3File();
            s3File.setJobRun(new org.soumitra.reviewsystem.model.JobRun() {{ setId(jobRunId); }});
            s3File.setS3Key(s3Key);
            s3File.setStartedAt(LocalDateTime.now());
            s3File.setStatus(status);
            s3File.setErrorMessage(errorMessage);
        }
        
        S3File saved = save(s3File);
        return saved.getId();
    }
    
    /**
     * Update file status
     */
    @Modifying
    @Transactional
    @Query("UPDATE S3File s SET s.finishedAt = :finishedAt, s.status = :status, s.errorMessage = :errorMessage, " +
           "s.recordCount = :recordCount WHERE s.id = :fileId")
    void updateFileStatus(@Param("fileId") Integer fileId,
                         @Param("finishedAt") LocalDateTime finishedAt,
                         @Param("status") String status,
                         @Param("errorMessage") String errorMessage,
                         @Param("recordCount") Integer recordCount);
    
    /**
     * Simplified update method for JobRunner
     */
    default void updateFileStatus(Integer fileId, String status, String errorMessage, Integer recordCount, boolean isNew) {
        updateFileStatus(fileId, LocalDateTime.now(), status, errorMessage, recordCount);
    }
    
    /**
     * Find files by job run ID
     */
    List<S3File> findByJobRunId(Integer jobRunId);
    
    /**
     * Find files by status
     */
    List<S3File> findByStatus(String status);
    
    /**
     * Find unprocessed files
     */
    @Query("SELECT s FROM S3File s WHERE s.status IN ('new', 'processing')")
    List<S3File> findUnprocessedFiles();
} 