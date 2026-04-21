package com.goviconnect.repository;

import com.goviconnect.entity.MarketProduct;
import com.goviconnect.entity.User;
import com.goviconnect.enums.ProductCategory;
import com.goviconnect.enums.ProductStatus;
import com.goviconnect.enums.SaleType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface MarketProductRepository extends JpaRepository<MarketProduct, Long> {

    @EntityGraph(attributePaths = {"seller"})
    List<MarketProduct> findByActiveTrueAndStatusOrderByCreatedDateDesc(ProductStatus status);

    @EntityGraph(attributePaths = {"seller"})
    @Query("SELECT p FROM MarketProduct p WHERE p.active = true AND p.status = 'APPROVED' " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
            "AND (:search IS NULL OR :search = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:location IS NULL OR :location = '' OR LOWER(p.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
            "AND (:saleType IS NULL OR p.saleType = :saleType) " +
            "AND (:inStockOnly IS FALSE OR p.stockStatus = 'IN_STOCK') " +
            "ORDER BY p.createdDate DESC")
    List<MarketProduct> findFiltered(
            @Param("category") ProductCategory category,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("search") String search,
            @Param("location") String location,
            @Param("saleType") SaleType saleType,
            @Param("inStockOnly") Boolean inStockOnly);

    List<MarketProduct> findBySellerOrderByCreatedDateDesc(User seller);

    @EntityGraph(attributePaths = {"seller"})
    List<MarketProduct> findByStatusOrderByCreatedDateDesc(ProductStatus status);

    /** Top 4 approved products by average rating (using subquery for portability) */
    @EntityGraph(attributePaths = {"seller"})
    @Query("SELECT p FROM MarketProduct p WHERE p.active = true AND p.status = 'APPROVED' " +
            "ORDER BY (SELECT COALESCE(AVG(r.ratingValue), 0) FROM ProductRating r WHERE r.product = p) DESC, p.createdDate DESC")
    List<MarketProduct> findTop4ByRating(org.springframework.data.domain.Pageable pageable);
}

