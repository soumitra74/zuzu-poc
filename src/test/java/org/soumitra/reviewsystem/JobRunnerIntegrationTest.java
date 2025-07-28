package org.soumitra.reviewsystem;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.soumitra.reviewsystem.dao.JobRunRepository;
import org.soumitra.reviewsystem.dao.S3FileRepository;
import org.soumitra.reviewsystem.dao.RecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ReviewSystemApplication.class)
@ActiveProfiles("test")
public class JobRunnerIntegrationTest {

    @Autowired
    private S3Client s3Client;
    
    private static final String BUCKET = "hotel-reviews";

    @Test
    void testListAllFilesInBucket() {
        try {
            // Create bucket if it doesn't exist
            try {
                s3Client.createBucket(software.amazon.awssdk.services.s3.model.CreateBucketRequest.builder()
                    .bucket(BUCKET).build());
            } catch (Exception e) {
                // Bucket might already exist, ignore
            }
            
            // Use JobRunner's S3FileLister; at least one test file should be in the bucket
            List<JobRunner.S3FileRef> files = JobRunner.S3FileLister.listAllFilesInBucket("s3://" + BUCKET, s3Client);
            assertTrue(files.size() >= 1);
            assertTrue(files.stream().anyMatch(f -> f.getKey().endsWith(".jl")));
        } catch (Exception e) {
            System.err.println("Error in test: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void testRunJobProcessesFiles() {
        // Mock repositories
        JobRunRepository jobRepo = Mockito.mock(JobRunRepository.class);
        S3FileRepository fileRepo = Mockito.mock(S3FileRepository.class);
        RecordRepository recordRepo = Mockito.mock(RecordRepository.class);

        // Upload a file
        // s3Client.putObject(PutObjectRequest.builder().bucket(BUCKET).key("test.jsonl").build(),
        //         software.amazon.awssdk.core.sync.RequestBody.fromString("{\"foo\":1}\n{\"foo\":2}"));

        JobRunner runner = new JobRunner(jobRepo, fileRepo, recordRepo, 
            s3Client, 10);
        // This will call S3FileLister.listAllFilesInBucket internally
        System.out.println("********************** Running job for bucket: " + BUCKET);
        runner.runJob("s3://" + BUCKET);
        // You can verify repository interactions here if needed
        // Mockito.verifyNoMoreInteractions(jobRepo, fileRepo, recordRepo, recordErrorRepo);
    }
} 