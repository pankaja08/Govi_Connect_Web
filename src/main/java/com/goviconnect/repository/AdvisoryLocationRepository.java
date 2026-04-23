package com.goviconnect.repository;

import com.goviconnect.entity.AdvisoryLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AdvisoryLocationRepository extends JpaRepository<AdvisoryLocation, Integer> {
    Optional<AdvisoryLocation> findByName(String name);
}

