package com.goviconnect.controller;

import com.goviconnect.entity.AdvisoryCrop;
import com.goviconnect.entity.SavedCropRecommendation;
import com.goviconnect.entity.User;
import com.goviconnect.service.CropAdvisoryService;
import com.goviconnect.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CropAdvisoryController {

    private final CropAdvisoryService advisoryService;
    private final UserService userService;

    /** Main advisory page — shows form + How It Works split layout */
    @GetMapping("/crop-advisory")
    public String cropAdvisory(Model model) {
        model.addAttribute("locations", advisoryService.getAllLocations());
        model.addAttribute("seasons", advisoryService.getAllSeasons());
        model.addAttribute("soilTypes", advisoryService.getAllSoilTypes());
        return "crop-advisory/index";
    }

    /** Results page — renders crop recommendation cards */
    @GetMapping("/crop-advisory/results")
    public String cropResults(
            @RequestParam(name = "location", required = false) String location,
            @RequestParam(name = "season", required = false) String season,
            @RequestParam(name = "soil", required = false, defaultValue = "") String soil,
            Model model) {
        List<AdvisoryCrop> crops = advisoryService.getRecommendations(location, season, soil);
        log.info("Found {} crop recommendations for location: {}, season: {}, soil: {}", 
                 crops != null ? crops.size() : 0, location, season, soil);
        model.addAttribute("crops", crops);
        model.addAttribute("location", location);
        model.addAttribute("season", season);
        model.addAttribute("soil", soil);
        return "crop-advisory/results";
    }

    /** REST API endpoint (kept for backward compatibility) */
    @GetMapping("/api/recommendations")
    @ResponseBody
    public List<AdvisoryCrop> getRecommendations(
            @RequestParam(name = "location", required = false) String location,
            @RequestParam(name = "season", required = false) String season,
            @RequestParam(name = "soil", required = false) String soil) {
        return advisoryService.getRecommendations(location, season, soil);
    }

    /** Save a recommendation */
    @PostMapping("/crop-advisory/save")
    @ResponseBody
    public ResponseEntity<?> saveRecommendation(
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Login required"));
        }

        try {
            User user = userService.findByUsername(authentication.getName());
            Integer cropId = Integer.valueOf(body.get("cropId"));
            String name = body.get("name");
            String location = body.get("location");
            String season = body.get("season");
            String soil = body.get("soil");

            advisoryService.saveRecommendation(user, cropId, name, location, season, soil);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** View saved recommendations page */
    @GetMapping("/crop-advisory/saved")
    public String viewSaved(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        User user = userService.findByUsername(authentication.getName());
        List<SavedCropRecommendation> saved = advisoryService.getSavedRecommendations(user);
        model.addAttribute("saved", saved);
        return "crop-advisory/saved";
    }

    /** Delete a saved recommendation */
    @PostMapping("/crop-advisory/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteSaved(
            @PathVariable Long id,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Login required"));
        }
        try {
            User user = userService.findByUsername(authentication.getName());
            advisoryService.deleteSavedRecommendation(id, user);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
