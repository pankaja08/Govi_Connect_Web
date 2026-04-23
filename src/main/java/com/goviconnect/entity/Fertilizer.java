package com.goviconnect.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fertilizers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fertilizer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id", nullable = false)
    @JsonBackReference
    private AdvisoryCrop crop;
}
