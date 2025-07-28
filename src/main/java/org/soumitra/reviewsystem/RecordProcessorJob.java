package org.soumitra.reviewsystem;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
import org.soumitra.reviewsystem.util.HotelReviewJsonParser;

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
    private final HotelReviewJsonParser parser;

    public RecordProcessorJob (JobRunRepository jobRepo,  
        RecordRepository recordRepo, RecordErrorRepository recordErrorRepo, 
        ReviewRepository reviewRepo, HotelRepository hotelRepo, 
        ProviderRepository providerRepo, ReviewerRepository reviewerRepo,
        HotelReviewJsonParser parser, int pageSize) {
        this.jobRepo = jobRepo;
        this.recordRepo = recordRepo;
        this.recordErrorRepo = recordErrorRepo;
        this.reviewRepo = reviewRepo;
        this.hotelRepo = hotelRepo;
        this.providerRepo = providerRepo;
        this.reviewerRepo = reviewerRepo;
        this.parser = parser;
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
                    recordRepo.updateRecordStatus(record.getId(), "processing");
                    processRecord(record.getRawData());
                    recordRepo.updateRecordStatus(record.getId(), "success");
                    System.out.println("Successfully processed record ID: " + record.getId());
                } catch (Exception recEx) {
                    String errorMessage = recEx.getMessage();
                    String traceback = getStackTrace(recEx);
                    
                    System.err.println("Failed to process record ID: " + record.getId());
                    System.err.println("Error: " + errorMessage);
                    
                    recordRepo.updateRecordStatus(record.getId(), "failed", errorMessage);
                    recordErrorRepo.logRecordError(record, errorMessage, traceback);
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
        HotelReviewJsonParser.HotelReviewParseResult hotelReview =
            this.parser.parseHotelReview(jsonLine);
        
        System.out.println("Processing record: " + jsonLine);
        
        // Extract and upsert provider first (hotels depend on providers)
        Provider provider = upsertProviderFromDto(hotelReview.getProvider());
        
        // Extract and upsert hotel (now with provider relationship)
        Hotel hotel = upsertHotelFromDto(hotelReview.getHotel(), provider);
        
        // Extract and upsert reviewer
        Reviewer reviewer = upsertReviewerFromDto(hotelReview.getReviewer());
        
        // Extract and upsert review
        upsertReviewFromDto(hotelReview.getReview(), hotel, provider, reviewer);
    }
    
    /**
     * Upsert provider from DTO
     */
    private Provider upsertProviderFromDto(org.soumitra.reviewsystem.dto.ProviderDto providerDto) {
        return providerRepo.findByExternalId(providerDto.getExternalId())
            .orElseGet(() -> {
                Provider newProvider = Provider.builder()
                    .externalId(providerDto.getExternalId())
                    .providerName(providerDto.getProviderName())
                    .build();
                System.out.println("Creating new provider: " + newProvider.getProviderName());
                return providerRepo.save(newProvider);
            });
    }

    /**
     * Upsert hotel from DTO
     */
    private Hotel upsertHotelFromDto(org.soumitra.reviewsystem.dto.HotelDto hotelDto, Provider provider) {
        return hotelRepo.findByExternalIdAndProvider(hotelDto.getExternalId(), provider)
            .orElseGet(() -> {
                Hotel newHotel = Hotel.builder()
                    .externalId(hotelDto.getExternalId())
                    .provider(provider)
                    .hotelName(hotelDto.getHotelName())
                    .build();
                System.out.println("Creating new hotel: " + newHotel);
                return hotelRepo.save(newHotel);
            });
    }

    /**
     * Upsert reviewer from DTO
     */
    private Reviewer upsertReviewerFromDto(org.soumitra.reviewsystem.dto.ReviewerDto reviewerDto) {
        return reviewerRepo.findByDisplayNameAndCountryName(reviewerDto.getDisplayName(), reviewerDto.getCountryName())
            .orElseGet(() -> {
                Reviewer newReviewer = Reviewer.builder()
                    .displayName(reviewerDto.getDisplayName())
                    .countryName(reviewerDto.getCountryName())
                    .countryId(reviewerDto.getCountryId())
                    .flagCode(reviewerDto.getFlagCode())
                    .isExpert(reviewerDto.getIsExpert())
                    .reviewsWritten(reviewerDto.getReviewsWritten())
                    .build();
                System.out.println("Creating new reviewer: " + newReviewer.getDisplayName());
                return reviewerRepo.save(newReviewer);
            });
    }

    /**
     * Upsert review from DTO
     */
    private void upsertReviewFromDto(org.soumitra.reviewsystem.dto.ReviewDto reviewDto, Hotel hotel, Provider provider, Reviewer reviewer) {
        if (reviewRepo.existsByReviewExternalId(reviewDto.getReviewExternalId())) {
            System.out.println("Review already exists, skipping: " + reviewDto.getReviewExternalId());
            return;
        }
        
        Review newReview = Review.builder()
            .reviewExternalId(reviewDto.getReviewExternalId())
            .hotel(hotel)
            .provider(provider)
            .reviewer(reviewer)
            .ratingRaw(reviewDto.getRatingRaw())
            .ratingText(reviewDto.getRatingText())
            .ratingFormatted(reviewDto.getRatingFormatted())
            .reviewTitle(reviewDto.getReviewTitle())
            .reviewComment(reviewDto.getReviewComment())
            .reviewVotePositive(reviewDto.getReviewVotePositive())
            .reviewVoteNegative(reviewDto.getReviewVoteNegative())
            .reviewDate(reviewDto.getReviewDate())
            .translateSource(reviewDto.getTranslateSource())
            .translateTarget(reviewDto.getTranslateTarget())
            .isResponseShown(reviewDto.getIsResponseShown())
            .responderName(reviewDto.getResponderName())
            .responseText(reviewDto.getResponseText())
            .responseDateText(reviewDto.getResponseDateText())
            .responseDateFmt(reviewDto.getResponseDateFmt())
            .checkInMonthYr(reviewDto.getCheckInMonthYr())
            .build();
            
        System.out.println("Creating new review: " + reviewDto.getReviewExternalId());
        reviewRepo.save(newReview);
    }
    
    /**
     * Get stack trace as string
     */
    private String getStackTrace(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * Extract hotel ID from JSON
     */
    private Integer extractHotelId(JsonNode jsonNode) {
        // Check for flattened structure first (hotelId at root level)
        if (jsonNode.has("hotelId")) {
            return jsonNode.get("hotelId").asInt();
        }
        
        // Fallback to nested structure
        JsonNode hotelNode = jsonNode.get("hotel");
        if (hotelNode != null && hotelNode.has("hotel_id")) {
            return hotelNode.get("hotel_id").asInt();
        }
        
        return null;
    }
    
    /**
     * Extract hotel name from JSON
     */
    private String extractHotelName(JsonNode jsonNode) {
        // Check for flattened structure first (hotelName at root level)
        if (jsonNode.has("hotelName")) {
            return jsonNode.get("hotelName").asText();
        }
        
        // Fallback to nested structure
        JsonNode hotelNode = jsonNode.get("hotel");
        if (hotelNode != null && hotelNode.has("hotel_name")) {
            return hotelNode.get("hotel_name").asText();
        }
        
        return null;
    }
    
    /**
     * Upsert provider data - handles flattened JSON structure
     */
    private Provider upsertProvider(JsonNode jsonNode) {
        // Try different possible locations for provider data
        Short providerExternalId = extractProviderId(jsonNode);
        String providerName = extractProviderName(jsonNode);
        
        if (providerExternalId == null || providerName == null) {
            throw new RuntimeException("Provider ID or name is missing. Available fields: " + jsonNode.fieldNames());
        }
        
        System.out.println("Upserting provider: ExternalID=" + providerExternalId + ", Name=" + providerName);
        
        // Check if provider exists by external ID
        return providerRepo.findByExternalId(providerExternalId)
            .orElseGet(() -> {
                Provider newProvider = Provider.builder()
                    .externalId(providerExternalId)
                    .providerName(providerName)
                    .build();
                System.out.println("Creating new provider: " + newProvider);
                return providerRepo.save(newProvider);
            });
    }
    
    /**
     * Extract provider ID from JSON
     */
    private Short extractProviderId(JsonNode jsonNode) {
        // Check for provider info in comment section
        JsonNode commentNode = jsonNode.get("comment");
        if (commentNode != null && commentNode.has("providerId")) {
            return commentNode.get("providerId").shortValue();
        }
        
        // Fallback to nested structure
        JsonNode providerNode = jsonNode.get("provider");
        if (providerNode != null && providerNode.has("provider_id")) {
            return providerNode.get("provider_id").shortValue();
        }
        
        return null;
    }
    
    /**
     * Extract provider name from JSON
     */
    private String extractProviderName(JsonNode jsonNode) {
        // Check for platform field at root level
        if (jsonNode.has("platform")) {
            return jsonNode.get("platform").asText();
        }
        
        // Check for provider info in comment section
        JsonNode commentNode = jsonNode.get("comment");
        if (commentNode != null && commentNode.has("reviewProviderText")) {
            return commentNode.get("reviewProviderText").asText();
        }
        
        // Fallback to nested structure
        JsonNode providerNode = jsonNode.get("provider");
        if (providerNode != null && providerNode.has("provider_name")) {
            return providerNode.get("provider_name").asText();
        }
        
        return null;
    }
    
    /**
     * Upsert reviewer data - handles flattened JSON structure
     */
    private Reviewer upsertReviewer(JsonNode jsonNode) {
        // Try to find reviewer info in comment section
        JsonNode commentNode = jsonNode.get("comment");
        if (commentNode == null) {
            throw new RuntimeException("Comment section is missing");
        }
        
        JsonNode reviewerInfoNode = commentNode.get("reviewerInfo");
        if (reviewerInfoNode == null) {
            throw new RuntimeException("Reviewer info is missing");
        }
        
        System.out.println("Upserting reviewer: " + reviewerInfoNode);
        
        String displayName = reviewerInfoNode.get("displayMemberName") != null ? 
            reviewerInfoNode.get("displayMemberName").asText() : null;
        String countryName = reviewerInfoNode.get("countryName") != null ? 
            reviewerInfoNode.get("countryName").asText() : null;
        Integer countryId = reviewerInfoNode.get("countryId") != null ? 
            reviewerInfoNode.get("countryId").asInt() : null;
        String flagCode = reviewerInfoNode.get("flagName") != null ? 
            reviewerInfoNode.get("flagName").asText() : null;
        Boolean isExpert = reviewerInfoNode.get("isExpertReviewer") != null ? 
            reviewerInfoNode.get("isExpertReviewer").asBoolean() : null;
        Integer reviewsWritten = reviewerInfoNode.get("reviewerReviewedCount") != null ? 
            reviewerInfoNode.get("reviewerReviewedCount").asInt() : null;
            
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
                System.out.println("Creating new reviewer: " + newReviewer);
                return reviewerRepo.save(newReviewer);
            });
    }
    
    /**
     * Upsert review data - handles flattened JSON structure
     */
    private void upsertReview(JsonNode jsonNode, Hotel hotel, Provider provider, Reviewer reviewer) {
        JsonNode commentNode = jsonNode.get("comment");
        if (commentNode == null) {
            throw new RuntimeException("Comment section is missing");
        }
        
        Long reviewExternalId = commentNode.get("hotelReviewId") != null ? 
            commentNode.get("hotelReviewId").asLong() : null;
            
        if (reviewExternalId == null) {
            throw new RuntimeException("Review ID is missing");
        }
        
        // Check if review already exists
        if (reviewRepo.existsByReviewExternalId(reviewExternalId)) {
            System.out.println("Review already exists, skipping: " + reviewExternalId);
            return; // Skip if review already exists
        }
        
        // Extract review data from comment section
        Double ratingRaw = commentNode.get("rating") != null ? 
            commentNode.get("rating").asDouble() : null;
        String ratingText = commentNode.get("ratingText") != null ? 
            commentNode.get("ratingText").asText() : null;
        String ratingFormatted = commentNode.get("formattedRating") != null ? 
            commentNode.get("formattedRating").asText() : null;
        String reviewTitle = commentNode.get("reviewTitle") != null ? 
            commentNode.get("reviewTitle").asText() : null;
        String reviewComment = commentNode.get("reviewComments") != null ? 
            commentNode.get("reviewComments").asText() : null;
        String translateSource = commentNode.get("translateSource") != null ? 
            commentNode.get("translateSource").asText() : null;
        String translateTarget = commentNode.get("translateTarget") != null ? 
            commentNode.get("translateTarget").asText() : null;
        Boolean isResponseShown = commentNode.get("isShowReviewResponse") != null ? 
            commentNode.get("isShowReviewResponse").asBoolean() : null;
        String responderName = commentNode.get("responderName") != null ? 
            commentNode.get("responderName").asText() : null;
        String responseText = commentNode.get("originalComment") != null ? 
            commentNode.get("originalComment").asText() : null;
        String responseDateText = commentNode.get("responseDateText") != null ? 
            commentNode.get("responseDateText").asText() : null;
        String responseDateFmt = commentNode.get("formattedResponseDate") != null ? 
            commentNode.get("formattedResponseDate").asText() : null;
        String checkInMonthYr = commentNode.get("checkInDateMonthAndYear") != null ? 
            commentNode.get("checkInDateMonthAndYear").asText() : null;
            
        // Parse review date
        OffsetDateTime reviewDate = null;
        if (commentNode.get("reviewDate") != null) {
            try {
                String reviewDateStr = commentNode.get("reviewDate").asText();
                reviewDate = OffsetDateTime.parse(reviewDateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            } catch (Exception e) {
                // Log warning but continue
                System.out.println("Warning: Could not parse review date: " + commentNode.get("reviewDate").asText());
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
            .reviewVotePositive(null) // Not available in this structure
            .reviewVoteNegative(null) // Not available in this structure
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
            
        System.out.println("Creating new review: " + reviewExternalId);
        reviewRepo.save(review);
    }
}
