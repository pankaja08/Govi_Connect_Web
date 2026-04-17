package com.goviconnect.service;

import com.goviconnect.entity.*;
import com.goviconnect.exception.DuplicateImageException;
import com.goviconnect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CropAdvisoryService {

    private final AdvisoryCropRepository cropRepository;
    private final AdvisoryLocationRepository locationRepository;
    private final AdvisorySeasonRepository seasonRepository;
    private final AdvisorySoilTypeRepository soilTypeRepository;
    private final SavedCropRecommendationRepository savedCropRepository;

    @Value("${app.crop.image.dir:uploads/crop_images}")
    private String cropImageDir;

    public List<AdvisoryLocation> getAllLocations() {
        return locationRepository.findAll();
    }

    public List<AdvisorySeason> getAllSeasons() {
        return seasonRepository.findAll();
    }

    public List<AdvisorySoilType> getAllSoilTypes() {
        return soilTypeRepository.findAll();
    }

    public List<AdvisoryCrop> getRecommendations(String location, String season, String soil) {
        // Normalise soil: null/blank/"Any" → pass null so the IS NULL condition skips soil filter
        String soilParam = (soil == null || soil.isBlank() || soil.equalsIgnoreCase("Any")) ? null : soil.trim();
        List<Integer> ids = cropRepository.findCropIdsByCriteria(
                location != null ? location.trim() : location,
                season != null ? season.trim() : season,
                soilParam);
        if (ids.isEmpty()) return java.util.Collections.emptyList();
        return cropRepository.findByIdIn(ids);
    }

    /** Creates a new crop record with all related data (locations, seasons, soilTypes, fertilizers, diseases). */
    @Transactional
    public AdvisoryCrop createCrop(String cropName, String careInstructions,
                                    MultipartFile imageFile,
                                    List<String> locationNames,
                                    List<String> seasonNames,
                                    List<String> soilTypeNames,
                                    List<String> fertilizerNames,
                                    List<String> diseaseNames) throws IOException {

        // 1. Compute SHA-256 hash & check for duplicate image
        String imageUrl = null;
        String imageHash = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            imageHash = calculateSha256(imageFile.getInputStream());
            if (cropRepository.existsByImageHash(imageHash)) {
                throw new DuplicateImageException(
                    "This crop image has already been used. Please upload a unique image for this crop.");
            }

            // Save image file to uploads/crop_images/
            Path dir = Paths.get(cropImageDir);
            if (!Files.exists(dir)) Files.createDirectories(dir);
            String ext = "";
            String orig = imageFile.getOriginalFilename();
            if (orig != null && orig.contains(".")) {
                ext = orig.substring(orig.lastIndexOf('.'));
            }
            String filename = cropName.trim().toLowerCase().replaceAll("[^a-z0-9]", "_") + ext;
            Files.copy(imageFile.getInputStream(), dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            imageUrl = filename;
        }

        // 2. Resolve or create Location, Season, SoilType records
        List<AdvisoryLocation> locations = locationNames == null ? new java.util.ArrayList<>() :
            locationNames.stream()
                .filter(n -> n != null && !n.isBlank())
                .map(n -> locationRepository.findByName(n.trim())
                        .orElseGet(() -> locationRepository.save(AdvisoryLocation.builder().name(n.trim()).build())))
                .collect(java.util.stream.Collectors.toList());

        List<AdvisorySeason> seasons = seasonNames == null ? new java.util.ArrayList<>() :
            seasonNames.stream()
                .filter(n -> n != null && !n.isBlank())
                .map(n -> seasonRepository.findByName(n.trim())
                        .orElseGet(() -> seasonRepository.save(AdvisorySeason.builder().name(n.trim()).build())))
                .collect(java.util.stream.Collectors.toList());

        List<AdvisorySoilType> soilTypes = soilTypeNames == null ? new java.util.ArrayList<>() :
            soilTypeNames.stream()
                .filter(n -> n != null && !n.isBlank())
                .map(n -> soilTypeRepository.findByName(n.trim())
                        .orElseGet(() -> soilTypeRepository.save(AdvisorySoilType.builder().name(n.trim()).build())))
                .collect(java.util.stream.Collectors.toList());

        // 3. Build and save crop
        AdvisoryCrop crop = AdvisoryCrop.builder()
                .cropName(cropName.trim())
                .careInstructions(careInstructions)
                .imageUrl(imageUrl)
                .imageHash(imageHash)
                .locations(locations)
                .seasons(seasons)
                .soilTypes(soilTypes)
                .build();
        crop = cropRepository.save(crop);

        // 4. Add fertilizers
        if (fertilizerNames != null) {
            for (String name : fertilizerNames) {
                if (name != null && !name.isBlank()) {
                    crop.getFertilizers().add(Fertilizer.builder().name(name.trim()).crop(crop).build());
                }
            }
        }

        // 5. Add diseases
        if (diseaseNames != null) {
            for (String name : diseaseNames) {
                if (name != null && !name.isBlank()) {
                    crop.getDiseases().add(Disease.builder().name(name.trim()).crop(crop).build());
                }
            }
        }
        return cropRepository.save(crop);
    }

    /** SHA-256 hash helper — same logic as BlogImageService. */
    private String calculateSha256(InputStream is) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            byte[] hashBytes = digest.digest();
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /** Returns true if a crop with this name already exists (case-insensitive). */
    public boolean cropNameExists(String cropName) {
        return cropRepository.existsByCropNameIgnoreCase(cropName.trim());
    }

    public AdvisoryCrop getCropById(Integer id) {
        return cropRepository.findById(id).orElse(null);
    }

    @Transactional
    public SavedCropRecommendation saveRecommendation(User user, Integer cropId, String name, String location, String season, String soil) {
        AdvisoryCrop crop = cropRepository.findById(cropId)
                .orElseThrow(() -> new IllegalArgumentException("Crop not found"));

        SavedCropRecommendation saved = SavedCropRecommendation.builder()
                .user(user)
                .crop(crop)
                .recommendationName(name)
                .searchLocation(location)
                .searchSeason(season)
                .searchSoil(soil)
                .build();
        return savedCropRepository.save(saved);
    }

    public List<SavedCropRecommendation> getSavedRecommendations(User user) {
        return savedCropRepository.findByUserOrderBySavedAtDesc(user);
    }

    @Transactional
    public void deleteSavedRecommendation(Long id, User user) {
        SavedCropRecommendation saved = savedCropRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found"));
        if (!saved.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Access denied");
        }
        savedCropRepository.delete(saved);
    }
}

