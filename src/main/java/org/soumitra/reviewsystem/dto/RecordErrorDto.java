package org.soumitra.reviewsystem.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordErrorDto {
    private Integer id;
    private RecordDto record;
    private Integer recordId;
    private String errorType;
    private String errorMessage;
    private String traceback;
} 