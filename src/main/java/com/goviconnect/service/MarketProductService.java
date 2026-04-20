package com.goviconnect.service;

import com.goviconnect.entity.MarketProduct;
import com.goviconnect.entity.User;
import com.goviconnect.enums.ProductCategory;
import com.goviconnect.enums.ProductStatus;
import com.goviconnect.enums.SaleType;
import com.goviconnect.enums.StockStatus;
import com.goviconnect.entity.ProductFavorite;
import com.goviconnect.repository.MarketProductRepository;
import com.goviconnect.repository.ProductFavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarketProductService {

    private final MarketProductRepository marketProductRepository;
    private final ProductFavoriteRepository productFavoriteRepository;
    private final com.goviconnect.repository.ProductRatingRepository productRatingRepository;
    private final EmailService emailService;

    @Value("${app.upload.dir:uploads/blog-images}")
    private String uploadDir;

    @Transactional(readOnly = true)
    public List<MarketProduct> getAllActiveProducts() {
        return marketProductRepository.findByActiveTrueAndStatusOrderByCreatedDateDesc(ProductStatus.APPROVED);
    }

    @Transactional(readOnly = true)
    public List<MarketProduct> getFilteredProducts(ProductCategory category, BigDecimal minPrice, BigDecimal maxPrice, String search, String location, SaleType saleType, Boolean inStockOnly) {
        return marketProductRepository.findFiltered(category, minPrice, maxPrice, search, location, saleType, inStockOnly);
    }

    @Transactional(readOnly = true)
    public MarketProduct getProductById(Long id) {
        return marketProductRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    @Transactional(readOnly = true)
    public List<MarketProduct> getProductsBySeller(User seller) {
        return marketProductRepository.findBySellerOrderByCreatedDateDesc(seller);
    }

    @Transactional
    public MarketProduct createProduct(User seller, String name, String description, BigDecimal price,
                                        ProductCategory category, MultipartFile image, Integer quantity,
                                        String unit, String contactNumber, String location, SaleType saleType) throws IOException {
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = saveImage(image);
        }

        MarketProduct product = MarketProduct.builder()
                .name(name)
                .description(description)
                .price(price)
                .category(category)
                .imageUrl(imageUrl)
                .quantity(quantity)
                .unit(unit)
                .seller(seller)
                .sellerName(seller.getFullName())
                .contactNumber(contactNumber != null ? contactNumber : seller.getContactNumber())
                .location(location != null ? location : seller.getDistrict())
                .saleType(saleType != null ? saleType : SaleType.BOTH)
                .active(true)
                .status(ProductStatus.PENDING)
                .build();

        return marketProductRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<MarketProduct> getTopRatedProducts() {
        return marketProductRepository.findTop4ByRating(
                org.springframework.data.domain.PageRequest.of(0, 4));
    }

    @Transactional
    public void deleteProduct(Long id, User seller) {
        MarketProduct product = getProductById(id);
        if (!product.getSeller().getId().equals(seller.getId())) {
            throw new SecurityException("You can only delete your own listings.");
        }
        marketProductRepository.delete(product);
    }

    @Transactional
    public void updateProductDetails(Long id, User seller, String description, BigDecimal price, Integer quantity, StockStatus stockStatus) {
        MarketProduct product = getProductById(id);
        if (!product.getSeller().getId().equals(seller.getId())) {
            throw new SecurityException("You can only edit your own listings.");
        }
        product.setDescription(description);
        product.setPrice(price);
        product.setQuantity(quantity);
        if (stockStatus != null) {
            product.setStockStatus(stockStatus);
        }
        marketProductRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<MarketProduct> getPendingProducts() {
        return marketProductRepository.findByStatusOrderByCreatedDateDesc(ProductStatus.PENDING);
    }

    @Transactional
    public void approveProduct(Long id) {
        MarketProduct product = getProductById(id);
        product.setStatus(ProductStatus.APPROVED);
        product.setActive(true);
        marketProductRepository.save(product);
        emailService.sendProductApprovalEmail(product.getSeller(), product);
    }

    @Transactional
    public void rejectProduct(Long id) {
        MarketProduct product = getProductById(id);
        product.setStatus(ProductStatus.REJECTED);
        product.setActive(false);
        marketProductRepository.save(product);
        emailService.sendProductRejectionEmail(product.getSeller(), product);
    }

    @Transactional
    public java.util.Map<String, Object> toggleFavorite(Long id, User user) {
        MarketProduct product = getProductById(id);
        var existingFavorite = productFavoriteRepository.findByUserAndProduct(user, product);
        boolean isFavorite;
        
        if (existingFavorite.isPresent()) {
            productFavoriteRepository.delete(existingFavorite.get());
            isFavorite = false;
        } else {
            productFavoriteRepository.save(ProductFavorite.builder().user(user).product(product).build());
            isFavorite = true;
        }

        // Return updated state
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("isFavorite", isFavorite);
        return response;
    }

    @Transactional
    public java.util.Map<String, Object> rateProduct(Long id, User user, int ratingValue) {
        MarketProduct product = getProductById(id);
        var existingRating = productRatingRepository.findByUserAndProduct(user, product);
        
        if (existingRating.isPresent()) {
            existingRating.get().setRatingValue(ratingValue);
            productRatingRepository.save(existingRating.get());
        } else {
            productRatingRepository.save(com.goviconnect.entity.ProductRating.builder()
                .user(user).product(product).ratingValue(ratingValue).build());
        }

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        return response;
    }

    @Transactional(readOnly = true)
    public java.util.Set<Long> getFavoriteProductIdsForUser(User user) {
        if (user == null) return java.util.Collections.emptySet();
        return productFavoriteRepository.findByUser(user).stream()
                .map(f -> f.getProduct().getId())
                .collect(java.util.stream.Collectors.toSet());
    }

    private String saveImage(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String filename = "market_" + UUID.randomUUID() + "_" + file.getOriginalFilename()
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/blog-images/" + filename;
    }
}
