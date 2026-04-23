package com.goviconnect.repository;

import com.goviconnect.entity.MarketProduct;
import com.goviconnect.entity.ProductRating;
import com.goviconnect.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRatingRepository extends JpaRepository<ProductRating, Long> {
    Optional<ProductRating> findByUserAndProduct(User user, MarketProduct product);
}
