package org.soumitra.reviewsystem.model;

import lombok.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProviderHotelGradeId implements Serializable {
    private Integer hotel;
    private Short provider;
    private Short category;
    private Long review;
} 