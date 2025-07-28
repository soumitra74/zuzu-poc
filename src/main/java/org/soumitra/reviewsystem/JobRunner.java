package org.soumitra.reviewsystem;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

// Repository interfaces
import org.soumitra.reviewsystem.dao.JobRunRepository;
import org.soumitra.reviewsystem.dao.S3FileRepository;
import org.soumitra.reviewsystem.dao.RecordRepository;
import org.soumitra.reviewsystem.dao.RecordErrorRepository;
import org.soumitra.reviewsystem.dao.ReviewRepository;
import org.soumitra.reviewsystem.dao.HotelRepository;
import org.soumitra.reviewsystem.dao.ProviderRepository;
import org.soumitra.reviewsystem.dao.ReviewerRepository;

// Model classes
import org.soumitra.reviewsystem.model.Review;
import org.soumitra.reviewsystem.model.Hotel;
import org.soumitra.reviewsystem.model.Provider;
import org.soumitra.reviewsystem.model.Reviewer;

// AWS SDK imports
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

// JSON parsing
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JobRunner {

    private final JobRunRepository jobRepo;
    private final S3FileRepository fileRepo;
    private final RecordRepository recordRepo;
    private final RecordErrorRepository recordErrorRepo;
    private final ReviewRepository reviewRepo;
    private final HotelRepository hotelRepo;
    private final ProviderRepository providerRepo;
    private final ReviewerRepository reviewerRepo;
    private final S3Client s3Client;

    private final int pageSize;
    private final ObjectMapper objectMapper;

    public JobRunner(JobRunRepository jobRepo, S3FileRepository fileRepo, 
        RecordRepository recordRepo, RecordErrorRepository recordErrorRepo, 
        ReviewRepository reviewRepo, HotelRepository hotelRepo, 
        ProviderRepository providerRepo, ReviewerRepository reviewerRepo,
        S3Client s3Client, int batchSize) {
        this.jobRepo = jobRepo;
        this.fileRepo = fileRepo;
        this.recordRepo = recordRepo;
        this.recordErrorRepo = recordErrorRepo;
        this.reviewRepo = reviewRepo;
        this.hotelRepo = hotelRepo;
        this.providerRepo = providerRepo;
        this.reviewerRepo = reviewerRepo;
        this.s3Client = s3Client;
        this.pageSize = batchSize > 0 ? batchSize : 10;
        this.objectMapper = new ObjectMapper();
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

    /**
     * Parse JSON review data and store in database
     * Implements upsert logic for reviews, hotels, providers, and reviewers
     */
    private void processRecord(String jsonLine) throws Exception {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonLine);
            
            // Extract and upsert hotel
            Hotel hotel = upsertHotel(jsonNode);
            
            // Extract and upsert provider
            Provider provider = upsertProvider(jsonNode);
            
            // Extract and upsert reviewer
            Reviewer reviewer = upsertReviewer(jsonNode);
            
            // Extract and upsert review
            upsertReview(jsonNode, hotel, provider, reviewer);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse and store record: " + e.getMessage(), e);
        }
    }
    
    /**
     * Upsert hotel data
     */
    private Hotel upsertHotel(JsonNode jsonNode) {
        JsonNode hotelNode = jsonNode.get("hotel");
        if (hotelNode == null) {
            throw new RuntimeException("Hotel data is missing");
        }
        
        Integer hotelExternalId = hotelNode.get("hotel_id") != null ? 
            hotelNode.get("hotel_id").asInt() : null;
        String hotelName = hotelNode.get("hotel_name") != null ? 
            hotelNode.get("hotel_name").asText() : null;
            
        if (hotelExternalId == null || hotelName == null) {
            throw new RuntimeException("Hotel ID or name is missing");
        }
        
        // Check if hotel exists
        return hotelRepo.findByExternalId(hotelExternalId)
            .orElseGet(() -> {
                Hotel newHotel = Hotel.builder()
                    .externalId(hotelExternalId)
                    .hotelName(hotelName)
                    .build();
                return hotelRepo.save(newHotel);
            });
    }
    
    /**
     * Upsert provider data
     */
    private Provider upsertProvider(JsonNode jsonNode) {
        JsonNode providerNode = jsonNode.get("provider");
        if (providerNode == null) {
            throw new RuntimeException("Provider data is missing");
        }
        
        Short providerExternalId = providerNode.get("provider_id") != null ? 
            providerNode.get("provider_id").shortValue() : null;
        String providerName = providerNode.get("provider_name") != null ? 
            providerNode.get("provider_name").asText() : null;
            
        if (providerExternalId == null || providerName == null) {
            throw new RuntimeException("Provider ID or name is missing");
        }
        
        // Check if provider exists
        return providerRepo.findByExternalId(providerExternalId)
            .orElseGet(() -> {
                Provider newProvider = Provider.builder()
                    .externalId(providerExternalId)
                    .providerName(providerName)
                    .build();
                return providerRepo.save(newProvider);
            });
    }
    
    /**
     * Upsert reviewer data
     */
    private Reviewer upsertReviewer(JsonNode jsonNode) {
        JsonNode reviewerNode = jsonNode.get("reviewer");
        if (reviewerNode == null) {
            throw new RuntimeException("Reviewer data is missing");
        }
        
        String displayName = reviewerNode.get("display_name") != null ? 
            reviewerNode.get("display_name").asText() : null;
        String countryName = reviewerNode.get("country_name") != null ? 
            reviewerNode.get("country_name").asText() : null;
        Integer countryId = reviewerNode.get("country_id") != null ? 
            reviewerNode.get("country_id").asInt() : null;
        String flagCode = reviewerNode.get("flag_code") != null ? 
            reviewerNode.get("flag_code").asText() : null;
        Boolean isExpert = reviewerNode.get("is_expert") != null ? 
            reviewerNode.get("is_expert").asBoolean() : null;
        Integer reviewsWritten = reviewerNode.get("reviews_written") != null ? 
            reviewerNode.get("reviews_written").asInt() : null;
            
        if (displayName == null) {
            throw new RuntimeException("Reviewer display name is missing");
        }
        
        // Check if reviewer exists
        return reviewerRepo.findByDisplayNameAndCountryName(displayName, countryName)
            .orElseGet(() -> {
                Reviewer newReviewer = Reviewer.builder()
                    .displayName(displayName)
                    .countryName(countryName)
                    .countryId(countryId)
                    .flagCode(flagCode)
                    .isExpert(isExpert)
                    .reviewsWritten(reviewsWritten)
                    .build();
                return reviewerRepo.save(newReviewer);
            });
    }
    
    /**
     * Upsert review data
     */
    private void upsertReview(JsonNode jsonNode, Hotel hotel, Provider provider, Reviewer reviewer) {
        Long reviewExternalId = jsonNode.get("review_id") != null ? 
            jsonNode.get("review_id").asLong() : null;
            
        if (reviewExternalId == null) {
            throw new RuntimeException("Review ID is missing");
        }
        
        // Check if review already exists
        if (reviewRepo.existsByReviewExternalId(reviewExternalId)) {
            return; // Skip if review already exists
        }
        
        // Extract review data
        Double ratingRaw = jsonNode.get("rating_raw") != null ? 
            jsonNode.get("rating_raw").asDouble() : null;
        String ratingText = jsonNode.get("rating_text") != null ? 
            jsonNode.get("rating_text").asText() : null;
        String ratingFormatted = jsonNode.get("rating_formatted") != null ? 
            jsonNode.get("rating_formatted").asText() : null;
        String reviewTitle = jsonNode.get("review_title") != null ? 
            jsonNode.get("review_title").asText() : null;
        String reviewComment = jsonNode.get("review_comment") != null ? 
            jsonNode.get("review_comment").asText() : null;
        Integer reviewVotePositive = jsonNode.get("review_vote_positive") != null ? 
            jsonNode.get("review_vote_positive").asInt() : null;
        Integer reviewVoteNegative = jsonNode.get("review_vote_negative") != null ? 
            jsonNode.get("review_vote_negative").asInt() : null;
        String translateSource = jsonNode.get("translate_source") != null ? 
            jsonNode.get("translate_source").asText() : null;
        String translateTarget = jsonNode.get("translate_target") != null ? 
            jsonNode.get("translate_target").asText() : null;
        Boolean isResponseShown = jsonNode.get("is_response_shown") != null ? 
            jsonNode.get("is_response_shown").asBoolean() : null;
        String responderName = jsonNode.get("responder_name") != null ? 
            jsonNode.get("responder_name").asText() : null;
        String responseText = jsonNode.get("response_text") != null ? 
            jsonNode.get("response_text").asText() : null;
        String responseDateText = jsonNode.get("response_date_text") != null ? 
            jsonNode.get("response_date_text").asText() : null;
        String responseDateFmt = jsonNode.get("response_date_fmt") != null ? 
            jsonNode.get("response_date_fmt").asText() : null;
        String checkInMonthYr = jsonNode.get("check_in_month_yr") != null ? 
            jsonNode.get("check_in_month_yr").asText() : null;
            
        // Parse review date
        OffsetDateTime reviewDate = null;
        if (jsonNode.get("review_date") != null) {
            try {
                String reviewDateStr = jsonNode.get("review_date").asText();
                reviewDate = OffsetDateTime.parse(reviewDateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            } catch (Exception e) {
                // Log warning but continue
                System.out.println("Warning: Could not parse review date: " + jsonNode.get("review_date").asText());
            }
        }
        
        // Create and save review
        Review review = Review.builder()
            .reviewExternalId(reviewExternalId)
            .hotel(hotel)
            .provider(provider)
            .reviewer(reviewer)
            .ratingRaw(ratingRaw)
            .ratingText(ratingText)
            .ratingFormatted(ratingFormatted)
            .reviewTitle(reviewTitle)
            .reviewComment(reviewComment)
            .reviewVotePositive(reviewVotePositive)
            .reviewVoteNegative(reviewVoteNegative)
            .reviewDate(reviewDate)
            .translateSource(translateSource)
            .translateTarget(translateTarget)
            .isResponseShown(isResponseShown)
            .responderName(responderName)
            .responseText(responseText)
            .responseDateText(responseDateText)
            .responseDateFmt(responseDateFmt)
            .checkInMonthYr(checkInMonthYr)
            .build();
            
        reviewRepo.save(review);
    }
}
