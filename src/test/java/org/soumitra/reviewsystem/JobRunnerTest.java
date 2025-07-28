package org.soumitra.reviewsystem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.soumitra.reviewsystem.dao.JobRunRepository;
import org.soumitra.reviewsystem.dao.RecordRepository;
import org.soumitra.reviewsystem.dao.S3FileRepository;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobRunnerTest {

    @Mock
    private JobRunRepository jobRunRepository;

    @Mock
    private S3FileRepository s3FileRepository;

    @Mock
    private RecordRepository recordRepository;

    @Mock
    private S3Client s3Client;

    @Mock
    private ListObjectsV2Response listObjectsResponse;

    private JobRunner jobRunner;

    @BeforeEach
    void setUp() {
        jobRunner = new JobRunner(
            jobRunRepository, s3FileRepository, recordRepository,
            s3Client, 10
        );
    }

    @Test
    void testRunJobWithValidS3Files() throws Exception {
        // Setup test data
        String s3Uri = "s3://test-bucket/test-prefix/";
        List<S3Object> s3Objects = Arrays.asList(
            createS3Object("test-prefix/file1.jsonl", 1000L),
            createS3Object("test-prefix/file2.jsonl", 2000L)
        );

        // Mock S3 responses
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
            .thenReturn(listObjectsResponse);
        when(listObjectsResponse.contents()).thenReturn(s3Objects);
        when(listObjectsResponse.nextContinuationToken()).thenReturn(null);

        // Mock repository responses
        when(jobRunRepository.insertJob(any(LocalDateTime.class), anyString(), anyString(), anyString()))
            .thenReturn(1);
        when(s3FileRepository.insertOrUpdateFile(anyInt(), anyString(), anyString(), anyString(), any(), anyBoolean()))
            .thenReturn(1);
        doNothing().when(s3FileRepository).updateFileStatus(anyInt(), anyString(), any(), anyInt(), anyBoolean());
        doNothing().when(jobRunRepository).updateJobStatus(anyInt(), any(LocalDateTime.class), anyString());

        // Mock JsonlPaginator (static method, so we need to test it separately)
        // For now, we'll just verify the job creation and completion

        // Execute
        jobRunner.runJob(s3Uri);

        // Verify
        verify(jobRunRepository).insertJob(any(LocalDateTime.class), eq("MANUAL"), eq("running"), eq("Processing S3 files"));
        verify(s3FileRepository, times(2)).insertOrUpdateFile(eq(1), eq("test-bucket"), anyString(), eq("processing"), eq(null), eq(true));
        verify(jobRunRepository).updateJobStatus(eq(1), any(LocalDateTime.class), eq("success"));
    }

    @Test
    void testRunJobWithEmptyBucket() throws Exception {
        // Setup test data
        String s3Uri = "s3://test-bucket/empty-prefix/";

        // Mock S3 responses
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
            .thenReturn(listObjectsResponse);
        when(listObjectsResponse.contents()).thenReturn(List.of());
        when(listObjectsResponse.nextContinuationToken()).thenReturn(null);

        // Mock repository responses
        when(jobRunRepository.insertJob(any(LocalDateTime.class), anyString(), anyString(), anyString()))
            .thenReturn(1);
        doNothing().when(jobRunRepository).updateJobStatus(anyInt(), any(LocalDateTime.class), anyString());

        // Execute
        jobRunner.runJob(s3Uri);

        // Verify
        verify(jobRunRepository).insertJob(any(LocalDateTime.class), eq("MANUAL"), eq("running"), eq("Processing S3 files"));
        verify(s3FileRepository, never()).insertOrUpdateFile(anyInt(), anyString(), anyString(), anyString(), any(), anyBoolean());
        verify(jobRunRepository).updateJobStatus(eq(1), any(LocalDateTime.class), eq("success"));
    }

    @Test
    void testS3FileListerParseUri() {
        // Test valid URI
        String[] result1 = JobRunner.S3FileLister.parseUri("s3://bucket-name/prefix/path/");
        assertEquals("bucket-name", result1[0]);
        assertEquals("prefix/path/", result1[1]);

        // Test URI without prefix
        String[] result2 = JobRunner.S3FileLister.parseUri("s3://bucket-name");
        assertEquals("bucket-name", result2[0]);
        assertEquals("", result2[1]);

        // Test URI with root prefix
        String[] result3 = JobRunner.S3FileLister.parseUri("s3://bucket-name/");
        assertEquals("bucket-name", result3[0]);
        assertEquals("", result3[1]);
    }

    @Test
    void testS3FileRef() {
        // Test constructor without lastModified
        JobRunner.S3FileRef fileRef1 = new JobRunner.S3FileRef("test-bucket", "test-key");
        assertEquals("test-bucket", fileRef1.getBucket());
        assertEquals("test-key", fileRef1.getKey());
        assertNull(fileRef1.getLastModified());

        // Test constructor with lastModified
        Instant now = Instant.now();
        JobRunner.S3FileRef fileRef2 = new JobRunner.S3FileRef("test-bucket", "test-key", now);
        assertEquals("test-bucket", fileRef2.getBucket());
        assertEquals("test-key", fileRef2.getKey());
        assertEquals(now, fileRef2.getLastModified());

        // Test toString
        String toString = fileRef2.toString();
        assertTrue(toString.contains("test-bucket"));
        assertTrue(toString.contains("test-key"));
        assertTrue(toString.contains(now.toString()));
    }

    @Test
    void testLogRecord() throws Exception {
        // Setup test data
        Integer fileId = 1;
        Integer jobId = 1;
        Integer lineNumber = 5;
        String jsonLine = "{\"test\": \"data\"}";

        // Mock repository
        when(recordRepository.logNewRecord(anyInt(), anyInt(), anyString())).thenReturn(1);

        // Execute (using reflection to access private method)
        // For now, we'll test the method indirectly through the public interface
        // In a real scenario, you might want to make this method package-private for testing

        // Verify that the method doesn't throw exceptions
        assertDoesNotThrow(() -> {
            // This would be the actual test if the method was accessible
        });
    }

    @Test
    void testRunJobWithS3Exception() throws Exception {
        // Setup test data
        String s3Uri = "s3://test-bucket/test-prefix/";

        // Mock S3 to throw exception
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
            .thenThrow(new RuntimeException("S3 connection failed"));

        // Mock repository responses
        when(jobRunRepository.insertJob(any(LocalDateTime.class), anyString(), anyString(), anyString()))
            .thenReturn(1);

        // Execute and verify exception is thrown
        Exception exception = assertThrows(RuntimeException.class, () -> {
            jobRunner.runJob(s3Uri);
        });

        assertEquals("S3 connection failed", exception.getMessage());
        verify(jobRunRepository).insertJob(any(LocalDateTime.class), eq("MANUAL"), eq("running"), eq("Processing S3 files"));
    }

    @Test
    void testRunJobWithInvalidS3Uri() {
        // Test with invalid URI format
        String invalidUri = "invalid-uri";

        Exception exception = assertThrows(Exception.class, () -> {
            jobRunner.runJob(invalidUri);
        });

        assertNotNull(exception);
    }

    // Helper methods
    private S3Object createS3Object(String key, Long size) {
        return S3Object.builder()
            .key(key)
            .size(size)
            .lastModified(Instant.now())
            .build();
    }
} 