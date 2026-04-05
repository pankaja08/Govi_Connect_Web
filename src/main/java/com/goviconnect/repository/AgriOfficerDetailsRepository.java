package com.goviconnect.repository;

import com.goviconnect.entity.AgriOfficerDetails;
import com.goviconnect.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgriOfficerDetailsRepository extends JpaRepository<AgriOfficerDetails, Long> {
    Optional<AgriOfficerDetails> findByUser(User user);
    boolean existsByRegistrationNumber(String registrationNumber);
}
