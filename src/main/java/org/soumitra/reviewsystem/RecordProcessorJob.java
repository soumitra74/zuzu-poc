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
import org.soumitra.reviewsystem.dao.StayInfoRepository;
import org.soumitra.reviewsystem.dao.ProviderHotelSummaryRepository;
import org.soumitra.reviewsystem.dao.ProviderHotelGradeRepository;
import org.soumitra.reviewsystem.dao.RatingCategoryRepository;

// Model classes
import org.soumitra.reviewsystem.model.Record;
import org.soumitra.reviewsystem.model.Review;
import org.soumitra.reviewsystem.model.Hotel;
import org.soumitra.reviewsystem.model.Provider;
import org.soumitra.reviewsystem.model.Reviewer;
import org.soumitra.reviewsystem.model.StayInfo;
import org.soumitra.reviewsystem.model.ProviderHotelSummary;
import org.soumitra.reviewsystem.model.ProviderHotelGrade;
import org.soumitra.reviewsystem.model.RatingCategory;
import org.soumitra.reviewsystem.util.HotelReviewJsonParser;

public class RecordProcessorJob {

    private final JobRunRepository jobRepo;
    private final RecordRepository recordRepo;
    private final RecordErrorRepository recordErrorRepo;
    private final ReviewRepository reviewRepo;
    private final HotelRepository hotelRepo;
    private final ProviderRepository providerRepo;
    private final ReviewerRepository reviewerRepo;
    private final StayInfoRepository stayInfoRepo;
    private final ProviderHotelSummaryRepository providerHotelSummaryRepo;
    private final ProviderHotelGradeRepository providerHotelGradeRepo;
    private final RatingCategoryRepository ratingCategoryRepo;

    private final int pageSize;
    private final HotelReviewJsonParser parser;

    public RecordProcessorJob (JobRunRepository jobRepo,  
        RecordRepository recordRepo, RecordErrorRepository recordErrorRepo, 
        ReviewRepository reviewRepo, HotelRepository hotelRepo, 
        ProviderRepository providerRepo, ReviewerRepository reviewerRepo,
        StayInfoRepository stayInfoRepo, ProviderHotelSummaryRepository providerHotelSummaryRepo,
        ProviderHotelGradeRepository providerHotelGradeRepo, RatingCategoryRepository ratingCategoryRepo,
        HotelReviewJsonParser parser, int pageSize) {
        this.jobRepo = jobRepo;
        this.recordRepo = recordRepo;
        this.recordErrorRepo = recordErrorRepo;
        this.reviewRepo = reviewRepo;
        this.hotelRepo = hotelRepo;
        this.providerRepo = providerRepo;
        this.reviewerRepo = reviewerRepo;
        this.stayInfoRepo = stayInfoRepo;
        this.providerHotelSummaryRepo = providerHotelSummaryRepo;
        this.providerHotelGradeRepo = providerHotelGradeRepo;
        this.ratingCategoryRepo = ratingCategoryRepo;
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
                    // Update status to processing and set startedAt
                    recordRepo.updateRecordStatusAndStartedAt(record.getId(), "processing");
                    processRecord(record.getRawData());
                    // Update status to success and set finishedAt
                    recordRepo.updateRecordStatusAndFinishedAt(record.getId(), "success");
                    System.out.println("Successfully processed record ID: " + record.getId());
                } catch (Exception recEx) {
                    String errorMessage = recEx.getMessage();
                    String traceback = getStackTrace(recEx);
                    
                    System.err.println("Failed to process record ID: " + record.getId());
                    System.err.println("Error: " + errorMessage);
                    
                    // Update status to failed and set finishedAt with error flag
                    recordRepo.updateRecordStatusWithErrorAndFinishedAt(record.getId(), "failed");
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
        Reviewer reviewer = upsertReviewerFromDto(hotelReview.getReviewer(), provider);
            
        // Extract and upsert review
        upsertReviewFromDto(hotelReview.getReview(), hotel, provider, reviewer);

        upsertStayInfoFromDto(hotelReview.getStayInfo(), hotel, provider, reviewer);
        
        // Extract and upsert provider hotel summaries and grades
        upsertProviderHotelSummariesFromDto(hotelReview.getProviderHotelSummaries(), hotel, provider);
        upsertProviderHotelGradesFromDto(hotelReview.getProviderHotelGrades(), hotel, provider);
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
    private Reviewer upsertReviewerFromDto(org.soumitra.reviewsystem.dto.ReviewerDto reviewerDto, Provider provider) {
        return reviewerRepo.findByDisplayNameAndCountryNameAndProvider(reviewerDto.getDisplayName(), reviewerDto.getCountryName(), provider)
            .orElseGet(() -> {
                Reviewer newReviewer = Reviewer.builder()
                    .displayName(reviewerDto.getDisplayName())
                    .countryName(reviewerDto.getCountryName())
                    .countryId(reviewerDto.getCountryId())
                    .flagCode(reviewerDto.getFlagCode())
                    .isExpert(reviewerDto.getIsExpert())
                    .reviewsWritten(reviewerDto.getReviewsWritten())
                    .provider(provider) // Add provider relationship
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
            .rating(reviewDto.getRating())
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
     * Upsert stay info from DTO
     */
    private void upsertStayInfoFromDto(org.soumitra.reviewsystem.dto.StayInfoDto stayInfoDto, Hotel hotel, Provider provider, Reviewer reviewer) {
        if (stayInfoDto == null) {
            System.out.println("No stay info available, skipping");
            return;
        }
        
        // Find the review that was just created
        Review review = reviewRepo.findByReviewExternalId(stayInfoDto.getReviewId())
            .orElse(null);
            
        if (review == null) {
            System.out.println("Review not found for stay info, skipping: " + stayInfoDto.getReviewId());
            return;
        }
        
        // Check if stay info already exists for this review
        if (stayInfoRepo.existsByReviewId(review.getReviewId())) {
            System.out.println("Stay info already exists for review, skipping: " + review.getReviewId());
            return;
        }
        
        StayInfo newStayInfo = StayInfo.builder()
            .reviewId(review.getReviewId())
            .roomTypeId(stayInfoDto.getRoomTypeId())
            .roomTypeName(stayInfoDto.getRoomTypeName())
            .reviewGroupId(stayInfoDto.getReviewGroupId())
            .reviewGroupName(stayInfoDto.getReviewGroupName())
            .lengthOfStay(stayInfoDto.getLengthOfStay())
            .build();
            
        System.out.println("Creating new stay info for review: " + review.getReviewId());
        stayInfoRepo.save(newStayInfo);
    }
    
    /**
     * Upsert provider hotel summaries from DTO
     */
    private void upsertProviderHotelSummariesFromDto(List<org.soumitra.reviewsystem.dto.ProviderHotelSummaryDto> summaries, Hotel hotel, Provider provider) {
        if (summaries == null || summaries.isEmpty()) {
            System.out.println("No provider hotel summaries available, skipping");
            return;
        }
        
        for (org.soumitra.reviewsystem.dto.ProviderHotelSummaryDto summaryDto : summaries) {
            // Find the provider for this summary
            Provider summaryProvider = providerRepo.findByExternalId(summaryDto.getProviderId())
                .orElse(provider); // Fallback to the main provider if not found
            
            // Check if summary already exists
            if (providerHotelSummaryRepo.existsByHotelHotelIdAndProviderProviderId(hotel.getHotelId(), summaryProvider.getProviderId())) {
                System.out.println("Provider hotel summary already exists for hotel: " + hotel.getHotelId() + ", provider: " + summaryProvider.getProviderId());
                continue;
            }
            
            ProviderHotelSummary newSummary = ProviderHotelSummary.builder()
                .hotel(hotel)
                .provider(summaryProvider)
                .overallScore(summaryDto.getOverallScore())
                .reviewCount(summaryDto.getReviewCount())
                .build();
                
            System.out.println("Creating new provider hotel summary for hotel: " + hotel.getHotelId() + ", provider: " + summaryProvider.getProviderId());
            providerHotelSummaryRepo.save(newSummary);
        }
    }
    
    /**
     * Upsert provider hotel grades from DTO
     */
    private void upsertProviderHotelGradesFromDto(List<org.soumitra.reviewsystem.dto.ProviderHotelGradeDto> grades, Hotel hotel, Provider provider) {
        if (grades == null || grades.isEmpty()) {
            System.out.println("No provider hotel grades available, skipping");
            return;
        }
        
        for (org.soumitra.reviewsystem.dto.ProviderHotelGradeDto gradeDto : grades) {
            // Find the provider for this grade
            Provider gradeProvider = providerRepo.findByExternalId(gradeDto.getProviderId())
                .orElse(provider); // Fallback to the main provider if not found
            
            // Upsert the rating category
            RatingCategory category = upsertRatingCategoryFromName(gradeDto.getCategoryName());
            
            if (category == null) {
                System.out.println("Could not create rating category for: " + gradeDto.getCategoryName());
                continue;
            }
            
            // Check if grade already exists
            if (providerHotelGradeRepo.existsByHotelHotelIdAndProviderProviderIdAndCategoryCategoryId(
                hotel.getHotelId(), gradeProvider.getProviderId(), category.getCategoryId())) {
                System.out.println("Provider hotel grade already exists for hotel: " + hotel.getHotelId() + 
                    ", provider: " + gradeProvider.getProviderId() + ", category: " + category.getCategoryId());
                continue;
            }
            
            ProviderHotelGrade newGrade = ProviderHotelGrade.builder()
                .hotel(hotel)
                .provider(gradeProvider)
                .category(category)
                .gradeValue(gradeDto.getGradeValue())
                .build();
                
            System.out.println("Creating new provider hotel grade for hotel: " + hotel.getHotelId() + 
                ", provider: " + gradeProvider.getProviderId() + ", category: " + category.getCategoryName());
            providerHotelGradeRepo.save(newGrade);
        }
    }
    
    /**
     * Upsert rating category from name
     */
    private RatingCategory upsertRatingCategoryFromName(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return null;
        }
        
        return ratingCategoryRepo.findByCategoryName(categoryName)
            .orElseGet(() -> {
                RatingCategory newCategory = RatingCategory.builder()
                    .categoryName(categoryName)
                    .build();
                System.out.println("Creating new rating category: " + categoryName);
                return ratingCategoryRepo.save(newCategory);
            });
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
