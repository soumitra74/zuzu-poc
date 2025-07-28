package org.soumitra.reviewsystem.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderHotelSummaryDto {
    private Integer hotelId;
    private Short providerId;
    private Double overallScore;
    private Integer reviewCount;
} 