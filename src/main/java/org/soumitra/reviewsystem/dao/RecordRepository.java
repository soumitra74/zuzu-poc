package org.soumitra.reviewsystem.dao;

import org.soumitra.reviewsystem.model.Record;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecordRepository extends JpaRepository<Record, Integer> {
    
    /**
     * Log a record with status and error message
     */
    @Modifying
    @Transactional
    @Query("INSERT INTO Record (s3File.id, jobRun.id, rawData, status, startedAt, finishedAt, errorFlag) " +
           "VALUES (:s3FileId, :jobRunId, :rawData, :status, :startedAt, :finishedAt, :errorFlag)")
    void logRecord(@Param("s3FileId") Integer s3FileId, 
                   @Param("jobRunId") Integer jobRunId,
                   @Param("rawData") String rawData,
                   @Param("status") String status,
                   @Param("startedAt") LocalDateTime startedAt,
                   @Param("finishedAt") LocalDateTime finishedAt,
                   @Param("errorFlag") Boolean errorFlag);
    
    /**
     * Simplified log method for JobRunner
     */
    default void logNewRecord(Integer s3FileId, Integer jobId, String jsonLine) {
        Record record = new Record();
        
        // Create S3File reference
        org.soumitra.reviewsystem.model.S3File s3File = new org.soumitra.reviewsystem.model.S3File();
        s3File.setId(s3FileId);
        record.setS3File(s3File);
        
        // Create JobRun reference
        org.soumitra.reviewsystem.model.JobRun jobRun = new org.soumitra.reviewsystem.model.JobRun();
        jobRun.setId(jobId);
        record.setJobRun(jobRun);
        
        record.setRawData(jsonLine);
        record.setStatus("new");
        record.setProcessedAt(LocalDateTime.now());
        
        save(record);        
    }
    
    /**
     * Find new records with pagination
     */
    @Query("SELECT r FROM Record r WHERE r.status = 'new' ORDER BY r.id ASC")
    List<Record> findNewRecords(Pageable pageable);
    
    /**
     * Find new records with limit (simplified method)
     */
    default List<Record> findNewRecords(int limit) {
        return findNewRecords(org.springframework.data.domain.PageRequest.of(0, limit));
    }
    
    /**
     * Update record status
     */
    @Modifying
    @Transactional
    @Query("UPDATE Record r SET r.status = :status, r.processedAt = :processedAt WHERE r.id = :recordId")
    void updateRecordStatus(@Param("recordId") Integer recordId, @Param("status") String status, @Param("processedAt") LocalDateTime processedAt);
    
    /**
     * Update record status (simplified method)
     */
    default void updateRecordStatus(Integer recordId, String status) {
        updateRecordStatus(recordId, status, LocalDateTime.now());
    }
    
    /**
     * Update record status with error message
     */
    @Modifying
    @Transactional
    @Query("UPDATE Record r SET r.status = :status, r.processedAt = :processedAt, r.errorFlag = true WHERE r.id = :recordId")
    void updateRecordStatusWithError(@Param("recordId") Integer recordId, @Param("status") String status, @Param("processedAt") LocalDateTime processedAt);
    
    /**
     * Update record status with error message (simplified method)
     */
    default void updateRecordStatus(Integer recordId, String status, String errorMessage) {
        updateRecordStatusWithError(recordId, status, LocalDateTime.now());
    }
    
    /**
     * Find records by S3 file ID
     */
    List<Record> findByS3FileId(Integer s3FileId);
    
    /**
     * Find records by job run ID
     */
    List<Record> findByJobRunId(Integer jobRunId);
    
    /**
     * Find records by status
     */
    List<Record> findByStatus(String status);
    
    /**
     * Count records by S3 file ID and status
     */
    @Query("SELECT COUNT(r) FROM Record r WHERE r.s3File.id = :s3FileId AND r.status = :status")
    long countByS3FileIdAndStatus(@Param("s3FileId") Integer s3FileId, @Param("status") String status);
} 