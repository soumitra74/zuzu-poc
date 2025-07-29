package org.soumitra.reviewsystem.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRunDto {
    private Integer id;
    private LocalDateTime scheduledAt;
    private LocalDateTime finishedAt;
    private String status;
    private String triggerType;
    private String notes;
} 