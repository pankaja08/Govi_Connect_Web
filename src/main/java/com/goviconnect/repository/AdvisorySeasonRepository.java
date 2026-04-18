package com.goviconnect.repository;

import com.goviconnect.entity.AdvisorySeason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AdvisorySeasonRepository extends JpaRepository<AdvisorySeason, Integer> {
    Optional<AdvisorySeason> findByName(String name);
}

