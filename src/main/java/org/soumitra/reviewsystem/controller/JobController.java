package org.soumitra.reviewsystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.soumitra.reviewsystem.JobRunner;
import org.soumitra.reviewsystem.RecordProcessorJob;
import org.soumitra.reviewsystem.dao.*;
import org.soumitra.reviewsystem.model.JobRun;
import org.soumitra.reviewsystem.util.HotelReviewJsonParser;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "*")
public class JobController {

    @Autowired
    private JobRunRepository jobRunRepository;

    @Autowired
    private S3FileRepository s3FileRepository;

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private RecordErrorRepository recordErrorRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private ReviewerRepository reviewerRepository;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private HotelReviewJsonParser hotelReviewJsonParser;

    @Autowired
    private Environment environment;

    /**
     * Endpoint to run the JobRunner job
     * POST /api/jobs/run-s3-ingest
     */
    @PostMapping("/run-s3-ingest")
    public ResponseEntity<Map<String, Object>> runJobRunner(@RequestBody JobRunnerRequest request) {
        try {
            // s3Uri is optional. if missing take from application config
            String s3Uri = request.getS3Uri();
            if (s3Uri == null || s3Uri.trim().isEmpty()) {
                s3Uri = "s3://" + environment.getProperty("aws.s3.bucket");
            }

            // Create JobRunner instance
            JobRunner jobRunner = new JobRunner(
                jobRunRepository,
                s3FileRepository,
                recordRepository,
                s3Client,
                request.getBatchSize() != null ? request.getBatchSize() : 10
            );

            // Run the job
            jobRunner.runJob(s3Uri);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "JobRunner job completed successfully");
            response.put("s3Uri", s3Uri);
            response.put("batchSize", request.getBatchSize() != null ? request.getBatchSize() : 10);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to run JobRunner job: " + e.getMessage());
            errorResponse.put("exception", e.getClass().getSimpleName());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Endpoint to run the RecordProcessorJob
     * POST /api/jobs/run-record-processor
     */
    @PostMapping("/run-record-processor")
    public ResponseEntity<Map<String, Object>> runRecordProcessor(@RequestBody RecordProcessorRequest request) {
        try {
            // Create RecordProcessorJob instance
            RecordProcessorJob recordProcessorJob = new RecordProcessorJob(
                jobRunRepository,
                recordRepository,
                recordErrorRepository,
                reviewRepository,
                hotelRepository,
                providerRepository,
                reviewerRepository,
                hotelReviewJsonParser,
                request.getPageSize() != null ? request.getPageSize() : 10
            );

            // Run the job
            recordProcessorJob.runJob();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "RecordProcessorJob completed successfully");
            response.put("pageSize", request.getPageSize() != null ? request.getPageSize() : 10);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to run RecordProcessorJob: " + e.getMessage());
            errorResponse.put("exception", e.getClass().getSimpleName());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get job status endpoint
     * GET /api/jobs/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getJobStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ready");
        response.put("availableJobs", new String[]{"job-runner", "record-processor"});
        response.put("message", "Job controller is ready to process requests");
        
        return ResponseEntity.ok(response);
    }

    /**
     * List all jobs
     * GET /api/jobs
     */
    @GetMapping("")
    public ResponseEntity<Map<String, Object>> getAllJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        
        try {
            List<JobRun> jobs;
            
            if (status != null && !status.trim().isEmpty()) {
                // Filter by status
                jobs = jobRunRepository.findByStatus(status);
            } else {
                // Get all jobs with pagination
                Pageable pageable = PageRequest.of(page, size, Sort.by("scheduledAt").descending());
                Page<JobRun> jobPage = jobRunRepository.findAll(pageable);
                jobs = jobPage.getContent();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobs", jobs);
            response.put("totalJobs", jobs.size());
            response.put("page", page);
            response.put("size", size);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to retrieve jobs: " + e.getMessage());
            errorResponse.put("exception", e.getClass().getSimpleName());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get a specific job by ID
     * GET /api/jobs/{jobId}
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<Map<String, Object>> getJobById(@PathVariable Integer jobId) {
        
        try {
            Optional<JobRun> jobOptional = jobRunRepository.findById(jobId);
            
            if (jobOptional.isPresent()) {
                JobRun job = jobOptional.get();
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("job", job);
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Job not found with ID: " + jobId);
                
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to retrieve job: " + e.getMessage());
            errorResponse.put("exception", e.getClass().getSimpleName());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // Request DTOs
    public static class JobRunnerRequest {
        private String s3Uri;
        private Integer batchSize;

        // Getters and setters
        public String getS3Uri() {
            return s3Uri;
        }

        public void setS3Uri(String s3Uri) {
            this.s3Uri = s3Uri;
        }

        public Integer getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(Integer batchSize) {
            this.batchSize = batchSize;
        }
    }

    public static class RecordProcessorRequest {
        private Integer pageSize;

        // Getters and setters
        public Integer getPageSize() {
            return pageSize;
        }

        public void setPageSize(Integer pageSize) {
            this.pageSize = pageSize;
        }
    }
} 