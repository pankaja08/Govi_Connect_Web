package com.goviconnect.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "crops")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdvisoryCrop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "crop_name", nullable = false)
    private String cropName;

    @Column(name = "care_instructions", columnDefinition = "TEXT")
    private String careInstructions;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "image_hash", unique = true, length = 64)
    private String imageHash;

    /**
     * Returns just the filename portion of imageUrl for serving from /uploads/crop_images/.
     * E.g. "/images/crops/paddy.jpg" → "paddy.jpg"
     */
    public String getImageFileName() {
        if (imageUrl == null || imageUrl.isBlank()) return null;
        int lastSlash = imageUrl.lastIndexOf('/');
        return lastSlash >= 0 ? imageUrl.substring(lastSlash + 1) : imageUrl;
    }

    @ManyToMany
    @JoinTable(
        name = "crop_locations",
        joinColumns = @JoinColumn(name = "crop_id"),
        inverseJoinColumns = @JoinColumn(name = "location_id")
    )
    @Builder.Default
    private List<AdvisoryLocation> locations = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "crop_seasons",
        joinColumns = @JoinColumn(name = "crop_id"),
        inverseJoinColumns = @JoinColumn(name = "season_id")
    )
    @Builder.Default
    private List<AdvisorySeason> seasons = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "crop_soil_types",
        joinColumns = @JoinColumn(name = "crop_id"),
        inverseJoinColumns = @JoinColumn(name = "soil_type_id")
    )
    @Builder.Default
    private List<AdvisorySoilType> soilTypes = new ArrayList<>();

    @OneToMany(mappedBy = "crop", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    @Builder.Default
    private List<Fertilizer> fertilizers = new ArrayList<>();

    @OneToMany(mappedBy = "crop", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    @Builder.Default
    private List<Disease> diseases = new ArrayList<>();
}
