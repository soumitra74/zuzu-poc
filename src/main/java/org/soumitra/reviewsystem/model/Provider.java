package org.soumitra.reviewsystem.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "provider")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Provider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "provider_id")
    private Short providerId;

    @Column(name = "external_id", nullable = false, unique = true)
    private Short externalId;

    @Column(name = "provider_name", nullable = false)
    private String providerName;
} 