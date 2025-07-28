package org.soumitra.reviewsystem.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderDto {
    private Short providerId;
    private Short externalId;
    private String providerName;
} 