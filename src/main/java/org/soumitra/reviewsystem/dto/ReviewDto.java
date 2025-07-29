package org.soumitra.reviewsystem.dto;

import lombok.*;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDto {
    private Long reviewId;
    private Long reviewExternalId;
    private HotelDto hotel;
    private ProviderDto provider;
    private ReviewerDto reviewer;
    private Double rating;
    private String ratingText;
    private String ratingFormatted;
    private String reviewTitle;
    private String reviewComment;
    private Integer reviewVotePositive;
    private Integer reviewVoteNegative;
    private OffsetDateTime reviewDate;
    private String translateSource;
    private String translateTarget;
    private Boolean isResponseShown;
    private String responderName;
    private String responseText;
    private String responseDateText;
    private String responseDateFmt;
    private String checkInMonthYr;
} 