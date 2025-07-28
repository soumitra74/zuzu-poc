package org.soumitra.reviewsystem.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelDto {
    private Integer hotelId;
    private Integer externalId;
    private ProviderDto provider;
    private String hotelName;
} 