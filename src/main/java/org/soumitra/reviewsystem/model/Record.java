package org.soumitra.reviewsystem.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Record {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "s3_file_id")
    private S3File s3File;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_run_id")
    private JobRun jobRun;

    @Column(name = "raw_data", columnDefinition = "jsonb")
    private String rawData;

    @Column(name = "status")
    private String status;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "error_flag")
    private Boolean errorFlag;
} 