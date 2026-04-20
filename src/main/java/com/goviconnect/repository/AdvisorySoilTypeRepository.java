package com.goviconnect.repository;

import com.goviconnect.entity.AdvisorySoilType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AdvisorySoilTypeRepository extends JpaRepository<AdvisorySoilType, Integer> {
    Optional<AdvisorySoilType> findByName(String name);
}

