package com.goviconnect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "crop_logs")
@lombok.Getter
@lombok.Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CropLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false, length = 100)
    private String cropName;

    @Column(nullable = false)
    private LocalDate plantedDate;

    @Column(nullable = false)
    private LocalDate harvestExpectedDate;

    private Double fieldSize; // in Acres or Hectares

    @Column(length = 100)
    private String seedVariety;

    @Column(length = 50)
    private String season; // Yala, Maha

    // Performance Analytics Fields
    private Double yieldAmount; // in Kg

    private Double incomeAmount; // in LKR

    @OneToMany(mappedBy = "cropLog", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonManagedReference
    private List<CropActivity> activities = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdDate;
}
