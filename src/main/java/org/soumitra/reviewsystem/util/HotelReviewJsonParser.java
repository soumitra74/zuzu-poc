package org.soumitra.reviewsystem.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.soumitra.reviewsystem.dto.*;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class HotelReviewJsonParser {

    private final ObjectMapper objectMapper;

    public HotelReviewJsonParser() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Parse a hotel review JSON string and create the corresponding DTO objects
     * 
     * @param hotelReviewJson JSON string containing hotel review data
     * @return HotelReviewParseResult containing all parsed DTOs
     * @throws Exception if parsing fails
     */
    public HotelReviewParseResult parseHotelReview(String hotelReviewJson) throws Exception {
        JsonNode rootNode = objectMapper.readTree(hotelReviewJson);
        
        // Parse provider first (hotels depend on providers)
        ProviderDto provider = parseProvider(rootNode);
        
        // Parse hotel (with provider relationship)
        HotelDto hotel = parseHotel(rootNode, provider);
        
        // Parse reviewer
        ReviewerDto reviewer = parseReviewer(rootNode);
        
        // Parse review
        ReviewDto review = parseReview(rootNode, hotel, provider, reviewer);
        
        // Parse additional data if available
        List<ProviderHotelSummaryDto> summaries = parseProviderHotelSummaries(rootNode, hotel, provider);
        List<ProviderHotelGradeDto> grades = parseProviderHotelGrades(rootNode, hotel, provider);
        StayInfoDto stayInfo = parseStayInfo(rootNode);
        
        return HotelReviewParseResult.builder()
                .provider(provider)
                .hotel(hotel)
                .reviewer(reviewer)
                .review(review)
                .stayInfo(stayInfo)
                .providerHotelSummaries(summaries)
                .providerHotelGrades(grades)
                .build();
    }

    /**
     * Parse provider information from JSON
     */
    private ProviderDto parseProvider(JsonNode rootNode) {
        // Extract provider ID from comment section
        Short providerExternalId = null;
        String providerName = null;
        
        JsonNode commentNode = rootNode.get("comment");
        if (commentNode != null && commentNode.has("providerId")) {
            providerExternalId = commentNode.get("providerId").shortValue();
        }
        
        // Extract provider name from platform field
        if (rootNode.has("platform")) {
            providerName = rootNode.get("platform").asText();
        } else if (commentNode != null && commentNode.has("reviewProviderText")) {
            providerName = commentNode.get("reviewProviderText").asText();
        }
        
        if (providerExternalId == null || providerName == null) {
            throw new RuntimeException("Provider ID or name is missing");
        }
        
        return ProviderDto.builder()
                .externalId(providerExternalId)
                .providerName(providerName)
                .build();
    }

    /**
     * Parse hotel information from JSON
     */
    private HotelDto parseHotel(JsonNode rootNode, ProviderDto provider) {
        Integer hotelExternalId = null;
        String hotelName = null;
        
        // Check for flattened structure first
        if (rootNode.has("hotelId")) {
            hotelExternalId = rootNode.get("hotelId").asInt();
        }
        
        if (rootNode.has("hotelName")) {
            hotelName = rootNode.get("hotelName").asText();
        }
        
        if (hotelExternalId == null || hotelName == null) {
            throw new RuntimeException("Hotel ID or name is missing");
        }
        
        return HotelDto.builder()
                .externalId(hotelExternalId)
                .provider(provider)
                .hotelName(hotelName)
                .build();
    }

    /**
     * Parse reviewer information from JSON
     */
    private ReviewerDto parseReviewer(JsonNode rootNode) {
        JsonNode commentNode = rootNode.get("comment");
        if (commentNode == null) {
            throw new RuntimeException("Comment section is missing");
        }
        
        JsonNode reviewerInfoNode = commentNode.get("reviewerInfo");
        if (reviewerInfoNode == null) {
            throw new RuntimeException("Reviewer info is missing");
        }
        
        String displayName = getStringValue(reviewerInfoNode, "displayMemberName");
        String countryName = getStringValue(reviewerInfoNode, "countryName");
        Integer countryId = getIntegerValue(reviewerInfoNode, "countryId");
        String flagCode = getStringValue(reviewerInfoNode, "flagName");
        Boolean isExpert = getBooleanValue(reviewerInfoNode, "isExpertReviewer");
        Integer reviewsWritten = getIntegerValue(reviewerInfoNode, "reviewerReviewedCount");
        
        if (displayName == null) {
            throw new RuntimeException("Reviewer display name is missing");
        }
        
        return ReviewerDto.builder()
                .displayName(displayName)
                .countryName(countryName)
                .countryId(countryId)
                .flagCode(flagCode)
                .isExpert(isExpert)
                .reviewsWritten(reviewsWritten)
                .build();
    }

    /**
     * Parse review information from JSON
     */
    private ReviewDto parseReview(JsonNode rootNode, HotelDto hotel, ProviderDto provider, ReviewerDto reviewer) {
        JsonNode commentNode = rootNode.get("comment");
        if (commentNode == null) {
            throw new RuntimeException("Comment section is missing");
        }
        
        Long reviewExternalId = getLongValue(commentNode, "hotelReviewId");
        if (reviewExternalId == null) {
            throw new RuntimeException("Review ID is missing");
        }
        
        Double ratingRaw = getDoubleValue(commentNode, "rating");
        String ratingText = getStringValue(commentNode, "ratingText");
        String ratingFormatted = getStringValue(commentNode, "formattedRating");
        String reviewTitle = getStringValue(commentNode, "reviewTitle");
        String reviewComment = getStringValue(commentNode, "reviewComments");
        String translateSource = getStringValue(commentNode, "translateSource");
        String translateTarget = getStringValue(commentNode, "translateTarget");
        Boolean isResponseShown = getBooleanValue(commentNode, "isShowReviewResponse");
        String responderName = getStringValue(commentNode, "responderName");
        String responseText = getStringValue(commentNode, "originalComment");
        String responseDateText = getStringValue(commentNode, "responseDateText");
        String responseDateFmt = getStringValue(commentNode, "formattedResponseDate");
        String checkInMonthYr = getStringValue(commentNode, "checkInDateMonthAndYear");
        
        // Parse review date
        OffsetDateTime reviewDate = null;
        if (commentNode.has("reviewDate")) {
            try {
                String reviewDateStr = commentNode.get("reviewDate").asText();
                reviewDate = OffsetDateTime.parse(reviewDateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            } catch (Exception e) {
                System.out.println("Warning: Could not parse review date: " + commentNode.get("reviewDate").asText());
            }
        }
        
        return ReviewDto.builder()
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
    }

    /**
     * Parse provider hotel summaries from JSON
     */
    private List<ProviderHotelSummaryDto> parseProviderHotelSummaries(JsonNode rootNode, HotelDto hotel, ProviderDto provider) {
        List<ProviderHotelSummaryDto> summaries = new ArrayList<>();
        
        JsonNode overallByProvidersNode = rootNode.get("overallByProviders");
        if (overallByProvidersNode != null && overallByProvidersNode.isArray()) {
            for (JsonNode summaryNode : overallByProvidersNode) {
                Short providerId = getShortValue(summaryNode, "providerId");
                String providerName = getStringValue(summaryNode, "provider");
                Double overallScore = getDoubleValue(summaryNode, "overallScore");
                Integer reviewCount = getIntegerValue(summaryNode, "reviewCount");
                
                if (providerId != null && providerName != null) {
                    summaries.add(ProviderHotelSummaryDto.builder()
                            .hotelId(hotel.getHotelId())
                            .providerId(providerId)
                            .overallScore(overallScore)
                            .reviewCount(reviewCount)
                            .build());
                }
            }
        }
        
        return summaries;
    }

    /**
     * Parse provider hotel grades from JSON
     */
    private List<ProviderHotelGradeDto> parseProviderHotelGrades(JsonNode rootNode, HotelDto hotel, ProviderDto provider) {
        List<ProviderHotelGradeDto> grades = new ArrayList<>();
        
        JsonNode overallByProvidersNode = rootNode.get("overallByProviders");
        if (overallByProvidersNode != null && overallByProvidersNode.isArray()) {
            for (JsonNode summaryNode : overallByProvidersNode) {
                Short providerId = getShortValue(summaryNode, "providerId");
                JsonNode gradesNode = summaryNode.get("grades");
                
                if (gradesNode != null && providerId != null) {
                    gradesNode.fieldNames().forEachRemaining(categoryName -> {
                        Double gradeValue = getDoubleValue(gradesNode, categoryName);
                        if (gradeValue != null) {
                            grades.add(ProviderHotelGradeDto.builder()
                                    .hotelId(hotel.getHotelId())
                                    .providerId(providerId)
                                    .categoryId(null) // Would need category mapping
                                    .gradeValue(gradeValue)
                                    .build());
                        }
                    });
                }
            }
        }
        
        return grades;
    }

    /**
     * Parse stay info from JSON
     */
    private StayInfoDto parseStayInfo(JsonNode rootNode) {
        JsonNode commentNode = rootNode.get("comment");
        if (commentNode == null) {
            return null;
        }
        JsonNode reviewerInfoNode = commentNode.get("reviewerInfo");
        if (reviewerInfoNode == null) {
            return null;
        }

        Long reviewId = getLongValue(commentNode, "hotelReviewId");
        Integer roomTypeId = getIntegerValue(reviewerInfoNode, "roomTypeId");
        String roomTypeName = getStringValue(reviewerInfoNode, "roomTypeName");
        Integer reviewGroupId = getIntegerValue(reviewerInfoNode, "reviewGroupId");
        String reviewGroupName = getStringValue(reviewerInfoNode, "reviewGroupName");
        Short lengthOfStay = getShortValue(reviewerInfoNode, "lengthOfStay");
        
        // Only return StayInfo if we have at least some meaningful data
        if (reviewId == null && roomTypeId == null && roomTypeName == null && 
            reviewGroupId == null && reviewGroupName == null && lengthOfStay == null) {
            return null;
        }
        
        return StayInfoDto.builder()
                .reviewId(reviewId)
                .roomTypeId(roomTypeId)
                .roomTypeName(roomTypeName)
                .reviewGroupId(reviewGroupId)
                .reviewGroupName(reviewGroupName)
                .lengthOfStay(lengthOfStay)
                .build();
    }

    // Helper methods for safe JSON value extraction
    private String getStringValue(JsonNode node, String fieldName) {
        return node.has(fieldName) && !node.get(fieldName).isNull() ? 
            node.get(fieldName).asText() : null;
    }

    private Integer getIntegerValue(JsonNode node, String fieldName) {
        return node.has(fieldName) && !node.get(fieldName).isNull() ? 
            node.get(fieldName).asInt() : null;
    }

    private Long getLongValue(JsonNode node, String fieldName) {
        return node.has(fieldName) && !node.get(fieldName).isNull() ? 
            node.get(fieldName).asLong() : null;
    }

    private Short getShortValue(JsonNode node, String fieldName) {
        return node.has(fieldName) && !node.get(fieldName).isNull() ? 
            node.get(fieldName).shortValue() : null;
    }

    private Double getDoubleValue(JsonNode node, String fieldName) {
        return node.has(fieldName) && !node.get(fieldName).isNull() ? 
            node.get(fieldName).asDouble() : null;
    }

    private Boolean getBooleanValue(JsonNode node, String fieldName) {
        return node.has(fieldName) && !node.get(fieldName).isNull() ? 
            node.get(fieldName).asBoolean() : null;
    }

    /**
     * Result class containing all parsed DTOs
     */
    public static class HotelReviewParseResult {
        private final ProviderDto provider;
        private final HotelDto hotel;
        private final ReviewerDto reviewer;
        private final ReviewDto review;
        private final StayInfoDto stayInfo;
        private final List<ProviderHotelSummaryDto> providerHotelSummaries;
        private final List<ProviderHotelGradeDto> providerHotelGrades;

        private HotelReviewParseResult(Builder builder) {
            this.provider = builder.provider;
            this.hotel = builder.hotel;
            this.reviewer = builder.reviewer;
            this.review = builder.review;
            this.stayInfo = builder.stayInfo;
            this.providerHotelSummaries = builder.providerHotelSummaries;
            this.providerHotelGrades = builder.providerHotelGrades;
        }

        public static Builder builder() {
            return new Builder();
        }

        public ProviderDto getProvider() { return provider; }
        public HotelDto getHotel() { return hotel; }
        public ReviewerDto getReviewer() { return reviewer; }
        public ReviewDto getReview() { return review; }
        public StayInfoDto getStayInfo() { return stayInfo; }
        public List<ProviderHotelSummaryDto> getProviderHotelSummaries() { return providerHotelSummaries; }
        public List<ProviderHotelGradeDto> getProviderHotelGrades() { return providerHotelGrades; }

        public static class Builder {
            private ProviderDto provider;
            private HotelDto hotel;
            private ReviewerDto reviewer;
            private ReviewDto review;
            private StayInfoDto stayInfo;
            private List<ProviderHotelSummaryDto> providerHotelSummaries = new ArrayList<>();
            private List<ProviderHotelGradeDto> providerHotelGrades = new ArrayList<>();

            public Builder provider(ProviderDto provider) {
                this.provider = provider;
                return this;
            }

            public Builder hotel(HotelDto hotel) {
                this.hotel = hotel;
                return this;
            }

            public Builder reviewer(ReviewerDto reviewer) {
                this.reviewer = reviewer;
                return this;
            }

            public Builder review(ReviewDto review) {
                this.review = review;
                return this;
            }

            public Builder stayInfo(StayInfoDto stayInfo) {
                this.stayInfo = stayInfo;
                return this;
            }

            public Builder providerHotelSummaries(List<ProviderHotelSummaryDto> summaries) {
                this.providerHotelSummaries = summaries;
                return this;
            }

            public Builder providerHotelGrades(List<ProviderHotelGradeDto> grades) {
                this.providerHotelGrades = grades;
                return this;
            }

            public HotelReviewParseResult build() {
                return new HotelReviewParseResult(this);
            }
        }
    }
} 