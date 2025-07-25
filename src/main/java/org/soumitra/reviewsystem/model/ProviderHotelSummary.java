package org.soumitra.reviewsystem.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "provider_hotel_summary")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ProviderHotelSummaryId.class)
public class ProviderHotelSummary {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private Provider provider;

    @Column(name = "overall_score")
    private Double overallScore;

    @Column(name = "review_count")
    private Integer reviewCount;
} 