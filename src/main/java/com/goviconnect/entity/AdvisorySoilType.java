package com.goviconnect.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "soil_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdvisorySoilType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;
}
