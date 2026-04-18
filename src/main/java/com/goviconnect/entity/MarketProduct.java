package com.goviconnect.entity;

import com.goviconnect.enums.ProductCategory;
import com.goviconnect.enums.ProductStatus;
import com.goviconnect.enums.SaleType;
import com.goviconnect.enums.StockStatus;
import org.hibernate.annotations.Formula;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProductStatus status = ProductStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StockStatus stockStatus = StockStatus.IN_STOCK;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SaleType saleType = SaleType.BOTH;

    @Formula("(SELECT COUNT(*) FROM product_favorite f WHERE f.product_id = id)")
    private int favoriteCount;

    @Formula("(SELECT COALESCE(AVG(r.rating_value), 0.0) FROM product_rating r WHERE r.product_id = id)")
    private Double averageRating = 0.0;

    @Formula("(SELECT COUNT(r.id) FROM product_rating r WHERE r.product_id = id)")
    private int ratingCount;

    @Column(length = 255)
    private String imageUrl;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 30)
    private String unit; // kg, pieces, liters, bags, etc.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(length = 100)
    private String sellerName;

    @Column(length = 20)
    private String contactNumber;

    @Column(length = 100)
    private String location;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdDate;

    @PrePersist
    public void prePersist() {
        this.createdDate = LocalDateTime.now();
    }
}
