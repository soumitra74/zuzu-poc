package org.soumitra.reviewsystem.dao;

import org.soumitra.reviewsystem.model.RecordError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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
     * Find errors by record ID
     */
    List<RecordError> findByRecordId(Integer recordId);
    
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