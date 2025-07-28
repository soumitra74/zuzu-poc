package org.soumitra.reviewsystem.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingCategoryDto {
    private Short categoryId;
    private String categoryName;
} 