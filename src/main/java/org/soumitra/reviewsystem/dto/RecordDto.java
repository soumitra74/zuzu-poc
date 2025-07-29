package org.soumitra.reviewsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordDto {
    private Integer id;
    private Integer s3FileId;
    private Integer jobRunId;
    private String rawData;
    private String status;
    private LocalDateTime downloadedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Boolean errorFlag;
} 