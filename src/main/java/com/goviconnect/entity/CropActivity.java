package com.goviconnect.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "crop_activities")
@lombok.Getter
@lombok.Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CropActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_log_id", nullable = false)
    @JsonBackReference
    private CropLog cropLog;

    @Column(nullable = false, length = 50)
    private String activityType; // e.g., FERTILIZER, PESTICIDE

    @Column(nullable = false, length = 150)
    private String activityName; // e.g., Urea, Roundup

    @Column(nullable = false)
    private LocalDate activityDate;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdDate;
}
