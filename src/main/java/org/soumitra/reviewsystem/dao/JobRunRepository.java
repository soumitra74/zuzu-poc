package org.soumitra.reviewsystem.dao;

import org.soumitra.reviewsystem.model.JobRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobRunRepository extends JpaRepository<JobRun, Integer> {
    
    /**
     * Insert a new job run
     */
    default Integer insertJob(LocalDateTime scheduledAt, String triggerType, String status, String notes) {
        JobRun jobRun = new JobRun();
        jobRun.setScheduledAt(scheduledAt);
        jobRun.setStatus(status);
        jobRun.setNotes(notes);
        
        JobRun saved = save(jobRun);
        return saved.getId();
    }
    
    /**
     * Update job status
     */
    @Modifying
    @Transactional
    @Query("UPDATE JobRun j SET j.finishedAt = :finishedAt, j.status = :status WHERE j.id = :jobId")
    void updateJobStatus(@Param("jobId") Integer jobId, 
                        @Param("finishedAt") LocalDateTime finishedAt, 
                        @Param("status") String status);
    
    /**
     * Find jobs by status
     */
    List<JobRun> findByStatus(String status);
    
    /**
     * Find jobs by scheduled date range
     */
    @Query("SELECT j FROM JobRun j WHERE j.scheduledAt BETWEEN :startDate AND :endDate")
    List<JobRun> findByScheduledAtBetween(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate);
} 