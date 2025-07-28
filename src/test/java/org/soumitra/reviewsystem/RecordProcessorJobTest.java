package org.soumitra.reviewsystem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.soumitra.reviewsystem.dao.*;
import org.soumitra.reviewsystem.model.*;
import org.soumitra.reviewsystem.model.Record;
import org.soumitra.reviewsystem.util.HotelReviewJsonParser;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class RecordProcessorJobTest {

    @Mock
    private JobRunRepository jobRunRepository;

    @Mock
    private RecordRepository recordRepository;

    @Mock
    private RecordErrorRepository recordErrorRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private ReviewerRepository reviewerRepository;

    @Mock
    private org.soumitra.reviewsystem.dao.StayInfoRepository stayInfoRepository;

    @Mock
    private org.soumitra.reviewsystem.dao.ProviderHotelSummaryRepository providerHotelSummaryRepository;

    @Mock
    private org.soumitra.reviewsystem.dao.ProviderHotelGradeRepository providerHotelGradeRepository;

    @Mock
    private org.soumitra.reviewsystem.dao.RatingCategoryRepository ratingCategoryRepository;

    @Mock
    private HotelReviewJsonParser parser;

    private RecordProcessorJob recordProcessorJob;

    @BeforeEach
    void setUp() {
        recordProcessorJob = new RecordProcessorJob(
            jobRunRepository, recordRepository, recordErrorRepository,
            reviewRepository, hotelRepository, providerRepository, reviewerRepository,
            stayInfoRepository, providerHotelSummaryRepository, providerHotelGradeRepository,
            ratingCategoryRepository, parser, 10
        );
    }

    @Test
    void testRunJobWithValidRecords() throws Exception {
        // Setup test data
        Record record1 = createTestRecord(1, "valid json 1");
        Record record2 = createTestRecord(2, "valid json 2");
        List<Record> records = Arrays.asList(record1, record2);

        // Mock repository responses
        when(jobRunRepository.insertJob(any(LocalDateTime.class), anyString(), anyString(), anyString()))
            .thenReturn(1);
        when(recordRepository.findNewRecords(10)).thenReturn(records).thenReturn(List.of());
        doNothing().when(recordRepository).updateRecordStatus(anyInt(), anyString());
        doNothing().when(recordRepository).updateRecordStatus(anyInt(), anyString(), anyString());
        when(jobRunRepository.updateJobStatus(anyInt(), any(LocalDateTime.class), anyString()))
            .thenReturn(1);

        // Mock parser responses
        when(parser.parseHotelReview("valid json 1")).thenReturn(createMockParseResult());
        when(parser.parseHotelReview("valid json 2")).thenReturn(createMockParseResult());

        // Mock entity repositories
        when(providerRepository.findByExternalId(any())).thenReturn(Optional.empty());
        when(providerRepository.save(any(Provider.class))).thenReturn(createMockProvider());
        when(hotelRepository.findByExternalIdAndProvider(anyInt(), any(Provider.class))).thenReturn(Optional.empty());
        when(hotelRepository.save(any(Hotel.class))).thenReturn(createMockHotel());
        when(reviewerRepository.findByDisplayNameAndCountryNameAndProvider(anyString(), anyString(), any(Provider.class))).thenReturn(Optional.empty());
        when(reviewerRepository.save(any(Reviewer.class))).thenReturn(createMockReviewer());
        when(reviewRepository.existsByReviewExternalId(anyLong())).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(createMockReview());

        // Execute
        recordProcessorJob.runJob();

        // Verify
        verify(jobRunRepository).insertJob(any(LocalDateTime.class), eq("MANUAL"), eq("running"), eq("Processing review records"));
        verify(recordRepository, times(2)).findNewRecords(10);
        verify(recordRepository, times(2)).updateRecordStatus(anyInt(), eq("processing"));
        verify(recordRepository, times(2)).updateRecordStatus(anyInt(), eq("success"));
        verify(jobRunRepository).updateJobStatus(eq(1), any(LocalDateTime.class), eq("success"));
        verify(parser, times(2)).parseHotelReview(anyString());
    }

    @Test
    void testRunJobWithInvalidRecord() throws Exception {
        // Setup test data
        Record record = createTestRecord(1, "invalid json");
        List<Record> records = Arrays.asList(record);

        // Mock repository responses
        when(jobRunRepository.insertJob(any(LocalDateTime.class), anyString(), anyString(), anyString()))
            .thenReturn(1);
        when(recordRepository.findNewRecords(10)).thenReturn(records).thenReturn(List.of());
        doNothing().when(recordRepository).updateRecordStatus(anyInt(), anyString());
        doNothing().when(recordRepository).updateRecordStatus(anyInt(), anyString(), anyString());
        when(jobRunRepository.updateJobStatus(anyInt(), any(LocalDateTime.class), anyString()))
            .thenReturn(1);

        // Mock parser to throw exception
        when(parser.parseHotelReview("invalid json")).thenThrow(new RuntimeException("Parsing failed"));

        // Execute
        recordProcessorJob.runJob();

        // Verify
        verify(recordRepository).updateRecordStatus(1, "processing");
        verify(recordRepository).updateRecordStatus(1, "failed", "Parsing failed");
        verify(recordErrorRepository).logRecordError(eq(record), eq("Parsing failed"), anyString());
        verify(jobRunRepository).updateJobStatus(eq(1), any(LocalDateTime.class), eq("success"));
    }

    @Test
    void testRunJobWithNoRecords() throws Exception {
        // Mock repository responses
        when(jobRunRepository.insertJob(any(LocalDateTime.class), anyString(), anyString(), anyString()))
            .thenReturn(1);
        when(recordRepository.findNewRecords(10)).thenReturn(List.of());
        when(jobRunRepository.updateJobStatus(anyInt(), any(LocalDateTime.class), anyString()))
            .thenReturn(1);

        // Execute
        recordProcessorJob.runJob();

        // Verify
        verify(jobRunRepository).insertJob(any(LocalDateTime.class), eq("MANUAL"), eq("running"), eq("Processing review records"));
        verify(recordRepository).findNewRecords(10);
        verify(jobRunRepository).updateJobStatus(eq(1), any(LocalDateTime.class), eq("success"));
        verify(parser, never()).parseHotelReview(anyString());
    }

    @Test
    void testRunJobWithExistingEntities() throws Exception {
        // Setup test data
        Record record = createTestRecord(1, "valid json");
        List<Record> records = Arrays.asList(record);

        // Mock repository responses
        when(jobRunRepository.insertJob(any(LocalDateTime.class), anyString(), anyString(), anyString()))
            .thenReturn(1);
        when(recordRepository.findNewRecords(10)).thenReturn(records).thenReturn(List.of());
        doNothing().when(recordRepository).updateRecordStatus(anyInt(), anyString());
        doNothing().when(recordRepository).updateRecordStatus(anyInt(), anyString(), anyString());
        when(jobRunRepository.updateJobStatus(anyInt(), any(LocalDateTime.class), anyString()))
            .thenReturn(1);

        // Mock parser responses
        when(parser.parseHotelReview("valid json")).thenReturn(createMockParseResult());

        // Mock existing entities
        Provider existingProvider = createMockProvider();
        Hotel existingHotel = createMockHotel();
        Reviewer existingReviewer = createMockReviewer();

        when(providerRepository.findByExternalId(any())).thenReturn(Optional.of(existingProvider));
        when(hotelRepository.findByExternalIdAndProvider(anyInt(), any(Provider.class))).thenReturn(Optional.of(existingHotel));
        when(reviewerRepository.findByDisplayNameAndCountryNameAndProvider(anyString(), anyString(), any(Provider.class))).thenReturn(Optional.of(existingReviewer));
        when(reviewRepository.existsByReviewExternalId(anyLong())).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(createMockReview());

        // Execute
        recordProcessorJob.runJob();

        // Verify
        verify(jobRunRepository).insertJob(any(LocalDateTime.class), eq("MANUAL"), eq("running"), eq("Processing review records"));
        verify(recordRepository).findNewRecords(10);
        verify(recordRepository).updateRecordStatus(1, "processing");
        verify(recordRepository).updateRecordStatus(1, "success");
        verify(providerRepository).findByExternalId(any());
        verify(providerRepository, never()).save(any(Provider.class));
        verify(hotelRepository).findByExternalIdAndProvider(anyInt(), any(Provider.class));
        verify(hotelRepository, never()).save(any(Hotel.class));
        verify(reviewerRepository).findByDisplayNameAndCountryNameAndProvider(anyString(), anyString(), any(Provider.class));
        verify(reviewerRepository, never()).save(any(Reviewer.class));
        verify(reviewRepository).save(any(Review.class));
        verify(jobRunRepository).updateJobStatus(eq(1), any(LocalDateTime.class), eq("success"));
    }

    @Test
    void testRunJobWithExistingReview() throws Exception {
        // Setup test data
        Record record = createTestRecord(1, "valid json");
        List<Record> records = Arrays.asList(record);

        // Mock repository responses
        when(jobRunRepository.insertJob(any(LocalDateTime.class), anyString(), anyString(), anyString()))
            .thenReturn(1);
        when(recordRepository.findNewRecords(10)).thenReturn(records).thenReturn(List.of());
        doNothing().when(recordRepository).updateRecordStatus(anyInt(), anyString());
        doNothing().when(recordRepository).updateRecordStatus(anyInt(), anyString(), anyString());
        when(jobRunRepository.updateJobStatus(anyInt(), any(LocalDateTime.class), anyString()))
            .thenReturn(1);

        // Mock parser responses
        when(parser.parseHotelReview("valid json")).thenReturn(createMockParseResult());

        // Mock existing entities
        Provider existingProvider = createMockProvider();
        Hotel existingHotel = createMockHotel();
        Reviewer existingReviewer = createMockReviewer();

        when(providerRepository.findByExternalId(any())).thenReturn(Optional.of(existingProvider));
        when(hotelRepository.findByExternalIdAndProvider(anyInt(), any(Provider.class))).thenReturn(Optional.of(existingHotel));
        when(reviewerRepository.findByDisplayNameAndCountryNameAndProvider(anyString(), anyString(), any(Provider.class))).thenReturn(Optional.of(existingReviewer));
        when(reviewRepository.existsByReviewExternalId(anyLong())).thenReturn(true);

        // Execute
        recordProcessorJob.runJob();

        // Verify
        verify(reviewRepository).existsByReviewExternalId(anyLong());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    // Helper methods
    private Record createTestRecord(int id, String rawData) {
        Record record = new Record();
        record.setId(id);
        record.setRawData(rawData);
        record.setStatus("new");
        return record;
    }

    private HotelReviewJsonParser.HotelReviewParseResult createMockParseResult() {
        return HotelReviewJsonParser.HotelReviewParseResult.builder()
            .provider(org.soumitra.reviewsystem.dto.ProviderDto.builder()
                .externalId((short) 332)
                .providerName("Agoda")
                .build())
            .hotel(org.soumitra.reviewsystem.dto.HotelDto.builder()
                .externalId(16402071)
                .hotelName("Test Hotel")
                .build())
            .reviewer(org.soumitra.reviewsystem.dto.ReviewerDto.builder()
                .displayName("Test Reviewer")
                .countryName("Test Country")
                .build())
            .review(org.soumitra.reviewsystem.dto.ReviewDto.builder()
                .reviewExternalId(947130812L)
                .ratingRaw(8.8)
                .build())
            .build();
    }

    private Provider createMockProvider() {
        return Provider.builder()
            .providerId((short) 1)
            .externalId((short) 332)
            .providerName("Agoda")
            .build();
    }

    private Hotel createMockHotel() {
        return Hotel.builder()
            .hotelId(1)
            .externalId(16402071)
            .hotelName("Test Hotel")
            .provider(createMockProvider())
            .build();
    }

    private Reviewer createMockReviewer() {
        return Reviewer.builder()
            .reviewerId(1L)
            .displayName("Test Reviewer")
            .countryName("Test Country")
            .build();
    }

    private Review createMockReview() {
        return Review.builder()
            .reviewId(1L)
            .reviewExternalId(947130812L)
            .hotel(createMockHotel())
            .provider(createMockProvider())
            .reviewer(createMockReviewer())
            .ratingRaw(8.8)
            .build();
    }
} 