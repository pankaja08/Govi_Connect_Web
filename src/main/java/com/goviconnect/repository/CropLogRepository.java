package com.goviconnect.repository;

import com.goviconnect.entity.CropLog;
import com.goviconnect.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CropLogRepository extends JpaRepository<CropLog, Long> {
    List<CropLog> findByUserOrderByPlantedDateDesc(User user);
}
