package com.goviconnect.controller;

import com.goviconnect.entity.User;
import com.goviconnect.entity.CropLog;
import com.goviconnect.entity.MarketProduct;
import com.goviconnect.service.CropService;
import com.goviconnect.service.MarketProductService;
import com.goviconnect.service.ProductMessageService;
import com.goviconnect.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user/profile")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final UserService userService;
    private final CropService cropService;
    private final MarketProductService marketProductService;
    private final ProductMessageService productMessageService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @GetMapping
    public String showProfile(Model model, Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        model.addAttribute("user", user);

        if (user.getRole() == com.goviconnect.enums.Role.AGRI_OFFICER) {
            com.goviconnect.entity.AgriOfficerDetails officerDetails = userService.getAgriOfficerDetails(user);
            model.addAttribute("officerDetails", officerDetails);
        }

        List<CropLog> cropLogs = cropService.getCropLogsByUser(user);
        Map<String, Double> yieldPerSeason = cropService.calculateYieldPerSeason(user);
        Map<String, Double> incomePerCrop = cropService.calculateIncomePerCrop(user);
        List<MarketProduct> myProducts = marketProductService.getProductsBySeller(user);

        String cropLogsJson = "[]";
        try {
            cropLogsJson = objectMapper.writeValueAsString(cropLogs);
        } catch (Exception e) {
            log.error("Failed to serialize cropLogs to JSON", e);
        }

        model.addAttribute("cropLogs", cropLogs);
        model.addAttribute("cropLogsJson", cropLogsJson);
        model.addAttribute("yieldPerSeason", yieldPerSeason);
        model.addAttribute("incomePerCrop", incomePerCrop);
        model.addAttribute("myProducts", myProducts);
        model.addAttribute("today", java.time.LocalDate.now());

        // Messaging data
        List<ProductMessageService.ConversationSummary> sellerInbox = productMessageService.getSellerInbox(user);
        List<ProductMessageService.ConversationSummary> buyerSent = productMessageService.getBuyerSent(user);
        long unreadCount = productMessageService.getUnreadCount(user);
        model.addAttribute("sellerInbox", sellerInbox);
        model.addAttribute("buyerSent", buyerSent);
        model.addAttribute("unreadCount", unreadCount);

        return "user/profile";
    }

    @PostMapping("/update")
    public String updateProfile(
            @RequestParam("fullName") String fullName,
            @RequestParam("nic") String nic,
            @RequestParam("email") String email,
            @RequestParam("contactNumber") String contactNumber,
            @RequestParam("address") String address,
            @RequestParam("district") String district,
            @RequestParam("province") String province,
            @RequestParam("dob") String dob,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            // Validation: Contact Number
            if (contactNumber == null || !contactNumber.matches("^0\\d{9}$")) {
                throw new IllegalArgumentException("Contact number must start with 0 and have exactly 10 digits.");
            }

            // Validation: Date of Birth (Must be at least 16 years old)
            if (dob != null && !dob.isEmpty()) {
                LocalDate birthDate = LocalDate.parse(dob);
                LocalDate sixteenYearsAgo = LocalDate.now().minusYears(16);
                if (birthDate.isAfter(sixteenYearsAgo)) {
                    throw new IllegalArgumentException("You must be at least 16 years old.");
                }
            }

            String username = authentication.getName();
            User user = userService.findByUsername(username);

            userService.updateUserProfile(user.getId(), fullName, nic, email, contactNumber, address, district,
                    province, dob);

            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
        } catch (IllegalArgumentException e) {
            log.warn("Validation failed for profile update: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error updating profile", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update profile: " + e.getMessage());
        }

        return "redirect:/user/profile";
    }

    @PostMapping("/crop/add")
    public String addCropLog(@RequestParam("cropName") String cropName,
            @RequestParam("plantedDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate plantedDate,
            @RequestParam("harvestExpectedDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate harvestExpectedDate,
            @RequestParam("fieldSize") Double fieldSize,
            @RequestParam(value = "seedVariety", required = false) String seedVariety,
            @RequestParam(value = "season", required = false) String season,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            validateCropLogData(cropName, plantedDate, harvestExpectedDate, fieldSize, seedVariety, season);
            
            User user = userService.findByUsername(authentication.getName());
            cropService.addCropLog(user, cropName, plantedDate, harvestExpectedDate, fieldSize, seedVariety, season);
            redirectAttributes.addFlashAttribute("successMessage", "Crop tracking log created successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error creating crop log", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to add crop log.");
        }
        return "redirect:/user/profile";
    }

    @PostMapping("/crop/{id}/edit")
    public String editCropLog(@PathVariable("id") Long id,
            @RequestParam("cropName") String cropName,
            @RequestParam("plantedDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate plantedDate,
            @RequestParam("harvestExpectedDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate harvestExpectedDate,
            @RequestParam("fieldSize") Double fieldSize,
            @RequestParam(value = "seedVariety", required = false) String seedVariety,
            @RequestParam(value = "season", required = false) String season,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            // Verify ownership
            User user = userService.findByUsername(authentication.getName());
            List<CropLog> userLogs = cropService.getCropLogsByUser(user);
            boolean ownsLog = userLogs.stream().anyMatch(l -> l.getId().equals(id));
            if (!ownsLog) {
                throw new IllegalAccessException("Unauthorized to edit this log.");
            }

            validateCropLogData(cropName, plantedDate, harvestExpectedDate, fieldSize, seedVariety, season);
            
            cropService.updateCropLog(id, cropName, plantedDate, harvestExpectedDate, fieldSize, seedVariety, season);
            redirectAttributes.addFlashAttribute("successMessage", "Crop log updated successfully!");
        } catch (IllegalArgumentException | IllegalAccessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error updating crop log", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update crop log.");
        }
        return "redirect:/user/profile";
    }

    @PostMapping("/crop/{id}/delete")
    public String deleteCropLog(@PathVariable("id") Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            // Verify ownership
            User user = userService.findByUsername(authentication.getName());
            List<CropLog> userLogs = cropService.getCropLogsByUser(user);
            boolean ownsLog = userLogs.stream().anyMatch(l -> l.getId().equals(id));
            if (!ownsLog) {
                throw new IllegalAccessException("Unauthorized to delete this log.");
            }

            cropService.deleteCropLog(id);
            redirectAttributes.addFlashAttribute("successMessage", "Crop log deleted successfully!");
        } catch (IllegalAccessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting crop log", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete crop log.");
        }
        return "redirect:/user/profile";
    }

    private void validateCropLogData(String cropName, LocalDate plantedDate, LocalDate harvestExpectedDate, 
                                    Double fieldSize, String seedVariety, String season) {
        // 1. Crop Name
        if (cropName == null || cropName.trim().isEmpty()) {
            throw new IllegalArgumentException("Crop name is required.");
        }
        if (cropName.length() < 2 || cropName.length() > 50) {
            throw new IllegalArgumentException("Crop name must be between 2 and 50 characters.");
        }
        if (!cropName.matches("^[a-zA-Z\\s]+$")) {
            throw new IllegalArgumentException("Crop name can only contain letters and spaces.");
        }

        // 2. Season
        if (season == null || (!season.equals("Yala") && !season.equals("Maha") && !season.equals("Inter-season"))) {
            throw new IllegalArgumentException("Please select a valid season (Yala, Maha, or Inter-season).");
        }

        // 3. Dates
        if (plantedDate == null || harvestExpectedDate == null) {
            throw new IllegalArgumentException("Both planted and harvest dates are required.");
        }
        if (plantedDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Planted date cannot be in the future.");
        }
        if (harvestExpectedDate.isBefore(plantedDate) || harvestExpectedDate.isEqual(plantedDate)) {
            throw new IllegalArgumentException("Expected harvest date must be after the planted date.");
        }
        if (harvestExpectedDate.isAfter(plantedDate.plusYears(5))) {
            throw new IllegalArgumentException("Expected harvest date cannot be more than 5 years away.");
        }

        // 4. Field Size
        if (fieldSize == null || fieldSize <= 0) {
            throw new IllegalArgumentException("Field size must be greater than 0.");
        }
        if (fieldSize > 10000) {
            throw new IllegalArgumentException("Field size cannot exceed 10,000 acres.");
        }

        // 5. Seed Variety
        if (seedVariety != null && seedVariety.length() > 100) {
            throw new IllegalArgumentException("Seed variety must be under 100 characters.");
        }
    }

    @PostMapping("/crop/{id}/activity/add")
    public String addCropActivity(@PathVariable("id") Long cropId,
            @RequestParam("activityType") String activityType,
            @RequestParam("activityName") String activityName,
            @RequestParam("activityDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate activityDate,
            RedirectAttributes redirectAttributes) {
        try {
            cropService.addActivity(cropId, activityType, activityName, activityDate);
            redirectAttributes.addFlashAttribute("successMessage", "Activity added successfully!");
        } catch (Exception e) {
            log.error("Error adding activity", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to add activity.");
        }
        return "redirect:/user/profile";
    }

    @PostMapping("/crop/{id}/update-analytics")
    public String updateCropAnalytics(@PathVariable("id") Long cropId,
            @RequestParam("yieldAmount") Double yieldAmount,
            @RequestParam("incomeAmount") Double incomeAmount,
            RedirectAttributes redirectAttributes) {
        try {
            cropService.updateCropYieldAndIncome(cropId, yieldAmount, incomeAmount);
            redirectAttributes.addFlashAttribute("successMessage", "Crop performance updated!");
        } catch (Exception e) {
            log.error("Error updating crop analytics", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update analytics.");
        }
        return "redirect:/user/profile";
    }

    @GetMapping("/debug/{username}")
    @org.springframework.web.bind.annotation.ResponseBody
    public String debugUser(@org.springframework.web.bind.annotation.PathVariable("username") String username) {
        try {
            User user = userService.findByUsername(username);
            com.goviconnect.entity.AgriOfficerDetails details = userService.getAgriOfficerDetails(user);
            return "User: " + user.getUsername() + ", Role: " + user.getRole() +
                    "<br>Officer Details Found: " + (details != null);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
