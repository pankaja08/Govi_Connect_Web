package com.goviconnect.repository;

import com.goviconnect.entity.AdvisoryCrop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdvisoryCropRepository extends JpaRepository<AdvisoryCrop, Integer> {

    /**
     * Native SQL query to filter crops using exact column names from the DB schema.
     * Location & Season are required (INNER JOIN). Soil type is optional (LEFT JOIN + IS NULL check).
     */
    @Query(value = "SELECT DISTINCT c.id FROM crops c " +
           "INNER JOIN crop_locations cl ON c.id = cl.crop_id " +
           "INNER JOIN locations l ON cl.location_id = l.id " +
           "INNER JOIN crop_seasons cs ON c.id = cs.crop_id " +
           "INNER JOIN seasons s ON cs.season_id = s.id " +
           "LEFT JOIN crop_soil_types cst ON c.id = cst.crop_id " +
           "LEFT JOIN soil_types st ON cst.soil_type_id = st.id " +
           "WHERE l.name = :locationName " +
           "AND s.name = :seasonName " +
           "AND (:soilName IS NULL OR :soilName = '' OR st.name = :soilName)",
           nativeQuery = true)
    List<Integer> findCropIdsByCriteria(@Param("locationName") String locationName,
                                        @Param("seasonName") String seasonName,
                                        @Param("soilName") String soilName);

    List<AdvisoryCrop> findByIdIn(List<Integer> ids);

    boolean existsByImageHash(String imageHash);

    boolean existsByCropNameIgnoreCase(String cropName);
}
