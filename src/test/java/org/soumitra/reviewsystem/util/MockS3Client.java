package org.soumitra.reviewsystem.util;

import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock implementation of S3Client for testing purposes
 */
public class MockS3Client implements S3Client {

    private final Map<String, List<S3Object>> bucketContents = new HashMap<>();
    private final Map<String, String> objectContents = new HashMap<>();
    private boolean shouldThrowException = false;
    private String exceptionMessage = "Mock S3 exception";

    public MockS3Client() {
        // Initialize with some test data
        setupTestData();
    }

    private void setupTestData() {
        // Add test files to a bucket
        List<S3Object> testFiles = new ArrayList<>();
        testFiles.add(createS3Object("test-prefix/file1.jsonl", 1000L));
        testFiles.add(createS3Object("test-prefix/file2.jsonl", 2000L));
        testFiles.add(createS3Object("test-prefix/file3.jsonl", 1500L));
        bucketContents.put("test-bucket", testFiles);

        // Add empty bucket
        bucketContents.put("empty-bucket", new ArrayList<>());

        // Add test object contents
        objectContents.put("test-bucket/test-prefix/file1.jsonl", 
            "{\"hotelId\": 1, \"platform\": \"Agoda\", \"hotelName\": \"Test Hotel 1\"}\n" +
            "{\"hotelId\": 2, \"platform\": \"Booking\", \"hotelName\": \"Test Hotel 2\"}");
        objectContents.put("test-bucket/test-prefix/file2.jsonl", 
            "{\"hotelId\": 3, \"platform\": \"Expedia\", \"hotelName\": \"Test Hotel 3\"}");
    }

    public void setShouldThrowException(boolean shouldThrowException) {
        this.shouldThrowException = shouldThrowException;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public void addBucketContent(String bucket, String key, long size) {
        bucketContents.computeIfAbsent(bucket, k -> new ArrayList<>())
            .add(createS3Object(key, size));
    }

    public void addObjectContent(String key, String content) {
        objectContents.put(key, content);
    }

    @Override
    public ListObjectsV2Response listObjectsV2(ListObjectsV2Request request) {
        if (shouldThrowException) {
            throw new RuntimeException(exceptionMessage);
        }

        // Handle null request
        if (request == null) {
            return ListObjectsV2Response.builder()
                .contents(new ArrayList<>())
                .nextContinuationToken(null)
                .build();
        }

        String bucket = request.bucket();
        String prefix = request.prefix() != null ? request.prefix() : "";
        
        List<S3Object> objects = bucketContents.getOrDefault(bucket, new ArrayList<>());
        
        // Filter by prefix if specified
        List<S3Object> filteredObjects = new ArrayList<>();
        for (S3Object obj : objects) {
            if (obj.key().startsWith(prefix)) {
                filteredObjects.add(obj);
            }
        }

        return ListObjectsV2Response.builder()
            .contents(filteredObjects)
            .nextContinuationToken(null) // No pagination for simplicity
            .build();
    }

    @Override
    public software.amazon.awssdk.core.ResponseInputStream<GetObjectResponse> getObject(GetObjectRequest request) {
        if (shouldThrowException) {
            throw new RuntimeException(exceptionMessage);
        }

        // Handle null request
        if (request == null) {
            GetObjectResponse response = GetObjectResponse.builder()
                .contentLength(0L)
                .build();
            return new software.amazon.awssdk.core.ResponseInputStream<>(response, null);
        }

        String key = request.key();
        String content = objectContents.getOrDefault(key, "");
        
        GetObjectResponse response = GetObjectResponse.builder()
            .contentLength((long) content.getBytes().length)
            .build();
        
        // Return a mock ResponseInputStream - in a real implementation, this would wrap the content
        return new software.amazon.awssdk.core.ResponseInputStream<>(response, null);
    }

    public PutObjectResponse putObject(PutObjectRequest request) {
        if (shouldThrowException) {
            throw new RuntimeException(exceptionMessage);
        }

        // Handle null request
        if (request == null) {
            return PutObjectResponse.builder().build();
        }

        // Store the object content
        objectContents.put(request.key(), "mock content");
        
        return PutObjectResponse.builder().build();
    }

    public DeleteObjectResponse deleteObject(DeleteObjectRequest request) {
        if (shouldThrowException) {
            throw new RuntimeException(exceptionMessage);
        }

        // Handle null request
        if (request == null) {
            return DeleteObjectResponse.builder().build();
        }

        objectContents.remove(request.key());
        return DeleteObjectResponse.builder().build();
    }

    public CreateBucketResponse createBucket(CreateBucketRequest request) {
        if (shouldThrowException) {
            throw new RuntimeException(exceptionMessage);
        }

        // Handle null request
        if (request == null) {
            return CreateBucketResponse.builder().build();
        }

        bucketContents.putIfAbsent(request.bucket(), new ArrayList<>());
        return CreateBucketResponse.builder().build();
    }

    public DeleteBucketResponse deleteBucket(DeleteBucketRequest request) {
        if (shouldThrowException) {
            throw new RuntimeException(exceptionMessage);
        }

        // Handle null request
        if (request == null) {
            return DeleteBucketResponse.builder().build();
        }

        bucketContents.remove(request.bucket());
        return DeleteBucketResponse.builder().build();
    }

    public HeadBucketResponse headBucket(HeadBucketRequest request) {
        if (shouldThrowException) {
            throw new RuntimeException(exceptionMessage);
        }

        // Handle null request
        if (request == null) {
            return HeadBucketResponse.builder().build();
        }

        boolean exists = bucketContents.containsKey(request.bucket());
        return HeadBucketResponse.builder().build();
    }

    public HeadObjectResponse headObject(HeadObjectRequest request) {
        if (shouldThrowException) {
            throw new RuntimeException(exceptionMessage);
        }

        // Handle null request
        if (request == null) {
            return HeadObjectResponse.builder()
                .contentLength(0L)
                .build();
        }

        boolean exists = objectContents.containsKey(request.key());
        return HeadObjectResponse.builder()
            .contentLength(exists ? 100L : 0L)
            .build();
    }

    // Helper method to create S3Object
    private S3Object createS3Object(String key, Long size) {
        return S3Object.builder()
            .key(key)
            .size(size)
            .lastModified(java.time.Instant.now())
            .build();
    }

    // Required S3Client interface methods (returning null for unused methods)
    @Override
    public String serviceName() {
        return "MockS3";
    }

    @Override
    public void close() {
        // No cleanup needed for mock
    }

    // Additional methods that might be needed for testing
    public void clearBucketContents() {
        bucketContents.clear();
    }

    public void clearObjectContents() {
        objectContents.clear();
    }

    public Map<String, List<S3Object>> getBucketContents() {
        return new HashMap<>(bucketContents);
    }

    public Map<String, String> getObjectContents() {
        return new HashMap<>(objectContents);
    }
} 