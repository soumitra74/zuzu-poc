package org.soumitra.reviewsystem.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StayInfoDto {
    private Long reviewId;
    private Integer roomTypeId;
    private String roomTypeName;
    private Integer reviewGroupId;
    private String reviewGroupName;
    private Short lengthOfStay;
} 