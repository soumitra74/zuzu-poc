package org.soumitra.reviewsystem.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "s3_files")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class S3File {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_run_id")
    private JobRun jobRun;

    @Column(name = "s3_key", nullable = false, unique = true)
    private String s3Key;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "record_count")
    private Integer recordCount;

    @Column(name = "status")
    private String status;

    @Column(name = "checksum")
    private String checksum;

    @Column(name = "error_message")
    private String errorMessage;
} 