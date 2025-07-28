package org.soumitra.reviewsystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.soumitra.reviewsystem.JobRunner;
import org.soumitra.reviewsystem.RecordProcessorJob;
import org.soumitra.reviewsystem.dao.*;
import org.soumitra.reviewsystem.model.JobRun;
import org.soumitra.reviewsystem.model.S3File;
import org.soumitra.reviewsystem.model.Record;
import org.soumitra.reviewsystem.model.RecordError;
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
    private org.soumitra.reviewsystem.dao.StayInfoRepository stayInfoRepository;

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
                stayInfoRepository,
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

    // ==================== S3 FILES ENDPOINTS ====================

    /**
     * List all S3 files
     * GET /api/jobs/s3-files
     */
    @GetMapping("/s3-files")
    public ResponseEntity<Map<String, Object>> getAllS3Files(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer jobRunId) {
        
        try {
            List<S3File> files;
            
            if (jobRunId != null) {
                // Filter by job run ID
                files = s3FileRepository.findByJobRunId(jobRunId);
            } else if (status != null && !status.trim().isEmpty()) {
                // Filter by status
                files = s3FileRepository.findByStatus(status);
            } else {
                // Get all files with pagination
                Pageable pageable = PageRequest.of(page, size, Sort.by("startedAt").descending());
                Page<S3File> filePage = s3FileRepository.findAll(pageable);
                files = filePage.getContent();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("files", files);
            response.put("totalFiles", files.size());
            response.put("page", page);
            response.put("size", size);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to retrieve S3 files: " + e.getMessage());
            errorResponse.put("exception", e.getClass().getSimpleName());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get a specific S3 file by ID
     * GET /api/jobs/s3-files/{fileId}
     */
    @GetMapping("/s3-files/{fileId}")
    public ResponseEntity<Map<String, Object>> getS3FileById(@PathVariable Integer fileId) {
        
        try {
            Optional<S3File> fileOptional = s3FileRepository.findById(fileId);
            
            if (fileOptional.isPresent()) {
                S3File file = fileOptional.get();
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("file", file);
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "S3 file not found with ID: " + fileId);
                
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to retrieve S3 file: " + e.getMessage());
            errorResponse.put("exception", e.getClass().getSimpleName());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // ==================== RECORDS ENDPOINTS ====================

    /**
     * List all records
     * GET /api/jobs/records
     */
    @GetMapping("/records")
    public ResponseEntity<Map<String, Object>> getAllRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer jobRunId,
            @RequestParam(required = false) Integer s3FileId) {
        
        try {
            List<Record> records;
            
            if (jobRunId != null) {
                // Filter by job run ID
                records = recordRepository.findByJobRunId(jobRunId);
            } else if (s3FileId != null) {
                // Filter by S3 file ID
                records = recordRepository.findByS3FileId(s3FileId);
            } else if (status != null && !status.trim().isEmpty()) {
                // Filter by status
                records = recordRepository.findByStatus(status);
            } else {
                // Get all records with pagination
                Pageable pageable = PageRequest.of(page, size, Sort.by("processedAt").descending());
                Page<Record> recordPage = recordRepository.findAll(pageable);
                records = recordPage.getContent();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("records", records);
            response.put("totalRecords", records.size());
            response.put("page", page);
            response.put("size", size);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to retrieve records: " + e.getMessage());
            errorResponse.put("exception", e.getClass().getSimpleName());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get a specific record by ID
     * GET /api/jobs/records/{recordId}
     */
    @GetMapping("/records/{recordId}")
    public ResponseEntity<Map<String, Object>> getRecordById(@PathVariable Integer recordId) {
        
        try {
            Optional<Record> recordOptional = recordRepository.findById(recordId);
            
            if (recordOptional.isPresent()) {
                Record record = recordOptional.get();
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("record", record);
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Record not found with ID: " + recordId);
                
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to retrieve record: " + e.getMessage());
            errorResponse.put("exception", e.getClass().getSimpleName());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // ==================== RECORD ERRORS ENDPOINTS ====================

    /**
     * List all record errors
     * GET /api/jobs/record-errors
     */
    @GetMapping("/record-errors")
    public ResponseEntity<Map<String, Object>> getAllRecordErrors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String errorType,
            @RequestParam(required = false) Integer recordId) {
        
        try {
            List<RecordError> errors;
            
            if (recordId != null) {
                // Filter by record ID
                Optional<RecordError> errorOptional = recordErrorRepository.findByRecordId(recordId);
                errors = errorOptional.map(List::of).orElse(List.of());
            } else if (errorType != null && !errorType.trim().isEmpty()) {
                // Filter by error type
                errors = recordErrorRepository.findByErrorType(errorType);
            } else {
                // Get all errors with pagination
                Pageable pageable = PageRequest.of(page, size);
                Page<RecordError> errorPage = recordErrorRepository.findAll(pageable);
                errors = errorPage.getContent();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("errors", errors);
            response.put("totalErrors", errors.size());
            response.put("page", page);
            response.put("size", size);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to retrieve record errors: " + e.getMessage());
            errorResponse.put("exception", e.getClass().getSimpleName());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get a specific record error by record ID
     * GET /api/jobs/record-errors/{recordId}
     */
    @GetMapping("/record-errors/{recordId}")
    public ResponseEntity<Map<String, Object>> getRecordErrorByRecordId(@PathVariable Integer recordId) {
        
        try {
            Optional<RecordError> errorOptional = recordErrorRepository.findByRecordId(recordId);
            
            if (errorOptional.isPresent()) {
                RecordError error = errorOptional.get();
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("error", error);
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Record error not found for record ID: " + recordId);
                
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to retrieve record error: " + e.getMessage());
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