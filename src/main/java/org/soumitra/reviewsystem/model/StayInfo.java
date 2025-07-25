package org.soumitra.reviewsystem.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stay_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StayInfo {
    @Id
    @Column(name = "review_id")
    private Long reviewId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", referencedColumnName = "review_id")
    private Review review;

    @Column(name = "room_type_id")
    private Integer roomTypeId;

    @Column(name = "room_type_name")
    private String roomTypeName;

    @Column(name = "review_group_id")
    private Integer reviewGroupId;

    @Column(name = "review_group_name")
    private String reviewGroupName;

    @Column(name = "length_of_stay")
    private Short lengthOfStay;
} 