package org.soumitra.reviewsystem.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderHotelGradeDto {
    private Integer hotelId;
    private Short providerId;
    private Short categoryId;
    private String categoryName;
    private Long reviewId;
    private Double gradeValue;
} 