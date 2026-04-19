package com.goviconnect.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_rating")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private MarketProduct product;

    @Column(nullable = false, name = "rating_value")
    private int ratingValue;
}
