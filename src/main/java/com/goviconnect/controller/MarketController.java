package com.goviconnect.controller;

import com.goviconnect.entity.User;
import com.goviconnect.enums.ProductCategory;
import com.goviconnect.enums.SaleType;
import com.goviconnect.enums.StockStatus;
import com.goviconnect.service.MarketProductService;
import com.goviconnect.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import java.util.regex.Pattern;
import java.math.BigDecimal;

@Controller
@RequestMapping("/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketProductService marketProductService;
    private final UserService userService;

    @GetMapping
    public String marketplace(
            @RequestParam(value = "category", required = false) String categoryStr,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "saleType", required = false) String saleTypeStr,
            @RequestParam(value = "favoritesOnly", required = false, defaultValue = "false") boolean favoritesOnly,
            @RequestParam(value = "inStockOnly", required = false, defaultValue = "false") boolean inStockOnly,
            Authentication authentication,
            Model model) {

        ProductCategory category = null;
        if (categoryStr != null && !categoryStr.isEmpty()) {
            try {
                category = ProductCategory.valueOf(categoryStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        SaleType saleType = null;
        if (saleTypeStr != null && !saleTypeStr.isEmpty()) {
            try {
                saleType = SaleType.valueOf(saleTypeStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        boolean isDefaultView = (category == null && minPrice == null && maxPrice == null
                && (search == null || search.isEmpty())
                && (location == null || location.isEmpty())
                && saleType == null
                && !inStockOnly
                && !favoritesOnly);

        java.util.List<com.goviconnect.entity.MarketProduct> products;
        if (isDefaultView) {
            products = marketProductService.getAllActiveProducts();
        } else {
            products = marketProductService.getFilteredProducts(category, minPrice, maxPrice, search, location, saleType, inStockOnly);
        }

        // Favorites Context
        java.util.Set<Long> favoriteProductIds = java.util.Collections.emptySet();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            User user = userService.findByUsername(authentication.getName());
            favoriteProductIds = marketProductService.getFavoriteProductIdsForUser(user);
        }

        if (favoritesOnly && !favoriteProductIds.isEmpty()) {
            final java.util.Set<Long> favs = favoriteProductIds;
            products = products.stream().filter(p -> favs.contains(p.getId())).collect(java.util.stream.Collectors.toList());
        } else if (favoritesOnly && favoriteProductIds.isEmpty()) {
            products = java.util.Collections.emptyList();
        }

        // Fetch top rated products for internal sorting/usage
        java.util.List<com.goviconnect.entity.MarketProduct> topRatedProducts = marketProductService.getTopRatedProducts();

        // Group and sort by category and rating for the view
        java.util.Map<ProductCategory, java.util.List<com.goviconnect.entity.MarketProduct>> categorizedProducts = new java.util.LinkedHashMap<>();
        java.util.List<ProductCategory> displayOrder = java.util.List.of(
                ProductCategory.VEGETABLES,
                ProductCategory.FRUITS,
                ProductCategory.SEEDS,
                ProductCategory.GRAINS,
                ProductCategory.FERTILIZERS,
                ProductCategory.EQUIPMENT
        );

        for (ProductCategory cat : displayOrder) {
            java.util.List<com.goviconnect.entity.MarketProduct> catProducts = products.stream()
                    .filter(p -> p.getCategory() == cat)
                    .sorted(java.util.Comparator.comparingDouble(com.goviconnect.entity.MarketProduct::getAverageRating).reversed())
                    .collect(java.util.stream.Collectors.toList());

            if (!catProducts.isEmpty()) {
                categorizedProducts.put(cat, catProducts);
            }
        }

        model.addAttribute("categorizedProducts", categorizedProducts);
        model.addAttribute("products", products);
        model.addAttribute("categories", ProductCategory.values());
        model.addAttribute("saleTypes", SaleType.values());
        model.addAttribute("selectedCategory", categoryStr);
        model.addAttribute("selectedSaleType", saleTypeStr);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("search", search);
        model.addAttribute("location", location);
        model.addAttribute("favoriteProductIds", favoriteProductIds);
        model.addAttribute("favoritesOnly", favoritesOnly);
        model.addAttribute("inStockOnly", inStockOnly);
        model.addAttribute("topRatedProducts", topRatedProducts);
        model.addAttribute("isDefaultView", isDefaultView);

        return "market/index";
    }

    @GetMapping("/list")
    public String listProductForm(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }
        User user = userService.findByUsername(authentication.getName());
        model.addAttribute("user", user);
        model.addAttribute("categories", ProductCategory.values());
        model.addAttribute("saleTypes", SaleType.values());
        return "market/list-product";
    }

    @GetMapping("/product/{id}")
    public String productDetails(@PathVariable("id") Long id, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        try {
            var product = marketProductService.getProductById(id);
            model.addAttribute("product", product);

            boolean isFavorite = false;
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                User user = userService.findByUsername(authentication.getName());
                isFavorite = marketProductService.getFavoriteProductIdsForUser(user).contains(id);
            }
            model.addAttribute("isFavorite", isFavorite);

            return "market/product-details";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Product not found: " + e.getMessage());
            return "redirect:/market";
        }
    }

    @PostMapping("/list")
    public String createListing(
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("price") BigDecimal price,
            @RequestParam("category") String categoryStr,
            @RequestParam(value = "saleType", required = false, defaultValue = "BOTH") String saleTypeStr,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam("quantity") Integer quantity,
            @RequestParam(value = "unit", required = false) String unit,
            @RequestParam(value = "contactNumber", required = false) String contactNumber,
            @RequestParam(value = "location", required = false) String location,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        try {
            // 1. Name Validation (3-100 chars, trimmed)
            String trimmedName = name != null ? name.trim() : "";
            if (trimmedName.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Please enter a product name.");
                return "redirect:/market/list";
            }
            if (trimmedName.length() < 3 || trimmedName.length() > 100) {
                redirectAttributes.addFlashAttribute("errorMessage", "Product name must be between 3 and 100 characters.");
                return "redirect:/market/list";
            }

            // 2. Price Validation (Positive, max 2 decimals)
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "Price must be greater than zero.");
                return "redirect:/market/list";
            }
            if (price.scale() > 2) {
                redirectAttributes.addFlashAttribute("errorMessage", "Price cannot have more than 2 decimal places.");
                return "redirect:/market/list";
            }

            // 3. Quantity Validation
            if (quantity == null || quantity <= 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "Quantity must be greater than zero.");
                return "redirect:/market/list";
            }

            // 4. Contact Number Validation (Optional but formatted)
            if (contactNumber != null && !contactNumber.isEmpty()) {
                if (!Pattern.matches("^0\\d{9}$", contactNumber.trim())) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Please enter a valid 10-digit phone number (e.g., 0712345699).");
                    return "redirect:/market/list";
                }
            }

            // 5. Image Validation
            if (image != null && !image.isEmpty()) {
                String contentType = image.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Invalid file type. Please upload a PNG, JPG, or WEBP image.");
                    return "redirect:/market/list";
                }
                if (image.getSize() > 5 * 1024 * 1024) { // 5MB
                    redirectAttributes.addFlashAttribute("errorMessage", "Image is too large. Keep it under 5MB.");
                    return "redirect:/market/list";
                }
            }

            // 6. Sanitization
            String safeDescription = description != null ? Jsoup.clean(description, Safelist.basic()) : "";
            if (safeDescription.length() > 1000) {
                redirectAttributes.addFlashAttribute("errorMessage", "Description is too long. Max 1000 characters.");
                return "redirect:/market/list";
            }
            String safeLocation = location != null ? Jsoup.clean(location, Safelist.none()) : "";
            if (safeLocation.length() > 50) {
                redirectAttributes.addFlashAttribute("errorMessage", "Location is too long. Max 50 characters.");
                return "redirect:/market/list";
            }

            User seller = userService.findByUsername(authentication.getName());
            ProductCategory category = ProductCategory.valueOf(categoryStr.toUpperCase());
            SaleType saleType = SaleType.BOTH;
            try { saleType = SaleType.valueOf(saleTypeStr.toUpperCase()); } catch (IllegalArgumentException ignored) {}
            marketProductService.createProduct(seller, trimmedName, safeDescription, price, category, image, quantity, unit, contactNumber, safeLocation, saleType);
            redirectAttributes.addFlashAttribute("successMessage", "Product listed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to list product: " + e.getMessage());
            return "redirect:/market/list";
        }
        return "redirect:/market";
    }

    @GetMapping("/{id}/edit")
    public String editProductForm(@PathVariable("id") Long id, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        try {
            var product = marketProductService.getProductById(id);
            User user = userService.findByUsername(authentication.getName());
            if (!product.getSeller().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to edit this product.");
                return "redirect:/user/profile";
            }
            model.addAttribute("product", product);
            model.addAttribute("stockStatuses", StockStatus.values());
            return "market/edit-product";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Product not found: " + e.getMessage());
            return "redirect:/user/profile";
        }
    }

    @PostMapping("/{id}/edit")
    public String updateProduct(
            @PathVariable("id") Long id,
            @RequestParam("description") String description,
            @RequestParam("price") BigDecimal price,
            @RequestParam("quantity") Integer quantity,
            @RequestParam("stockStatus") String stockStatusStr,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        try {
            // 1. Description Validation/Sanitization
            String safeDescription = description != null ? Jsoup.clean(description, Safelist.basic()) : "";
            if (safeDescription.length() > 1000) {
                redirectAttributes.addFlashAttribute("errorMessage", "Description is too long. Max 1000 characters.");
                return "redirect:/market/" + id + "/edit";
            }

            // 2. Price Validation
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "Price must be greater than zero.");
                return "redirect:/market/" + id + "/edit";
            }
            if (price.scale() > 2) {
                redirectAttributes.addFlashAttribute("errorMessage", "Price cannot have more than 2 decimal places.");
                return "redirect:/market/" + id + "/edit";
            }

            // 3. Quantity Validation
            if (quantity == null || quantity < 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "Quantity cannot be negative.");
                return "redirect:/market/" + id + "/edit";
            }

            User seller = userService.findByUsername(authentication.getName());
            StockStatus stockStatus = StockStatus.valueOf(stockStatusStr);
            marketProductService.updateProductDetails(id, seller, safeDescription, price, quantity, stockStatus);
            redirectAttributes.addFlashAttribute("successMessage", "Product updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update product: " + e.getMessage());
            return "redirect:/market/" + id + "/edit";
        }
        return "redirect:/user/profile";
    }

    @PostMapping("/{id}/delete")
    public String deleteProduct(@PathVariable("id") Long id, Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        try {
            User seller = userService.findByUsername(authentication.getName());
            marketProductService.deleteProduct(id, seller);
            redirectAttributes.addFlashAttribute("successMessage", "Product removed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to remove product: " + e.getMessage());
        }
        return "redirect:/market";
    }

    @PostMapping("/api/favorite/{id}")
    @ResponseBody
    public ResponseEntity<?> toggleFavorite(@PathVariable("id") Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).body(java.util.Map.of("error", "Unauthorized"));
        }
        try {
            User user = userService.findByUsername(authentication.getName());
            return ResponseEntity.ok(marketProductService.toggleFavorite(id, user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/rate/{id}")
    @ResponseBody
    public ResponseEntity<?> rateProduct(@PathVariable("id") Long id, @RequestParam("rating") int rating, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).body(java.util.Map.of("error", "Unauthorized"));
        }
        try {
            if (rating < 1 || rating > 5) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "Rating must be between 1 and 5"));
            }
            User user = userService.findByUsername(authentication.getName());
            return ResponseEntity.ok(marketProductService.rateProduct(id, user, rating));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
