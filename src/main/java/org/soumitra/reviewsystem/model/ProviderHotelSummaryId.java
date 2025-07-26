package org.soumitra.reviewsystem.model;

import lombok.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProviderHotelSummaryId implements Serializable {
    private Integer hotel;
    private Short provider;
} 