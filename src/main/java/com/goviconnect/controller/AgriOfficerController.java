package com.goviconnect.controller;

import com.goviconnect.entity.User;
import com.goviconnect.exception.DuplicateImageException;
import com.goviconnect.service.BlogService;
import com.goviconnect.service.CropAdvisoryService;
import com.goviconnect.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/officer")
@PreAuthorize("hasRole('AGRI_OFFICER')")
@RequiredArgsConstructor
public class AgriOfficerController {

    private final BlogService blogService;
    private final UserService userService;
    private final com.goviconnect.service.ForumService forumService;
    private final CropAdvisoryService cropAdvisoryService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        User officer = userService.findByUsername(authentication.getName());
        model.addAttribute("officer", officer);
        model.addAttribute("myBlogs", blogService.getBlogsByAuthor(officer));
        model.addAttribute("questions", forumService.getAllActiveQuestions());
        model.addAttribute("locations", cropAdvisoryService.getAllLocations());
        model.addAttribute("seasons", cropAdvisoryService.getAllSeasons());
        model.addAttribute("soilTypes", cropAdvisoryService.getAllSoilTypes());
        return "officer/dashboard";
    }

    @PostMapping("/blog")
    public String createBlog(@RequestParam("heading") String heading,
            @RequestParam("textContent") String textContent,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "locationTag", required = false) String locationTag,
            @RequestParam(value = "seasonTag", required = false) String seasonTag,
            @RequestParam(value = "cropTag", required = false) String cropTag,
            @RequestParam(value = "farmingMethodTag", required = false) String farmingMethodTag,
            @RequestParam(value = "scheduledDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledDate,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            // 1. Title Validation (10-150 chars, trimmed)
            String trimmedHeading = heading != null ? heading.trim() : "";
            if (trimmedHeading.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Please enter an article title.");
                return "redirect:/officer/dashboard";
            }
            if (trimmedHeading.length() < 10) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Your title is a bit too short. Please use at least 10 characters.");
                return "redirect:/officer/dashboard";
            }
            if (trimmedHeading.length() > 150) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Your title is too long. Please keep it under 150 characters.");
                return "redirect:/officer/dashboard";
            }

            // 2. Content Validation (Min 50 words, HTML cleaning)
            String cleanContent = Jsoup.clean(textContent, Safelist.basicWithImages());
            String plainText = Jsoup.parse(textContent).text().trim();
            String[] words = plainText.split("\\s+");
            if (plainText.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Please add content to your blog.");
                return "redirect:/officer/dashboard";
            }
            if (words.length < 50) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Your blog post is quite short. Please aim for at least 50 words to provide value to the community.");
                return "redirect:/officer/dashboard";
            }

            // 3. Dropdown Validation (Predefined options check)
            List<String> validLocations = Arrays.asList("", "Western", "Central", "Southern", "Northern", "Eastern",
                    "North Western", "North Central", "Uva", "Sabaragamuwa");
            if (!validLocations.contains(locationTag)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Please select a valid location from the list.");
                return "redirect:/officer/dashboard";
            }
            // (Similarly for other tags if needed, but location is the most critical
            // example)

            // 4. Image Validation
            if (image != null && !image.isEmpty()) {
                String contentType = image.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                            "Invalid file type. Please upload a PNG, JPG, or WEBP image.");
                    return "redirect:/officer/dashboard";
                }
                if (image.getSize() > 5 * 1024 * 1024) { // 5MB limit
                    redirectAttributes.addFlashAttribute("errorMessage",
                            "This image is too large. Please upload a file smaller than 5MB.");
                    return "redirect:/officer/dashboard";
                }
            }

            // XSS Sanitization for title (just text)
            String safeHeading = Jsoup.clean(trimmedHeading, Safelist.none());

            User officer = userService.findByUsername(authentication.getName());
            blogService.createBlog(officer, safeHeading, cleanContent, image, locationTag, seasonTag, cropTag,
                    farmingMethodTag, scheduledDate);
            redirectAttributes.addFlashAttribute("successMessage", "Blog published successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to publish blog: " + e.getMessage());
        }
        return "redirect:/officer/dashboard";
    }

    @GetMapping("/blog/{id}/edit")
    public String editBlog(@PathVariable("id") Long id, Authentication authentication, Model model) {
        try {
            User officer = userService.findByUsername(authentication.getName());
            model.addAttribute("blog", blogService.getBlogByIdAndAuthor(id, officer));
            model.addAttribute("officer", officer);
            return "officer/edit-blog";
        } catch (Exception e) {
            return "redirect:/officer/dashboard?error=BlogNotFound";
        }
    }

    @PostMapping("/blog/{id}/edit")
    public String updateBlog(@PathVariable("id") Long id,
            @RequestParam("heading") String heading,
            @RequestParam("textContent") String textContent,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "locationTag", required = false) String locationTag,
            @RequestParam(value = "seasonTag", required = false) String seasonTag,
            @RequestParam(value = "cropTag", required = false) String cropTag,
            @RequestParam(value = "farmingMethodTag", required = false) String farmingMethodTag,
            @RequestParam(value = "scheduledDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledDate,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            User officer = userService.findByUsername(authentication.getName());
            blogService.updateBlog(id, officer, heading, textContent, image, locationTag, seasonTag, cropTag,
                    farmingMethodTag, scheduledDate);
            redirectAttributes.addFlashAttribute("successMessage", "Blog updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update blog: " + e.getMessage());
        }
        return "redirect:/officer/dashboard";
    }

    @PostMapping("/blog/{id}/delete")
    public String deleteBlog(@PathVariable("id") Long id, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            User officer = userService.findByUsername(authentication.getName());
            blogService.deleteBlog(id, officer);
            redirectAttributes.addFlashAttribute("successMessage", "Blog deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete blog: " + e.getMessage());
        }
        return "redirect:/officer/dashboard";
    }

    @GetMapping("/forum")
    public String forumAnswers(Authentication authentication, Model model) {
        User officer = userService.findByUsername(authentication.getName());
        model.addAttribute("officer", officer);
        model.addAttribute("questions", forumService.getAllActiveQuestions());
        return "officer/forum";
    }

    @PostMapping("/forum/answer/{id}")
    public String answerQuestion(@PathVariable("id") Long id, @RequestParam("content") String content,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User officer = userService.findByUsername(authentication.getName());
            forumService.answerQuestion(id, officer, content);
            redirectAttributes.addFlashAttribute("successMessage", "Answer posted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to post answer: " + e.getMessage());
        }
        redirectAttributes.addFlashAttribute("activeTab", "forum");
        return "redirect:/officer/dashboard";
    }

    @PostMapping("/forum/answer/{id}/edit")
    public String editAnswer(@PathVariable("id") Long id, @RequestParam("content") String content,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User officer = userService.findByUsername(authentication.getName());
            forumService.editAnswer(id, officer, content);
            redirectAttributes.addFlashAttribute("successMessage", "Answer updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update answer: " + e.getMessage());
        }
        redirectAttributes.addFlashAttribute("activeTab", "forum");
        return "redirect:/officer/dashboard";
    }

    @PostMapping("/forum/answer/{id}/delete")
    public String deleteAnswer(@PathVariable("id") Long id, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            User officer = userService.findByUsername(authentication.getName());
            forumService.deleteAnswer(id, officer);
            redirectAttributes.addFlashAttribute("successMessage", "Answer deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete answer: " + e.getMessage());
        }
        redirectAttributes.addFlashAttribute("activeTab", "forum");
        return "redirect:/officer/dashboard";
    }

    // ====================== CROP PROFILE ========================

    @PostMapping("/crop")
    public String createCrop(
            @RequestParam("cropName") String cropName,
            @RequestParam(value = "careInstructions", required = false) String careInstructions,
            @RequestParam(value = "cropImage", required = false) MultipartFile cropImage,
            @RequestParam(value = "locations", required = false) List<String> locationNames,
            @RequestParam(value = "seasons", required = false) List<String> seasonNames,
            @RequestParam(value = "soilTypes", required = false) List<String> soilTypeNames,
            @RequestParam(value = "fertilizers", required = false) List<String> fertilizerNames,
            @RequestParam(value = "diseases", required = false) List<String> diseaseNames,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            // Validate crop name
            String trimmedName = cropName != null ? cropName.trim() : "";
            if (trimmedName.isEmpty()) {
                redirectAttributes.addFlashAttribute("cropError", "Crop name is required.");
                redirectAttributes.addFlashAttribute("activeTab", "crop");
                return "redirect:/officer/dashboard";
            }
            if (trimmedName.length() < 2 || trimmedName.length() > 50) {
                redirectAttributes.addFlashAttribute("cropError", "Crop name must be 2–50 characters.");
                redirectAttributes.addFlashAttribute("activeTab", "crop");
                return "redirect:/officer/dashboard";
            }
            // Duplicate crop name check
            if (cropAdvisoryService.cropNameExists(trimmedName)) {
                redirectAttributes.addFlashAttribute("cropError",
                        "A crop named \"" + trimmedName + "\" already exists. Please use a different name.");
                redirectAttributes.addFlashAttribute("activeTab", "crop");
                return "redirect:/officer/dashboard";
            }
            // Validate required selections
            if (locationNames == null || locationNames.isEmpty()) {
                redirectAttributes.addFlashAttribute("cropError", "Please select at least one location.");
                redirectAttributes.addFlashAttribute("activeTab", "crop");
                return "redirect:/officer/dashboard";
            }
            if (seasonNames == null || seasonNames.isEmpty()) {
                redirectAttributes.addFlashAttribute("cropError", "Please select at least one season.");
                redirectAttributes.addFlashAttribute("activeTab", "crop");
                return "redirect:/officer/dashboard";
            }
            // Validate care instructions (Min 20 words)
            String trimmedInstructions = careInstructions != null ? careInstructions.trim() : "";
            if (trimmedInstructions.isEmpty()) {
                redirectAttributes.addFlashAttribute("cropError", "Care instructions are required.");
                redirectAttributes.addFlashAttribute("activeTab", "crop");
                return "redirect:/officer/dashboard";
            }
            String[] instrWords = trimmedInstructions.split("\\s+");
            if (instrWords.length < 20) {
                redirectAttributes.addFlashAttribute("cropError",
                        "Care instructions must be at least 20 words. (Currently: " + instrWords.length + ")");
                redirectAttributes.addFlashAttribute("activeTab", "crop");
                return "redirect:/officer/dashboard";
            }

            // Validate image (Mandatory)
            if (cropImage == null || cropImage.isEmpty()) {
                redirectAttributes.addFlashAttribute("cropError", "Please upload a crop image.");
                redirectAttributes.addFlashAttribute("activeTab", "crop");
                return "redirect:/officer/dashboard";
            } else {
                String ct = cropImage.getContentType();
                if (ct == null || !ct.startsWith("image/")) {
                    redirectAttributes.addFlashAttribute("cropError",
                            "Invalid image type. Please upload PNG, JPG, or WEBP.");
                    redirectAttributes.addFlashAttribute("activeTab", "crop");
                    return "redirect:/officer/dashboard";
                }
                if (cropImage.getSize() > 5 * 1024 * 1024) {
                    redirectAttributes.addFlashAttribute("cropError", "Image must be under 5MB.");
                    redirectAttributes.addFlashAttribute("activeTab", "crop");
                    return "redirect:/officer/dashboard";
                }
            }
            String safeCareInstructions = careInstructions != null ? Jsoup.clean(careInstructions, Safelist.none())
                    : "";
            cropAdvisoryService.createCrop(trimmedName, safeCareInstructions, cropImage,
                    locationNames, seasonNames, soilTypeNames, fertilizerNames, diseaseNames);
            redirectAttributes.addFlashAttribute("cropSuccess",
                    "Crop \"" + trimmedName + "\" added successfully to the advisory database!");
        } catch (DuplicateImageException e) {
            redirectAttributes.addFlashAttribute("cropError",
                    "Duplicate image detected: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("cropError", "Failed to save crop: " + e.getMessage());
        }
        redirectAttributes.addFlashAttribute("activeTab", "crop");
        return "redirect:/officer/dashboard";
    }
}
