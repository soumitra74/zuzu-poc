package org.soumitra.reviewsystem.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "record_errors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordError {
    @Id
    @Column(name = "record_id")
    private Integer recordId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", referencedColumnName = "id")
    private Record record;

    @Column(name = "error_type")
    private String errorType;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "traceback")
    private String traceback;
} 