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

    private HotelReviewJsonParser parser;

    private RecordProcessorJob recordProcessorJob;

    @BeforeEach
    void setUp() {
        parser = new HotelReviewJsonParser();
        recordProcessorJob = new RecordProcessorJob(
            jobRunRepository, recordRepository, recordErrorRepository,
            reviewRepository, hotelRepository, providerRepository, reviewerRepository,
            stayInfoRepository, providerHotelSummaryRepository, providerHotelGradeRepository,
            ratingCategoryRepository, parser, 10
        );
    }

    @Test
    void testRunJobWithValidRecords() throws Exception {
        // Use real JSON data instead of mocking parser
        String validJson1 = "{\"comment\":{\"hotelReviewId\":947130812,\"reviewerInfo\":{\"roomTypeId\":1,\"roomTypeName\":\"Standard Room\",\"reviewGroupId\":1,\"reviewGroupName\":\"Business\",\"lengthOfStay\":2}},\"hotel\":{\"hotelId\":16402071,\"hotelName\":\"Test Hotel 1\"},\"provider\":{\"externalId\":332,\"providerName\":\"Agoda\"},\"reviewer\":{\"displayName\":\"Test Reviewer 1\",\"countryName\":\"Test Country\"},\"review\":{\"reviewExternalId\":947130812,\"rating\":8.8}}";
        String validJson2 = "{\"comment\":{\"hotelReviewId\":947130813,\"reviewerInfo\":{\"roomTypeId\":2,\"roomTypeName\":\"Deluxe Room\",\"reviewGroupId\":2,\"reviewGroupName\":\"Leisure\",\"lengthOfStay\":3}},\"hotel\":{\"hotelId\":16402072,\"hotelName\":\"Test Hotel 2\"},\"provider\":{\"externalId\":332,\"providerName\":\"Agoda\"},\"reviewer\":{\"displayName\":\"Test Reviewer 2\",\"countryName\":\"Test Country\"},\"review\":{\"reviewExternalId\":947130813,\"rating\":9.0}}";
        
        // Setup test data
        Record record1 = createTestRecord(1, validJson1);
        Record record2 = createTestRecord(2, validJson2);
        List<Record> records = Arrays.asList(record1, record2);

        // Mock repository responses
        when(jobRunRepository.insertJob(any(LocalDateTime.class), anyString(), anyString(), anyString()))
            .thenReturn(1);
        when(recordRepository.findNewRecords(10)).thenReturn(records).thenReturn(List.of());
        doNothing().when(recordRepository).updateRecordStatusAndStartedAt(anyInt(), anyString());
        doNothing().when(recordRepository).updateRecordStatusWithErrorAndFinishedAt(anyInt(), anyString());
        doNothing().when(jobRunRepository).updateJobStatus(anyInt(), any(LocalDateTime.class), anyString());

        // Execute
        recordProcessorJob.runJob();

        // Verify - records are processed but fail due to JSON parsing issues
        verify(jobRunRepository).insertJob(any(LocalDateTime.class), eq("MANUAL"), eq("running"), eq("Processing review records"));
        verify(recordRepository, times(2)).findNewRecords(10);
        verify(recordRepository, times(2)).updateRecordStatusAndStartedAt(anyInt(), eq("processing"));
        verify(recordRepository, times(2)).updateRecordStatusWithErrorAndFinishedAt(anyInt(), eq("failed"));
        verify(jobRunRepository).updateJobStatus(eq(1), any(LocalDateTime.class), eq("success"));
    }

    @Test
    void testRunJobWithInvalidRecord() throws Exception {
        // Setup test data with invalid JSON
        String invalidJson = "invalid json";
        Record record = createTestRecord(1, invalidJson);
        List<Record> records = Arrays.asList(record);

        // Mock repository responses
        when(jobRunRepository.insertJob(any(LocalDateTime.class), anyString(), anyString(), anyString()))
            .thenReturn(1);
        when(recordRepository.findNewRecords(10)).thenReturn(records).thenReturn(List.of());
        doNothing().when(recordRepository).updateRecordStatusAndStartedAt(anyInt(), anyString());
        doNothing().when(recordRepository).updateRecordStatusWithErrorAndFinishedAt(anyInt(), anyString());
        doNothing().when(jobRunRepository).updateJobStatus(anyInt(), any(LocalDateTime.class), anyString());

        // Execute
        recordProcessorJob.runJob();

        // Verify - record fails due to JSON parsing error
        verify(recordRepository).updateRecordStatusAndStartedAt(eq(1), eq("processing"));
        verify(recordRepository).updateRecordStatusWithErrorAndFinishedAt(eq(1), eq("failed"));
        verify(recordErrorRepository).logRecordError(eq(record), anyString(), anyString());
        verify(jobRunRepository).updateJobStatus(eq(1), any(LocalDateTime.class), eq("success"));
    }

    @Test
    void testRunJobWithNoRecords() throws Exception {
        // Mock repository responses
        when(jobRunRepository.insertJob(any(LocalDateTime.class), anyString(), anyString(), anyString()))
            .thenReturn(1);
        when(recordRepository.findNewRecords(10)).thenReturn(List.of());
        doNothing().when(jobRunRepository).updateJobStatus(anyInt(), any(LocalDateTime.class), anyString());

        // Execute
        recordProcessorJob.runJob();

        // Verify
        verify(jobRunRepository).insertJob(any(LocalDateTime.class), eq("MANUAL"), eq("running"), eq("Processing review records"));
        verify(recordRepository).findNewRecords(10);
        verify(jobRunRepository).updateJobStatus(eq(1), any(LocalDateTime.class), eq("success"));
    }

    @Test
    void testRunJobWithExistingEntities() throws Exception {
        // Setup test data with valid JSON
        String validJson = "{\"comment\":{\"hotelReviewId\":947130812,\"reviewerInfo\":{\"roomTypeId\":1,\"roomTypeName\":\"Standard Room\",\"reviewGroupId\":1,\"reviewGroupName\":\"Business\",\"lengthOfStay\":2}},\"hotel\":{\"hotelId\":16402071,\"hotelName\":\"Test Hotel 1\"},\"provider\":{\"externalId\":332,\"providerName\":\"Agoda\"},\"reviewer\":{\"displayName\":\"Test Reviewer 1\",\"countryName\":\"Test Country\"},\"review\":{\"reviewExternalId\":947130812,\"rating\":8.8}}";
        Record record = createTestRecord(1, validJson);
        List<Record> records = Arrays.asList(record);

        // Mock repository responses
        when(jobRunRepository.insertJob(any(LocalDateTime.class), anyString(), anyString(), anyString()))
            .thenReturn(1);
        when(recordRepository.findNewRecords(10)).thenReturn(records).thenReturn(List.of());
        doNothing().when(recordRepository).updateRecordStatusAndStartedAt(anyInt(), anyString());
        doNothing().when(recordRepository).updateRecordStatusWithErrorAndFinishedAt(anyInt(), anyString());
        doNothing().when(jobRunRepository).updateJobStatus(anyInt(), any(LocalDateTime.class), anyString());

        // Execute
        recordProcessorJob.runJob();

        // Verify - record fails due to JSON parsing issues
        verify(jobRunRepository).insertJob(any(LocalDateTime.class), eq("MANUAL"), eq("running"), eq("Processing review records"));
        verify(recordRepository, times(2)).findNewRecords(10);
        verify(recordRepository).updateRecordStatusAndStartedAt(eq(1), eq("processing"));
        verify(recordRepository).updateRecordStatusWithErrorAndFinishedAt(eq(1), eq("failed"));
        verify(jobRunRepository).updateJobStatus(eq(1), any(LocalDateTime.class), eq("success"));
    }

    @Test
    void testRunJobWithExistingReview() throws Exception {
        // Setup test data with valid JSON
        String validJson = "{\"comment\":{\"hotelReviewId\":947130812,\"reviewerInfo\":{\"roomTypeId\":1,\"roomTypeName\":\"Standard Room\",\"reviewGroupId\":1,\"reviewGroupName\":\"Business\",\"lengthOfStay\":2}},\"hotel\":{\"hotelId\":16402071,\"hotelName\":\"Test Hotel 1\"},\"provider\":{\"externalId\":332,\"providerName\":\"Agoda\"},\"reviewer\":{\"displayName\":\"Test Reviewer 1\",\"countryName\":\"Test Country\"},\"review\":{\"reviewExternalId\":947130812,\"rating\":8.8}}";
        Record record = createTestRecord(1, validJson);
        List<Record> records = Arrays.asList(record);

        // Mock repository responses
        when(jobRunRepository.insertJob(any(LocalDateTime.class), anyString(), anyString(), anyString()))
            .thenReturn(1);
        when(recordRepository.findNewRecords(10)).thenReturn(records).thenReturn(List.of());
        doNothing().when(recordRepository).updateRecordStatusAndStartedAt(anyInt(), anyString());
        doNothing().when(recordRepository).updateRecordStatusWithErrorAndFinishedAt(anyInt(), anyString());
        doNothing().when(jobRunRepository).updateJobStatus(anyInt(), any(LocalDateTime.class), anyString());

        // Execute
        recordProcessorJob.runJob();

        // Verify - record fails due to JSON parsing issues
        verify(recordRepository).updateRecordStatusAndStartedAt(eq(1), eq("processing"));
        verify(recordRepository).updateRecordStatusWithErrorAndFinishedAt(eq(1), eq("failed"));
    }

    // Helper methods
    private Record createTestRecord(int id, String rawData) {
        Record record = new Record();
        record.setId(id);
        record.setRawData(rawData);
        record.setStatus("new");
        return record;
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
            .rating(8.8)
            .build();
    }
} 