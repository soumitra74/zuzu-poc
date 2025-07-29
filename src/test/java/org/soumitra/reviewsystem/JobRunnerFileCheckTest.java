package org.soumitra.reviewsystem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.soumitra.reviewsystem.dao.JobRunRepository;
import org.soumitra.reviewsystem.dao.RecordRepository;
import org.soumitra.reviewsystem.dao.S3FileRepository;
import org.soumitra.reviewsystem.util.MockS3Client;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobRunnerFileCheckTest {

    @Mock
    private JobRunRepository jobRunRepository;

    @Mock
    private S3FileRepository s3FileRepository;

    @Mock
    private RecordRepository recordRepository;

    private MockS3Client s3Client;
    private JobRunner jobRunner;

    @BeforeEach
    void setUp() {
        s3Client = new MockS3Client();
        jobRunner = new JobRunner(
            jobRunRepository, s3FileRepository, recordRepository,
            s3Client, 10
        );
    }

    @Test
    void testRunJobSkipsAlreadyProcessedFiles() throws Exception {
        // Setup test data
        String s3Uri = "s3://test-bucket/test-prefix/";
        
        // Add test files to MockS3Client
        s3Client.clearBucketContents();
        s3Client.addBucketContent("test-bucket", "test-prefix/file1.jsonl", 1000L);
        s3Client.addBucketContent("test-bucket", "test-prefix/file2.jsonl", 2000L);

        // Mock repository responses
        when(jobRunRepository.insertJob(any(LocalDateTime.class), anyString(), anyString(), anyString()))
            .thenReturn(1);
        doNothing().when(jobRunRepository).updateJobStatus(anyInt(), any(LocalDateTime.class), anyString());

        // Mock file repository to simulate already processed file
        when(s3FileRepository.isFileSuccessfullyProcessed("test-prefix/file1.jsonl")).thenReturn(true);
        when(s3FileRepository.isFileSuccessfullyProcessed("test-prefix/file2.jsonl")).thenReturn(false);
        when(s3FileRepository.isFileBeingProcessed(anyString())).thenReturn(false);
        when(s3FileRepository.insertOrUpdateFile(anyInt(), anyString(), anyString(), anyString(), any(), anyBoolean()))
            .thenReturn(1);
        doNothing().when(s3FileRepository).updateFileStatus(anyInt(), anyString(), any(), anyInt(), anyBoolean());

        // Execute
        jobRunner.runJob(s3Uri);

        // Verify
        verify(jobRunRepository).insertJob(any(LocalDateTime.class), eq("MANUAL"), eq("running"), eq("Processing S3 files"));
        
        // Verify that isFileSuccessfullyProcessed was called for both files
        verify(s3FileRepository).isFileSuccessfullyProcessed("test-prefix/file1.jsonl");
        verify(s3FileRepository).isFileSuccessfullyProcessed("test-prefix/file2.jsonl");
        
        // Verify that insertOrUpdateFile was only called for the unprocessed file
        verify(s3FileRepository).insertOrUpdateFile(eq(1), eq("test-bucket"), eq("test-prefix/file2.jsonl"), eq("processing"), eq(null), eq(true));
        
        // Verify that insertOrUpdateFile was NOT called for the already processed file
        verify(s3FileRepository, never()).insertOrUpdateFile(eq(1), eq("test-bucket"), eq("test-prefix/file1.jsonl"), anyString(), any(), anyBoolean());
        
        verify(jobRunRepository).updateJobStatus(eq(1), any(LocalDateTime.class), eq("success"));
    }

    @Test
    void testRunJobSkipsCurrentlyProcessingFiles() throws Exception {
        // Setup test data
        String s3Uri = "s3://test-bucket/test-prefix/";
        
        // Add test files to MockS3Client
        s3Client.clearBucketContents();
        s3Client.addBucketContent("test-bucket", "test-prefix/file1.jsonl", 1000L);

        // Mock repository responses
        when(jobRunRepository.insertJob(any(LocalDateTime.class), anyString(), anyString(), anyString()))
            .thenReturn(1);
        doNothing().when(jobRunRepository).updateJobStatus(anyInt(), any(LocalDateTime.class), anyString());

        // Mock file repository to simulate currently processing file
        when(s3FileRepository.isFileSuccessfullyProcessed("test-prefix/file1.jsonl")).thenReturn(false);
        when(s3FileRepository.isFileBeingProcessed("test-prefix/file1.jsonl")).thenReturn(true);

        // Execute
        jobRunner.runJob(s3Uri);

        // Verify
        verify(jobRunRepository).insertJob(any(LocalDateTime.class), eq("MANUAL"), eq("running"), eq("Processing S3 files"));
        
        // Verify that isFileBeingProcessed was called
        verify(s3FileRepository).isFileBeingProcessed("test-prefix/file1.jsonl");
        
        // Verify that insertOrUpdateFile was NOT called for the currently processing file
        verify(s3FileRepository, never()).insertOrUpdateFile(anyInt(), anyString(), anyString(), anyString(), any(), anyBoolean());
        
        verify(jobRunRepository).updateJobStatus(eq(1), any(LocalDateTime.class), eq("success"));
    }

    @Test
    void testRunJobProcessesNewFiles() throws Exception {
        // Setup test data
        String s3Uri = "s3://test-bucket/test-prefix/";
        
        // Add test files to MockS3Client
        s3Client.clearBucketContents();
        s3Client.addBucketContent("test-bucket", "test-prefix/file1.jsonl", 1000L);

        // Mock repository responses
        when(jobRunRepository.insertJob(any(LocalDateTime.class), anyString(), anyString(), anyString()))
            .thenReturn(1);
        doNothing().when(jobRunRepository).updateJobStatus(anyInt(), any(LocalDateTime.class), anyString());

        // Mock file repository to simulate new file
        when(s3FileRepository.isFileSuccessfullyProcessed("test-prefix/file1.jsonl")).thenReturn(false);
        when(s3FileRepository.isFileBeingProcessed("test-prefix/file1.jsonl")).thenReturn(false);
        when(s3FileRepository.insertOrUpdateFile(anyInt(), anyString(), anyString(), anyString(), any(), anyBoolean()))
            .thenReturn(1);
        doNothing().when(s3FileRepository).updateFileStatus(anyInt(), anyString(), any(), anyInt(), anyBoolean());

        // Execute
        jobRunner.runJob(s3Uri);

        // Verify
        verify(jobRunRepository).insertJob(any(LocalDateTime.class), eq("MANUAL"), eq("running"), eq("Processing S3 files"));
        
        // Verify that insertOrUpdateFile was called for the new file
        verify(s3FileRepository).insertOrUpdateFile(eq(1), eq("test-bucket"), eq("test-prefix/file1.jsonl"), eq("processing"), eq(null), eq(true));
        
        verify(jobRunRepository).updateJobStatus(eq(1), any(LocalDateTime.class), eq("success"));
    }
} 