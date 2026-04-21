package com.goviconnect.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "agri_officer_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgriOfficerDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    @Column(nullable = false, unique = true)
    private String registrationNumber;

    private String designation;
    private String specializationArea;
    private String assignedArea;

    @Column(nullable = false)
    private String officialEmail;
}
