package com.goviconnect.repository;

import com.goviconnect.entity.SavedCropRecommendation;
import com.goviconnect.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavedCropRecommendationRepository extends JpaRepository<SavedCropRecommendation, Long> {
    List<SavedCropRecommendation> findByUserOrderBySavedAtDesc(User user);
}
