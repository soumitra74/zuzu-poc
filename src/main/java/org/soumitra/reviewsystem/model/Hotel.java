package org.soumitra.reviewsystem.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hotel")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hotel {
    @Id
    @Column(name = "hotel_id")
    private Integer hotelId;

    @Column(name = "external_id", nullable = false, unique = true)
    private Integer externalId;

    @Column(name = "hotel_name", nullable = false)
    private String hotelName;
} 