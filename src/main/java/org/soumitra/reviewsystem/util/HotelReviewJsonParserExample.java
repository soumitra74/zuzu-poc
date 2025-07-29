package org.soumitra.reviewsystem.util;

import org.soumitra.reviewsystem.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Example class demonstrating how to use the HotelReviewJsonParser
 */
@Component
public class HotelReviewJsonParserExample {

    @Autowired
    private HotelReviewJsonParser parser;

    /**
     * Example method showing how to parse a hotel review JSON string
     */
    public void parseExample() {
        String hotelReviewJson = """
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
                    "encryptedReviewData": "gZROtCSVrPGpuWYhM/X9Nw==",
                    "formattedRating": "8.8",
                    "formattedReviewDate": "April 10, 2025",
                    "ratingText": "Excellent",
                    "responderName": "Surfer's Point Deck",
                    "responseDateText": "",
                    "responseTranslateSource": "en",
                    "reviewComments": "perfect spot to just look at the sea. ",
                    "reviewNegatives": "",
                    "reviewPositives": "",
                    "reviewProviderLogo": "",
                    "reviewProviderText": "Agoda",
                    "reviewTitle": "value for money",
                    "translateSource": "en",
                    "translateTarget": "en",
                    "reviewDate": "2025-04-10T04:10:00+07:00",
                    "reviewerInfo": {
                        "countryName": "Philippines",
                        "displayMemberName": "*****",
                        "flagName": "ph",
                        "reviewGroupName": "Couple",
                        "roomTypeName": "Deluxe Room",
                        "countryId": 70,
                        "lengthOfStay": 3,
                        "reviewGroupId": 2,
                        "roomTypeId": 0,
                        "reviewerReviewedCount": 1,
                        "isExpertReviewer": false,
                        "isShowGlobalIcon": false,
                        "isShowReviewedCount": false
                    },
                    "originalTitle": "",
                    "originalComment": "",
                    "formattedResponseDate": ""
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

        try {
            // Parse the JSON string
            HotelReviewJsonParser.HotelReviewParseResult result = parser.parseHotelReview(hotelReviewJson);

            // Access the parsed DTOs
            ProviderDto provider = result.getProvider();
            HotelDto hotel = result.getHotel();
            ReviewerDto reviewer = result.getReviewer();
            ReviewDto review = result.getReview();
            
            // Print the results
            System.out.println("Parsed Provider: " + provider);
            System.out.println("Parsed Hotel: " + hotel);
            System.out.println("Parsed Reviewer: " + reviewer);
            System.out.println("Parsed Review: " + review);
            
            // Access additional data
            System.out.println("Provider Hotel Summaries: " + result.getProviderHotelSummaries());
            System.out.println("Provider Hotel Grades: " + result.getProviderHotelGrades());

        } catch (Exception e) {
            System.err.println("Error parsing hotel review JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Example method showing how to parse multiple hotel reviews
     */
    public void parseMultipleReviews(String[] hotelReviewJsons) {
        for (int i = 0; i < hotelReviewJsons.length; i++) {
            try {
                System.out.println("Parsing review " + (i + 1) + ":");
                HotelReviewJsonParser.HotelReviewParseResult result = parser.parseHotelReview(hotelReviewJsons[i]);
                
                System.out.println("  Hotel: " + result.getHotel().getHotelName());
                System.out.println("  Provider: " + result.getProvider().getProviderName());
                System.out.println("  Review ID: " + result.getReview().getReviewExternalId());
                System.out.println("  Rating: " + result.getReview().getRating());
                System.out.println();
                
            } catch (Exception e) {
                System.err.println("Error parsing review " + (i + 1) + ": " + e.getMessage());
            }
        }
    }
} 