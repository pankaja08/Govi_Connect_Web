package com.goviconnect.repository;

import com.goviconnect.entity.MarketProduct;
import com.goviconnect.entity.ProductFavorite;
import com.goviconnect.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductFavoriteRepository extends JpaRepository<ProductFavorite, Long> {

    Optional<ProductFavorite> findByUserAndProduct(User user, MarketProduct product);

    List<ProductFavorite> findByUser(User user);

    void deleteByUserAndProduct(User user, MarketProduct product);
}
