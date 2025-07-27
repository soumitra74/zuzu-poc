package org.soumitra.reviewsystem;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

// Repository interfaces
import org.soumitra.reviewsystem.dao.JobRunRepository;
import org.soumitra.reviewsystem.dao.RecordRepository;
import org.soumitra.reviewsystem.dao.RecordErrorRepository;
import org.soumitra.reviewsystem.dao.ReviewRepository;
import org.soumitra.reviewsystem.dao.HotelRepository;
import org.soumitra.reviewsystem.dao.ProviderRepository;
import org.soumitra.reviewsystem.dao.ReviewerRepository;

// Model classes
import org.soumitra.reviewsystem.model.Record;
import org.soumitra.reviewsystem.model.Review;
import org.soumitra.reviewsystem.model.Hotel;
import org.soumitra.reviewsystem.model.Provider;
import org.soumitra.reviewsystem.model.Reviewer;

// JSON parsing
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RecordProcessorJob {

    private final JobRunRepository jobRepo;
    private final RecordRepository recordRepo;
    private final RecordErrorRepository recordErrorRepo;
    private final ReviewRepository reviewRepo;
    private final HotelRepository hotelRepo;
    private final ProviderRepository providerRepo;
    private final ReviewerRepository reviewerRepo;

    private final int pageSize;
    private final ObjectMapper objectMapper;

    public RecordProcessorJob (JobRunRepository jobRepo,  
        RecordRepository recordRepo, RecordErrorRepository recordErrorRepo, 
        ReviewRepository reviewRepo, HotelRepository hotelRepo, 
        ProviderRepository providerRepo, ReviewerRepository reviewerRepo,
        int pageSize) {
        this.jobRepo = jobRepo;
        this.recordRepo = recordRepo;
        this.recordErrorRepo = recordErrorRepo;
        this.reviewRepo = reviewRepo;
        this.hotelRepo = hotelRepo;
        this.providerRepo = providerRepo;
        this.reviewerRepo = reviewerRepo;
        this.pageSize = pageSize > 0 ? pageSize : 10;
        this.objectMapper = new ObjectMapper();
    }

    public void runJob() {
        // Create a new job run
        Integer jobId = jobRepo.insertJob(LocalDateTime.now(), "MANUAL", "running", "Processing review records");

        int totalRecordsProcessed = 0;

        //Read new records from the record table in the order of their creation, using pageSize
        List<Record> records = recordRepo.findNewRecords(pageSize);

        while(records.size() > 0) {
            for (Record record : records) {
                try {
                    processRecord(record.getRawData());
                    recordRepo.updateRecordStatus(record.getId(), "processed");
                } catch (Exception recEx) {
                    recEx.printStackTrace();
                    recordRepo.updateRecordStatus(record.getId(), "failed", recEx.getMessage());
                    recordErrorRepo.logRecordError(record, recEx.getMessage());
                } finally {
                    totalRecordsProcessed++;
                }
            }
            records = recordRepo.findNewRecords(pageSize);
        }

        System.out.println("Total records processed: " + totalRecordsProcessed);

        // Update job status
        jobRepo.updateJobStatus(jobId, LocalDateTime.now(), "success");
    }


    /**
     * Parse JSON review data and store in database
     * Implements upsert logic for reviews, hotels, providers, and reviewers
     */
    private void processRecord(String jsonLine) throws Exception {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonLine);
            
            System.out.println("Processing record: " + jsonLine);
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
        
        System.out.println("Upserting hotel: " + hotelNode);

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
                System.out.println("New hotel: " + newHotel);
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
        
        System.out.println("Upserting reviewer: " + reviewerNode);

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
