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
    
}
