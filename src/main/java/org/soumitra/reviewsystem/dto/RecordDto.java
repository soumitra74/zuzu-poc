package org.soumitra.reviewsystem.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordDto {
    private Integer id;
    private S3FileDto s3File;
    private JobRunDto jobRun;
    private Object rawData;
    private String status;
    private LocalDateTime processedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Boolean errorFlag;
} 