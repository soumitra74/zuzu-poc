package org.soumitra.reviewsystem.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rating_category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Short categoryId;

    @Column(name = "category_name", nullable = false, unique = true)
    private String categoryName;
} 