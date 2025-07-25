package org.soumitra.reviewsystem.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reviewer")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reviewer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "country_id")
    private Integer countryId;

    @Column(name = "country_name")
    private String countryName;

    @Column(name = "flag_code")
    private String flagCode;

    @Column(name = "is_expert")
    private Boolean isExpert;

    @Column(name = "reviews_written")
    private Integer reviewsWritten;
} 