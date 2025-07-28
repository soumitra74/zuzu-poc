package org.soumitra.reviewsystem.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class S3FileDto {
    private Integer id;
    private Integer jobRunId;
    private String bucket;
    private String key;
    private String status;
    private String errorMessage;
    private Integer recordCount;
    private Boolean isActive;
    private LocalDateTime processedAt;
} 