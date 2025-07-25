package org.soumitra.reviewsystem.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "provider_hotel_grade")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ProviderHotelGradeId.class)
public class ProviderHotelGrade {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private Provider provider;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private RatingCategory category;

    @Column(name = "grade_value")
    private Double gradeValue;
} 