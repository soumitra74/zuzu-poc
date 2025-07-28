package org.soumitra.reviewsystem.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hotel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hotel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hotel_id")
    private Integer hotelId;

    @Column(name = "external_id", nullable = false)
    private Integer externalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @Column(name = "hotel_name", nullable = false)
    private String hotelName;
} 