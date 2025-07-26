package org.soumitra.reviewsystem;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class JsonlPaginator {
    
    public static List<String> readJsonLines(String bucket, String key, int startLine, int pageSize) {
        S3Client s3Client = S3Client.create();
        List<String> lines = new ArrayList<>();
        
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            
            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response))) {
                String line;
                int currentLine = 0;
                int linesRead = 0;
                
                // Skip to start line
                while (currentLine < startLine && (line = reader.readLine()) != null) {
                    currentLine++;
                }
                
                // Read pageSize lines
                while (linesRead < pageSize && (line = reader.readLine()) != null) {
                    lines.add(line);
                    linesRead++;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading from S3: " + e.getMessage(), e);
        }
        
        return lines;
    }
    
    public static List<String> listS3Keys(String bucket) {
        S3Client s3Client = S3Client.create();
        List<String> keys = new ArrayList<>();
        
        try {
            software.amazon.awssdk.services.s3.model.ListObjectsV2Request request = 
                software.amazon.awssdk.services.s3.model.ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .build();
            
            software.amazon.awssdk.services.s3.model.ListObjectsV2Response response = 
                s3Client.listObjectsV2(request);
            
            for (software.amazon.awssdk.services.s3.model.S3Object obj : response.contents()) {
                keys.add(obj.key());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error listing S3 keys: " + e.getMessage(), e);
        }
        
        return keys;
    }
} 