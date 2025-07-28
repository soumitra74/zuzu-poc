package org.soumitra.reviewsystem.dao;

import org.soumitra.reviewsystem.model.Record;
import org.soumitra.reviewsystem.model.RecordError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecordErrorRepository extends JpaRepository<RecordError, Integer> {
    
    /**
     * Log a record error
     */
    default void logRecordError(Integer recordId, String errorType, String errorMessage, String traceback) {
        RecordError recordError = new RecordError();
        
        // Create Record reference
        org.soumitra.reviewsystem.model.Record record = new org.soumitra.reviewsystem.model.Record();
        record.setId(recordId);
        recordError.setRecord(record);
        
        recordError.setRecordId(recordId);
        recordError.setErrorType(errorType);
        recordError.setErrorMessage(errorMessage);
        recordError.setTraceback(traceback);
        
        save(recordError);
    }
    
    /**
     * Simplified log method for processing errors
     */
    default void logRecordError(Integer recordId, String errorMessage) {
        logRecordError(recordId, "PROCESSING_ERROR", errorMessage, null);
    }
    
    /**
     * Log record error with Record object and upsert logic
     */
    default void logRecordError(Record record, String errorMessage) {
        logRecordError(record, errorMessage, null);
    }
    
    /**
     * Log record error with Record object, error message, and traceback
     */
    default void logRecordError(Record record, String errorMessage, String traceback) {
        // Check if error already exists for this record
        Optional<RecordError> existingError = findByRecordId(record.getId());
        
        if (existingError.isPresent()) {
            // Update existing error
            RecordError error = existingError.get();
            error.setErrorMessage(errorMessage);
            error.setTraceback(traceback);
            save(error);
        } else {
            // Create new error - only set the primary key, not the foreign key object
            RecordError recordError = new RecordError();
            recordError.setRecordId(record.getId());
            recordError.setErrorType("PROCESSING_ERROR");
            recordError.setErrorMessage(errorMessage);
            recordError.setTraceback(traceback);
            
            save(recordError);
        }
    }
    
    /**
     * Find error by record ID and error type
     */
    @Query("SELECT re FROM RecordError re WHERE re.recordId = :recordId AND re.errorType = :errorType")
    Optional<RecordError> findByRecordIdAndErrorType(@Param("recordId") Integer recordId, @Param("errorType") String errorType);
    
    /**
     * Find errors by record ID
     */
    Optional<RecordError> findByRecordId(Integer recordId);
    
    /**
     * Find errors by error type
     */
    List<RecordError> findByErrorType(String errorType);
    
    /**
     * Count errors by record ID
     */
    @Query("SELECT COUNT(re) FROM RecordError re WHERE re.recordId = :recordId")
    long countByRecordId(@Param("recordId") Integer recordId);
} 