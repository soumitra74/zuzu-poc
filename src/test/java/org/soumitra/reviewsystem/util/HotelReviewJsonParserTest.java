package org.soumitra.reviewsystem.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.soumitra.reviewsystem.dto.*;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class HotelReviewJsonParserTest {

    private HotelReviewJsonParser parser;

    @BeforeEach
    void setUp() {
        parser = new HotelReviewJsonParser();
    }

    @Test
    void testParseValidHotelReview() throws Exception {
        String validJson = """
            {
                "hotelId": 16402071,
                "platform": "Agoda",
                "hotelName": "Surfer's Point Deck",
                "comment": {
                    "isShowReviewResponse": false,
                    "hotelReviewId": 947130812,
                    "providerId": 332,
                    "rating": 8.8,
                    "checkInDateMonthAndYear": "March 2025",
                    "formattedRating": "8.8",
                    "ratingText": "Excellent",
                    "responderName": "Surfer's Point Deck",
                    "reviewComments": "perfect spot to just look at the sea.",
                    "reviewTitle": "value for money",
                    "translateSource": "en",
                    "translateTarget": "en",
                    "reviewDate": "2025-04-10T04:10:00+07:00",
                    "reviewerInfo": {
                        "countryName": "Philippines",
                        "displayMemberName": "*****",
                        "flagName": "ph",
                        "countryId": 70,
                        "reviewerReviewedCount": 1,
                        "isExpertReviewer": false
                    }
                },
                "overallByProviders": [
                    {
                        "providerId": 332,
                        "provider": "Agoda",
                        "overallScore": 6.5,
                        "reviewCount": 262,
                        "grades": {
                            "Cleanliness": 6.0,
                            "Facilities": 5.5,
                            "Location": 7.9,
                            "Service": 7.0,
                            "Value for money": 6.2
                        }
                    }
                ]
            }
            """;

        HotelReviewJsonParser.HotelReviewParseResult result = parser.parseHotelReview(validJson);

        // Test Provider
        assertNotNull(result.getProvider());
        assertEquals(Short.valueOf((short) 332), result.getProvider().getExternalId());
        assertEquals("Agoda", result.getProvider().getProviderName());

        // Test Hotel
        assertNotNull(result.getHotel());
        assertEquals(16402071, result.getHotel().getExternalId());
        assertEquals("Surfer's Point Deck", result.getHotel().getHotelName());
        assertNotNull(result.getHotel().getProvider());

        // Test Reviewer
        assertNotNull(result.getReviewer());
        assertEquals("*****", result.getReviewer().getDisplayName());
        assertEquals("Philippines", result.getReviewer().getCountryName());
        assertEquals(70, result.getReviewer().getCountryId());
        assertEquals("ph", result.getReviewer().getFlagCode());
        assertEquals(false, result.getReviewer().getIsExpert());
        assertEquals(1, result.getReviewer().getReviewsWritten());

        // Test Review
        assertNotNull(result.getReview());
        assertEquals(947130812L, result.getReview().getReviewExternalId());
        assertEquals(8.8, result.getReview().getRatingRaw());
        assertEquals("Excellent", result.getReview().getRatingText());
        assertEquals("8.8", result.getReview().getRatingFormatted());
        assertEquals("value for money", result.getReview().getReviewTitle());
        assertEquals("perfect spot to just look at the sea.", result.getReview().getReviewComment());
        assertEquals("en", result.getReview().getTranslateSource());
        assertEquals("en", result.getReview().getTranslateTarget());
        assertEquals(false, result.getReview().getIsResponseShown());
        assertEquals("Surfer's Point Deck", result.getReview().getResponderName());
        assertEquals("March 2025", result.getReview().getCheckInMonthYr());

        // Test Provider Hotel Summaries
        assertNotNull(result.getProviderHotelSummaries());
        assertEquals(1, result.getProviderHotelSummaries().size());
        ProviderHotelSummaryDto summary = result.getProviderHotelSummaries().get(0);
        assertEquals(Short.valueOf((short) 332), summary.getProviderId());
        assertEquals(6.5, summary.getOverallScore());
        assertEquals(262, summary.getReviewCount());

        // Test Provider Hotel Grades
        assertNotNull(result.getProviderHotelGrades());
        assertEquals(5, result.getProviderHotelGrades().size());
    }

    @Test
    void testParseHotelReviewWithMissingProviderId() {
        String invalidJson = """
            {
                "hotelId": 16402071,
                "platform": "Agoda",
                "hotelName": "Surfer's Point Deck",
                "comment": {
                    "hotelReviewId": 947130812,
                    "rating": 8.8
                }
            }
            """;

        Exception exception = assertThrows(RuntimeException.class, () -> {
            parser.parseHotelReview(invalidJson);
        });

        assertTrue(exception.getMessage().contains("Provider ID or name is missing"));
    }

    @Test
    void testParseHotelReviewWithMissingHotelId() {
        String invalidJson = """
            {
                "platform": "Agoda",
                "hotelName": "Surfer's Point Deck",
                "comment": {
                    "providerId": 332,
                    "hotelReviewId": 947130812,
                    "rating": 8.8
                }
            }
            """;

        Exception exception = assertThrows(RuntimeException.class, () -> {
            parser.parseHotelReview(invalidJson);
        });

        assertTrue(exception.getMessage().contains("Hotel ID or name is missing"));
    }

    @Test
    void testParseHotelReviewWithMissingReviewerInfo() {
        String invalidJson = """
            {
                "hotelId": 16402071,
                "platform": "Agoda",
                "hotelName": "Surfer's Point Deck",
                "comment": {
                    "providerId": 332,
                    "hotelReviewId": 947130812,
                    "rating": 8.8
                }
            }
            """;

        Exception exception = assertThrows(RuntimeException.class, () -> {
            parser.parseHotelReview(invalidJson);
        });

        assertTrue(exception.getMessage().contains("Reviewer info is missing"));
    }

    @Test
    void testParseHotelReviewWithMissingReviewId() {
        String invalidJson = """
            {
                "hotelId": 16402071,
                "platform": "Agoda",
                "hotelName": "Surfer's Point Deck",
                "comment": {
                    "providerId": 332,
                    "rating": 8.8,
                    "reviewerInfo": {
                        "displayMemberName": "*****",
                        "countryName": "Philippines"
                    }
                }
            }
            """;

        Exception exception = assertThrows(RuntimeException.class, () -> {
            parser.parseHotelReview(invalidJson);
        });

        assertTrue(exception.getMessage().contains("Review ID is missing"));
    }

    @Test
    void testParseHotelReviewWithInvalidJson() {
        String invalidJson = "{ invalid json }";

        Exception exception = assertThrows(Exception.class, () -> {
            parser.parseHotelReview(invalidJson);
        });

        assertNotNull(exception);
    }

    @Test
    void testParseHotelReviewWithNullValues() throws Exception {
        String jsonWithNulls = """
            {
                "hotelId": 16402071,
                "platform": "Agoda",
                "hotelName": "Surfer's Point Deck",
                "comment": {
                    "providerId": 332,
                    "hotelReviewId": 947130812,
                    "rating": 8.8,
                    "ratingText": null,
                    "reviewComments": null,
                    "reviewerInfo": {
                        "displayMemberName": "*****",
                        "countryName": "Philippines",
                        "countryId": null,
                        "flagName": null,
                        "isExpertReviewer": null,
                        "reviewerReviewedCount": null
                    }
                }
            }
            """;

        HotelReviewJsonParser.HotelReviewParseResult result = parser.parseHotelReview(jsonWithNulls);

        // Should handle null values gracefully
        assertNotNull(result.getReviewer());
        assertEquals("*****", result.getReviewer().getDisplayName());
        assertEquals("Philippines", result.getReviewer().getCountryName());
        assertNull(result.getReviewer().getCountryId());
        assertNull(result.getReviewer().getFlagCode());
        assertNull(result.getReviewer().getIsExpert());
        assertNull(result.getReviewer().getReviewsWritten());
    }
} 