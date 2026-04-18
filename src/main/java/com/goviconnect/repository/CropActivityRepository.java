package com.goviconnect.repository;

import com.goviconnect.entity.CropActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CropActivityRepository extends JpaRepository<CropActivity, Long> {
}
