package org.soumitra.reviewsystem.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewerDto {
    private Long reviewerId;
    private String displayName;
    private Integer countryId;
    private String countryName;
    private String flagCode;
    private Boolean isExpert;
    private Integer reviewsWritten;
} 