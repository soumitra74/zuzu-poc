package org.soumitra.reviewsystem.cli;

import org.soumitra.reviewsystem.JobRunner;
import org.soumitra.reviewsystem.RecordProcessorJob;
import org.soumitra.reviewsystem.dao.JobRunRepository;
import org.soumitra.reviewsystem.dao.S3FileRepository;
import org.soumitra.reviewsystem.dao.RecordRepository;
import org.soumitra.reviewsystem.dao.RecordErrorRepository;
import org.soumitra.reviewsystem.dao.ReviewRepository;
import org.soumitra.reviewsystem.dao.HotelRepository;
import org.soumitra.reviewsystem.dao.ProviderRepository;
import org.soumitra.reviewsystem.dao.ReviewerRepository;
import org.soumitra.reviewsystem.model.JobRun;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class CLIService {

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
    private org.soumitra.reviewsystem.util.HotelReviewJsonParser hotelReviewJsonParser;

    public void runJob(String bucket, int pageSize, String triggerType, String notes) {
        System.out.println("Starting job to process reviews from bucket: " + bucket);
        System.out.println("Page size: " + pageSize);
        System.out.println("Trigger type: " + triggerType);
        System.out.println("Notes: " + notes);
        
        try {
            JobRunner runner = new JobRunner(jobRunRepository, s3FileRepository, 
                recordRepository, s3Client, pageSize);
            
            String s3Uri = "s3://" + bucket;
            runner.runJob(s3Uri);
            
            System.out.println("Job completed successfully!");
        } catch (Exception e) {
            System.err.println("Job failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void processRecords(int batchSize, String triggerType, String notes) {
        System.out.println("Starting record processing job");
        System.out.println("Batch size: " + batchSize);
        System.out.println("Trigger type: " + triggerType);
        System.out.println("Notes: " + notes);
        
        try {
            RecordProcessorJob processor = new RecordProcessorJob(jobRunRepository, 
                recordRepository, recordErrorRepository, reviewRepository, hotelRepository,
                providerRepository, reviewerRepository, stayInfoRepository, hotelReviewJsonParser, batchSize);
            
            processor.runJob();
            
            System.out.println("Record processing job completed successfully!");
        } catch (Exception e) {
            System.err.println("Record processing job failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void listJobs(int limit, String status) {
        System.out.println("Recent job runs (limit: " + limit + ")");
        if (status != null) {
            System.out.println("Filtering by status: " + status);
        }
        System.out.println();
        
        List<JobRun> jobs;
        if (status != null) {
            jobs = jobRunRepository.findByStatus(status);
        } else {
            // Get all jobs and limit them (you might want to add a method to repository for this)
            jobs = jobRunRepository.findAll();
        }
        
        if (jobs.isEmpty()) {
            System.out.println("No jobs found.");
            return;
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        System.out.printf("%-5s %-20s %-10s %-20s %-20s%n", 
            "ID", "Scheduled At", "Status", "Started At", "Finished At");
        System.out.println("-".repeat(80));
        
        jobs.stream()
            .limit(limit)
            .forEach(job -> {
                System.out.printf("%-5d %-20s %-10s %-20s %-20s%n",
                    job.getId(),
                    job.getScheduledAt() != null ? job.getScheduledAt().format(formatter) : "N/A",
                    job.getStatus() != null ? job.getStatus() : "N/A",
                    job.getStartedAt() != null ? job.getStartedAt().format(formatter) : "N/A",
                    job.getFinishedAt() != null ? job.getFinishedAt().format(formatter) : "N/A"
                );
            });
    }

    public void listFiles(String bucket, String prefix) {
        System.out.println("Files in bucket: " + bucket);
        if (prefix != null) {
            System.out.println("With prefix: " + prefix);
        }
        System.out.println();
        
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(prefix != null ? prefix : "")
                .build();
            
            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            
            if (response.contents().isEmpty()) {
                System.out.println("No files found in bucket.");
                return;
            }
            
            System.out.printf("%-50s %-15s %-20s%n", "Key", "Size (bytes)", "Last Modified");
            System.out.println("-".repeat(90));
            
            for (S3Object obj : response.contents()) {
                String lastModified = obj.lastModified() != null ? 
                    obj.lastModified().toString() : "N/A";
                System.out.printf("%-50s %-15d %-20s%n",
                    obj.key(),
                    obj.size(),
                    lastModified
                );
            }
            
            System.out.println("\nTotal files: " + response.contents().size());
            
        } catch (Exception e) {
            System.err.println("Error listing files: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 