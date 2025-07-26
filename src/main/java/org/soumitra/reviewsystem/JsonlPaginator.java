package org.soumitra.reviewsystem.util;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class JsonlPaginator {

    public static List<String> readJsonLines(String bucket, String key, int startLine, int pageSize) throws Exception {
        S3Client s3 = S3Client.create();

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        BufferedReader reader = new BufferedReader(new InputStreamReader(s3.getObject(request)));
        List<String> lines = new ArrayList<>();

        int currentLine = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            if (currentLine >= startLine && lines.size() < pageSize) {
                lines.add(line);
            }
            currentLine++;
            if (lines.size() == pageSize) break;
        }

        reader.close();
        return lines;
    }
}