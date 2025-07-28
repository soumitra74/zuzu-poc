package org.soumitra.reviewsystem;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

// Repository interfaces
import org.soumitra.reviewsystem.dao.JobRunRepository;
import org.soumitra.reviewsystem.dao.S3FileRepository;
import org.soumitra.reviewsystem.dao.RecordRepository;

// AWS SDK imports
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

// JSON parsing
import com.fasterxml.jackson.databind.ObjectMapper;

public class JobRunner {

    private final JobRunRepository jobRepo;
    private final S3FileRepository fileRepo;
    private final RecordRepository recordRepo;
    private final S3Client s3Client;

    private final int pageSize;

    public JobRunner(JobRunRepository jobRepo, S3FileRepository fileRepo, 
        RecordRepository recordRepo, 
        S3Client s3Client, int batchSize) {
        this.jobRepo = jobRepo;
        this.fileRepo = fileRepo;
        this.recordRepo = recordRepo;
        this.s3Client = s3Client;
        this.pageSize = batchSize > 0 ? batchSize : 10;
    }

    public void runJob(String s3Uri) {
        // Create a new job run
        Integer jobId = jobRepo.insertJob(LocalDateTime.now(), "MANUAL", "running", "Processing S3 files");

        List<S3FileRef> filesToProcess = S3FileLister.listAllFilesInBucket(s3Uri, s3Client);

        int totalFilesProcessed = 0;
        int totalRecordsProcessed = 0;

        for (S3FileRef file : filesToProcess) {
            Integer fileId = fileRepo.insertOrUpdateFile(jobId, file.getBucket(), file.getKey(), "processing", null, true);

            int line = 0;
            boolean fileSuccess = true;
            int fileRecordCount = 0;
            String fileErrorMsg = null;

            try {
                while (true) {
                    List<String> lines = JsonlPaginator.readJsonLines(file.getBucket(), file.getKey(), line, pageSize, s3Client);

                    if (lines.isEmpty()) break;

                    for (int i = 0; i < lines.size(); i++) {
                        String jsonLine = lines.get(i);
                        int lineNumber = line + i; // 0-based line indexing

                        try {
                            logRecord(fileId, jobId, lineNumber, jsonLine);
                            fileRecordCount++;
                        } catch (Exception recEx) {
                            fileSuccess = false;
                            //recordRepo.logRecord(fileId, lineNumber, "FAILED", recEx.getMessage());
                        }
                    }

                    line += lines.size();
                }
            } catch (Exception fileEx) {
                fileSuccess = false;
                fileErrorMsg = fileEx.getMessage();
            } finally {
                fileRepo.updateFileStatus(fileId, fileSuccess ? "success" : "failed", fileErrorMsg, fileRecordCount, false);
            }

            totalFilesProcessed++;
            totalRecordsProcessed += fileRecordCount;
        }

        System.out.println("Total files processed: " + totalFilesProcessed);
        System.out.println("Total records processed: " + totalRecordsProcessed);

        // Update job status
        jobRepo.updateJobStatus(jobId, LocalDateTime.now(), "success");
    }

    public static class S3FileLister {

        public static List<S3FileRef> listAllFilesInBucket(String s3Uri, S3Client s3Client) {
            String[] parsed = parseUri(s3Uri);
            String bucket = parsed[0];
            String prefix = parsed[1];
    
            List<S3FileRef> results = new ArrayList<>();
    
            String continuationToken = null;
            do {
                ListObjectsV2Request request = ListObjectsV2Request.builder()
                        .bucket(bucket)
                        .prefix(prefix)
                        .continuationToken(continuationToken)
                        .build();
    
                ListObjectsV2Response response = s3Client.listObjectsV2(request);
    
                for (S3Object obj : response.contents()) {
                    results.add(new S3FileRef(bucket, obj.key(), obj.lastModified()));
                }
    
                continuationToken = response.nextContinuationToken();
            } while (continuationToken != null);
    
            return results;
        }

        public static String[] parseUri(String uri) {
            uri = uri.replace("s3://", "");
            String[] parts = uri.split("/", 2);
            String bucket = parts[0];
            String prefix = parts.length > 1 ? parts[1] : "";
            return new String[]{bucket, prefix};
        }
    }

    public static class S3FileRef {
        private final String bucket;
        private final String key;
        private final Instant lastModified;

        public S3FileRef(String bucket, String key) {
            this.bucket = bucket;
            this.key = key;
            this.lastModified = null;
        }

        public S3FileRef(String bucket, String key, Instant lastModified) {
            this.bucket = bucket;
            this.key = key;
            this.lastModified = lastModified;
        }
    
        public String getBucket() { return bucket; }
        public String getKey() { return key; }
        public Instant getLastModified() { return lastModified; }

        @Override
        public String toString() {
            return bucket + "/" + key + " (" + lastModified + ")";
        }
    }

    /**
     * Store each line of jsonl file in the record table
     */
    private void logRecord(Integer fileId, Integer jobId, Integer lineNumber, String jsonLine) throws Exception {
        System.out.println("Processing record " + lineNumber ); // + ": " + jsonLine);
        recordRepo.logNewRecord(fileId, jobId, jsonLine);
    }

}
