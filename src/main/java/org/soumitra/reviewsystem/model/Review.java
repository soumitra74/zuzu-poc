package org.soumitra.reviewsystem.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "review")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {
    @Id
    @Column(name = "review_id")
    private Long reviewId;

    @Column(name = "review_external_id", nullable = false)
    private Long reviewExternalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private Provider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private Reviewer reviewer;

    @Column(name = "rating_raw")
    private Double ratingRaw;

    @Column(name = "rating_text")
    private String ratingText;

    @Column(name = "rating_formatted")
    private String ratingFormatted;

    @Column(name = "review_title")
    private String reviewTitle;

    @Column(name = "review_comment")
    private String reviewComment;

    @Column(name = "review_vote_positive")
    private Integer reviewVotePositive;

    @Column(name = "review_vote_negative")
    private Integer reviewVoteNegative;

    @Column(name = "review_date")
    private OffsetDateTime reviewDate;

    @Column(name = "translate_source")
    private String translateSource;

    @Column(name = "translate_target")
    private String translateTarget;

    @Column(name = "is_response_shown")
    private Boolean isResponseShown;

    @Column(name = "responder_name")
    private String responderName;

    @Column(name = "response_text")
    private String responseText;

    @Column(name = "response_date_text")
    private String responseDateText;

    @Column(name = "response_date_fmt")
    private String responseDateFmt;

    @Column(name = "check_in_month_yr")
    private String checkInMonthYr;
} 