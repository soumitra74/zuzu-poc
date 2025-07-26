package org.soumitra.reviewsystem;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.soumitra.reviewsystem.dao.JobRunRepository;
import org.soumitra.reviewsystem.dao.S3FileRepository;
import org.soumitra.reviewsystem.dao.RecordRepository;
import org.soumitra.reviewsystem.dao.RecordErrorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class JobRunnerIntegrationTest {

    @Autowired
    private S3Client s3Client;
    
    private static final String BUCKET = "hotel-reviews";

    @Test
    void testListAllFilesInBucket() {
        // Upload some files
        // s3Client.putObject(PutObjectRequest.builder().bucket(BUCKET).key("file1.jsonl").build(),
        //         software.amazon.awssdk.core.sync.RequestBody.fromString("{}\n{}"));
        // s3Client.putObject(PutObjectRequest.builder().bucket(BUCKET).key("file2.jsonl").build(),
        //         software.amazon.awssdk.core.sync.RequestBody.fromString("{}"));

        // Use JobRunner's S3FileLister
        List<JobRunner.S3FileRef> files = JobRunner.S3FileLister.listAllFilesInBucket("s3://" + BUCKET, s3Client);
        assertEquals(2, files.size());
        assertTrue(files.stream().anyMatch(f -> f.getKey().equals("file1.jsonl")));
        assertTrue(files.stream().anyMatch(f -> f.getKey().equals("file2.jsonl")));
    }

    @Test
    void testRunJobProcessesFiles() {
        // Mock repositories
        JobRunRepository jobRepo = Mockito.mock(JobRunRepository.class);
        S3FileRepository fileRepo = Mockito.mock(S3FileRepository.class);
        RecordRepository recordRepo = Mockito.mock(RecordRepository.class);
        RecordErrorRepository recordErrorRepo = Mockito.mock(RecordErrorRepository.class);

        // Upload a file
        // s3Client.putObject(PutObjectRequest.builder().bucket(BUCKET).key("test.jsonl").build(),
        //         software.amazon.awssdk.core.sync.RequestBody.fromString("{\"foo\":1}\n{\"foo\":2}"));

        JobRunner runner = new JobRunner(jobRepo, fileRepo, recordRepo, recordErrorRepo, s3Client, 10);
        // This will call S3FileLister.listAllFilesInBucket internally
        System.out.println("********************** Running job for bucket: " + BUCKET);
        runner.runJob("s3://" + BUCKET);
        // You can verify repository interactions here if needed
        Mockito.verifyNoMoreInteractions(jobRepo, fileRepo, recordRepo, recordErrorRepo);
    }
} 