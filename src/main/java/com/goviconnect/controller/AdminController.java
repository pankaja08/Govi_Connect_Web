package com.goviconnect.controller;

import com.goviconnect.entity.AgriOfficerDetails;
import com.goviconnect.entity.User;
import com.goviconnect.enums.Role;
import com.goviconnect.service.CropService;
import com.goviconnect.service.MarketProductService;
import com.goviconnect.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final UserService userService;
    private final MarketProductService marketProductService;
    private final CropService cropService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<User> pendingOfficers = userService.getPendingOfficers();
        model.addAttribute("pendingOfficers", pendingOfficers);

        // Build a map of userId -> AgriOfficerDetails for pending officers
        Map<Long, AgriOfficerDetails> officerDetailsMap = new HashMap<>();
        for (User officer : pendingOfficers) {
            AgriOfficerDetails details = userService.getAgriOfficerDetails(officer);
            if (details != null) {
                officerDetailsMap.put(officer.getId(), details);
            }
        }
        model.addAttribute("officerDetailsMap", officerDetailsMap);

        model.addAttribute("allUsers", userService.getAllUsers());
        model.addAttribute("normalUsers", userService.getUsersByRole(Role.USER));
        model.addAttribute("officers", userService.getApprovedOfficers());
        model.addAttribute("admins", userService.getUsersByRole(Role.ADMIN));
        model.addAttribute("moderators", userService.getUsersByRole(Role.BLOG_MODERATOR));

        model.addAttribute("pendingProducts", marketProductService.getPendingProducts());
        model.addAttribute("farmerDistricts", userService.getFarmerCountByDistrict());
        model.addAttribute("allCrops", cropService.getUniqueCropNames());
        model.addAttribute("allProvincesList", userService.getAllProvinces());
        return "admin/dashboard";
    }

    @PostMapping("/approve/{id}")
    public String approve(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.approveOfficer(id);
            redirectAttributes.addFlashAttribute("successMessage", "Officer approved successfully.");
        } catch (Exception e) {
            log.error("Failed to approve officer ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to approve: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/reject/{id}")
    public String reject(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.rejectOfficer(id);
            redirectAttributes.addFlashAttribute("successMessage", "Officer registration rejected.");
        } catch (Exception e) {
            log.error("Failed to reject officer ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reject: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/product/approve/{id}")
    public String approveProduct(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            marketProductService.approveProduct(id);
            redirectAttributes.addFlashAttribute("successMessage", "Product approved successfully.");
        } catch (Exception e) {
            log.error("Failed to approve product ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to approve product: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/product/reject/{id}")
    public String rejectProduct(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            marketProductService.rejectProduct(id);
            redirectAttributes.addFlashAttribute("successMessage", "Product listing rejected.");
        } catch (Exception e) {
            log.error("Failed to reject product ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reject product: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/users/export/csv")
    public ResponseEntity<byte[]> exportUsersCsv() {
        try {
            StringBuilder csvBuilder = new StringBuilder();
            csvBuilder.append("ID,Full Name,Username,Email,NIC,Role,Account Status,District,Province,Contact Number\n");

            Role[] roles = {Role.ADMIN, Role.AGRI_OFFICER, Role.BLOG_MODERATOR, Role.USER};
            String[] sectionHeaders = {"ADMINISTRATORS", "AGRI OFFICERS / EXPERTS", "BLOG MODERATORS", "FARMERS / USERS"};

            for (int i = 0; i < roles.length; i++) {
                Role role = roles[i];
                List<User> usersInRole = userService.getUsersByRole(role);
                if (!usersInRole.isEmpty()) {
                    csvBuilder.append("\n--- ").append(sectionHeaders[i]).append(" ---\n");
                    for (User user : usersInRole) {
                        csvBuilder.append((user.getId() != null ? user.getId() : "")).append(",")
                                .append(escapeCsv(user.getFullName())).append(",")
                                .append(escapeCsv(user.getUsername())).append(",")
                                .append(escapeCsv(user.getEmail())).append(",")
                                .append(escapeCsv(user.getNic())).append(",")
                                .append(user.getRole() != null ? user.getRole().name() : "").append(",")
                                .append(user.getAccountStatus() != null ? user.getAccountStatus().name() : "").append(",")
                                .append(escapeCsv(user.getDistrict())).append(",")
                                .append(escapeCsv(user.getProvince())).append(",")
                                .append(escapeCsv(user.getContactNumber())).append("\n");
                    }
                }
            }

            byte[] csvBytes = csvBuilder.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "goviconnect_users_organized.csv");

            return new ResponseEntity<>(csvBytes, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to export users CSV", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @GetMapping("/api/user/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable("id") Long id) {
        try {
            User user = userService.getUserById(id);
            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("fullName", user.getFullName());
            data.put("nic", user.getNic());
            data.put("email", user.getEmail());
            data.put("contactNumber", user.getContactNumber());
            data.put("address", user.getAddress());
            data.put("district", user.getDistrict());
            data.put("province", user.getProvince());
            data.put("dob", user.getDob() != null ? user.getDob().toString() : "");
            data.put("accountStatus", user.getAccountStatus().name());
            data.put("role", user.getRole().name());
            data.put("username", user.getUsername());
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/officer-details/{userId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOfficerDetails(@PathVariable("userId") Long userId) {
        try {
            User user = userService.getUserById(userId);
            AgriOfficerDetails details = userService.getAgriOfficerDetails(user);
            Map<String, Object> data = new HashMap<>();
            if (details != null) {
                data.put("registrationNumber", details.getRegistrationNumber());
                data.put("designation", details.getDesignation());
                data.put("specializationArea", details.getSpecializationArea());
                data.put("assignedArea", details.getAssignedArea());
                data.put("officialEmail", details.getOfficialEmail());
            }
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/crop-analytics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCropAnalytics(
            @RequestParam(value = "province", required = false) String province,
            @RequestParam(value = "startDate", required = false) String startDateStr,
            @RequestParam(value = "endDate", required = false) String endDateStr) {
        try {
            LocalDate startDate = (startDateStr != null && !startDateStr.isEmpty()) ? LocalDate.parse(startDateStr) : null;
            LocalDate endDate = (endDateStr != null && !endDateStr.isEmpty()) ? LocalDate.parse(endDateStr) : null;

            Map<String, List<Map<String, Object>>> bestCrops = cropService.calculateBestCropsPerSeason(province, null, null, startDate, endDate);
            Map<String, Double> distribution = cropService.calculateCropDistribution(province, null, null, startDate, endDate);
            
            List<User> filteredUsers = userService.getAllUsers().stream()
                .filter(u -> province == null || province.equalsIgnoreCase("all") || (u.getProvince() != null && u.getProvince().equalsIgnoreCase(province)))
                .filter(u -> {
                    if (startDate == null && endDate == null) return true;
                    if (u.getCreatedAt() == null) return true;
                    if (startDate != null && u.getCreatedAt().isBefore(startDate)) return false;
                    if (endDate != null && u.getCreatedAt().isAfter(endDate)) return false;
                    return true;
                })
                .toList();
            
            Map<String, Long> userCounts = new HashMap<>();
            userCounts.put("farmers", filteredUsers.stream().filter(u -> u.getRole() == Role.USER).count());
            userCounts.put("officers", filteredUsers.stream().filter(u -> u.getRole() == Role.AGRI_OFFICER).count());
            userCounts.put("admins", filteredUsers.stream().filter(u -> u.getRole() == Role.ADMIN).count());

            Map<String, Long> districtCounts = new HashMap<>();
            userService.getAllDistricts().forEach(d -> {
                String normalized = toTitleCase(d);
                districtCounts.put(normalized, 0L);
            });

            filteredUsers.stream()
                .filter(u -> u.getRole() == Role.USER)
                .forEach(u -> {
                    String d = toTitleCase(u.getDistrict());
                    districtCounts.put(d, districtCounts.getOrDefault(d, 0L) + 1);
                });

            if (districtCounts.containsKey("Unknown") && districtCounts.get("Unknown") == 0) {
                districtCounts.remove("Unknown");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("bestCrops", bestCrops);
            response.put("distribution", distribution);
            response.put("userCounts", userCounts);
            response.put("districtCounts", districtCounts);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to fetch crop analytics", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/favorite-analytics")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getFavoriteAnalytics() {
        try {
            return ResponseEntity.ok(marketProductService.getMonthlyFavoriteAnalytics());
        } catch (Exception e) {
            log.error("Failed to fetch favorite analytics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/seed-favorites")
    @ResponseBody
    public String seedFavorites() {
        try {
            List<Long> productIds = jdbcTemplate.queryForList("SELECT id FROM market_products", Long.class);
            List<Long> userIds = jdbcTemplate.queryForList("SELECT id FROM users", Long.class);
            if (productIds.isEmpty() || userIds.isEmpty()) return "Need users and products in DB.";

            int count = 0;
            for (int i = 0; i < Math.min(productIds.size(), 10); i++) {
                for (int j = 0; j < Math.min(userIds.size(), 5); j++) {
                    int month = (i % 5) + 1;
                    String date = "2024-0" + month + "-10";
                    try {
                        jdbcTemplate.update("INSERT INTO product_favorite (product_id, user_id, created_date) VALUES (?, ?, ?)",
                            productIds.get(i), userIds.get(j), date);
                        count++;
                    } catch (Exception e) {}
                }
            }
            return "Seeded " + count + " favorites.";
        } catch (Exception e) { return "Error: " + e.getMessage(); }
    }

    @PostMapping("/user/update/{id}")
    public String updateUser(@PathVariable("id") Long id,
            @RequestParam("fullName") String fullName,
            @RequestParam("nic") String nic,
            @RequestParam("email") String email,
            @RequestParam("contactNumber") String contactNumber,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "district", required = false) String district,
            @RequestParam(value = "province", required = false) String province,
            @RequestParam(value = "dob", required = false) String dob,
            @RequestParam(value = "accountStatus", required = false) String accountStatus,
            RedirectAttributes redirectAttributes) {
        try {
            userService.updateUser(id, fullName, nic, email, contactNumber, address, district, province, dob, accountStatus);
            redirectAttributes.addFlashAttribute("successMessage", "User details updated successfully.");
        } catch (Exception e) {
            log.error("Failed to update user ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/user/delete/{id}")
    public String deleteUser(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully.");
        } catch (Exception e) {
            log.error("Failed to delete user ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/user/create")
    public String createUser(@RequestParam("fullName") String fullName,
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("nic") String nic,
            @RequestParam("password") String password,
            @RequestParam("role") String roleStr,
            @RequestParam(value = "contactNumber", required = false) String contactNumber,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "district", required = false) String district,
            @RequestParam(value = "province", required = false) String province,
            @RequestParam(value = "dob", required = false) String dob,
            @RequestParam(value = "registrationNumber", required = false) String registrationNumber,
            @RequestParam(value = "designation", required = false) String designation,
            @RequestParam(value = "specializationArea", required = false) String specializationArea,
            @RequestParam(value = "assignedArea", required = false) String assignedArea,
            @RequestParam(value = "officialEmail", required = false) String officialEmail,
            RedirectAttributes redirectAttributes) {
        try {
            if (fullName.isBlank() || username.isBlank() || email.isBlank() || nic.isBlank() || password.isBlank()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Required fields are missing.");
                return "redirect:/admin/dashboard";
            }
            Role role = Role.valueOf(roleStr);
            userService.createUser(fullName, username, email, nic, password, role, contactNumber, address, district, province, 
                    LocalDate.parse(dob), registrationNumber, designation, specializationArea, assignedArea, officialEmail);
            redirectAttributes.addFlashAttribute("successMessage", "User created successfully.");
        } catch (Exception e) {
            log.error("Failed to create user: {}", username, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to create: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    private String toTitleCase(String str) {
        if (str == null || str.isBlank()) return "Unknown";
        String[] words = str.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase()).append(" ");
        }
        return sb.toString().trim();
    }
}
