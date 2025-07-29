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
    private String s3Key;
    private String status;
    private String errorMessage;
    private Integer recordCount;
    private Integer pageNumber;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
} 