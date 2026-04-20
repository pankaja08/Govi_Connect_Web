package com.goviconnect.service;

import com.goviconnect.entity.CropActivity;
import com.goviconnect.entity.CropLog;
import com.goviconnect.entity.User;
import com.goviconnect.repository.CropActivityRepository;
import com.goviconnect.repository.CropLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CropService {

    private final CropLogRepository cropLogRepository;
    private final CropActivityRepository cropActivityRepository;

    public List<CropLog> getCropLogsByUser(User user) {
        return cropLogRepository.findByUserOrderByPlantedDateDesc(user);
    }

    public List<CropLog> getAllCropLogs() {
        return cropLogRepository.findAll();
    }

    @Transactional
    public CropLog addCropLog(User user, String cropName, LocalDate plantedDate, LocalDate harvestExpectedDate,
            Double fieldSize, String seedVariety, String season) {
        CropLog cropLog = CropLog.builder()
                .user(user)
                .cropName(cropName)
                .plantedDate(plantedDate)
                .harvestExpectedDate(harvestExpectedDate)
                .fieldSize(fieldSize)
                .seedVariety(seedVariety)
                .season(season)
                .build();
        return cropLogRepository.save(cropLog);
    }

    @Transactional
    public CropLog updateCropYieldAndIncome(Long cropId, Double yieldAmount, Double incomeAmount) {
        CropLog cropLog = cropLogRepository.findById(cropId)
                .orElseThrow(() -> new IllegalArgumentException("Crop Log not found."));
        cropLog.setYieldAmount(yieldAmount);
        cropLog.setIncomeAmount(incomeAmount);
        return cropLogRepository.save(cropLog);
    }

    @Transactional
    public CropLog updateCropLog(Long id, String cropName, LocalDate plantedDate, LocalDate harvestExpectedDate,
                                Double fieldSize, String seedVariety, String season) {
        CropLog cropLog = cropLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Crop Log not found."));
        
        cropLog.setCropName(cropName);
        cropLog.setPlantedDate(plantedDate);
        cropLog.setHarvestExpectedDate(harvestExpectedDate);
        cropLog.setFieldSize(fieldSize);
        cropLog.setSeedVariety(seedVariety);
        cropLog.setSeason(season);
        
        return cropLogRepository.save(cropLog);
    }

    @Transactional
    public void deleteCropLog(Long id) {
        CropLog log = cropLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Crop Log not found."));
        
        // Delete associated activities first
        cropActivityRepository.deleteAll(log.getActivities());
        
        // Delete the log itself
        cropLogRepository.delete(log);
    }

    @Transactional
    public CropActivity addActivity(Long cropLogId, String activityType, String activityName, LocalDate activityDate) {
        CropLog cropLog = cropLogRepository.findById(cropLogId)
                .orElseThrow(() -> new IllegalArgumentException("Crop Log not found."));

        CropActivity activity = CropActivity.builder()
                .cropLog(cropLog)
                .activityType(activityType)
                .activityName(activityName)
                .activityDate(activityDate)
                .build();
        return cropActivityRepository.save(activity);
    }

    public Map<String, Double> calculateYieldPerSeason(User user) {
        List<CropLog> logs = getCropLogsByUser(user);
        Map<String, Double> yieldPerSeason = new HashMap<>();
        for (CropLog log : logs) {
            if (log.getSeason() != null && log.getYieldAmount() != null) {
                yieldPerSeason.put(log.getSeason(),
                        yieldPerSeason.getOrDefault(log.getSeason(), 0.0) + log.getYieldAmount());
            }
        }
        return yieldPerSeason;
    }

    public Map<String, Double> calculateIncomePerCrop(User user) {
        List<CropLog> logs = getCropLogsByUser(user);
        Map<String, Double> incomePerCrop = new HashMap<>();
        for (CropLog log : logs) {
            // Group by lowercased crop name to merge e.g., "Paddy" and "paddy"
            if (log.getCropName() != null && log.getIncomeAmount() != null) {
                String crop = log.getCropName().trim().toLowerCase();
                String capitalized = crop.substring(0, 1).toUpperCase() + crop.substring(1);
                incomePerCrop.put(capitalized, incomePerCrop.getOrDefault(capitalized, 0.0) + log.getIncomeAmount());
            }
        }
        return incomePerCrop;
    }

    public Map<String, List<Map<String, Object>>> calculateBestCropsPerSeason(String province, String district, String cropName, LocalDate startDate, LocalDate endDate) {
        List<CropLog> allLogs = getAllCropLogs();
        Map<String, List<Map<String, Object>>> bestCropsResponse = new HashMap<>();

        // Helper record/class equivalent to keep aggregations
        class Agg {
            double totalIncome = 0;
            double totalAcres = 0;
            double totalYield = 0;
        }

        // Season -> (CropName -> Agg)
        Map<String, Map<String, Agg>> aggregatedData = new HashMap<>();

        for (CropLog log : allLogs) {
            if (log.getSeason() == null || log.getCropName() == null) continue;
            
            // Filter by Province (via User)
            if (province != null && !province.isEmpty() && !province.equalsIgnoreCase("all")) {
                if (log.getUser() == null || log.getUser().getProvince() == null || !log.getUser().getProvince().equalsIgnoreCase(province)) {
                    continue;
                }
            }

            // Filter by District (via User)
            if (district != null && !district.isEmpty() && !district.equalsIgnoreCase("all")) {
                if (log.getUser() == null || log.getUser().getDistrict() == null || !log.getUser().getDistrict().equalsIgnoreCase(district)) {
                    continue;
                }
            }

            // Filter by Crop Name
            if (cropName != null && !cropName.isEmpty() && !cropName.equalsIgnoreCase("all")) {
                if (!log.getCropName().equalsIgnoreCase(cropName)) {
                    continue;
                }
            }

            // Filter by Date Range (Planted Date)
            if (startDate != null && log.getPlantedDate() != null && log.getPlantedDate().isBefore(startDate)) continue;
            if (endDate != null && log.getPlantedDate() != null && log.getPlantedDate().isAfter(endDate)) continue;

            // For analytical purposes, only count logs with actual data
            if (log.getIncomeAmount() == null && log.getYieldAmount() == null && log.getFieldSize() == null) continue;

            String season = log.getSeason().trim().toLowerCase();
            String currentCropName = log.getCropName().trim().toLowerCase();
            String capitalizedCrop = currentCropName.substring(0, 1).toUpperCase() + currentCropName.substring(1);

            aggregatedData.putIfAbsent(season, new HashMap<>());
            Agg agg = aggregatedData.get(season).computeIfAbsent(capitalizedCrop, k -> new Agg());

            if (log.getIncomeAmount() != null) agg.totalIncome += log.getIncomeAmount();
            if (log.getFieldSize() != null) agg.totalAcres += log.getFieldSize();
            if (log.getYieldAmount() != null) agg.totalYield += log.getYieldAmount();
        }

        // Calculate CPS and sort
        for (Map.Entry<String, Map<String, Agg>> seasonEntry : aggregatedData.entrySet()) {
            String season = seasonEntry.getKey();
            List<Map<String, Object>> cropStatsList = new java.util.ArrayList<>();

            for (Map.Entry<String, Agg> cropEntry : seasonEntry.getValue().entrySet()) {
                String crop = cropEntry.getKey();
                Agg agg = cropEntry.getValue();

                if (agg.totalAcres <= 0) continue; // Avoid division by zero

                double incomePerAcre = agg.totalIncome / agg.totalAcres;
                double yieldStability = agg.totalYield / agg.totalAcres;
                double cps = incomePerAcre * yieldStability;

                Map<String, Object> statMap = new HashMap<>();
                statMap.put("cropName", crop);
                statMap.put("cps", cps);
                statMap.put("incomePerAcre", incomePerAcre);
                statMap.put("totalAcres", agg.totalAcres);
                statMap.put("totalYield", agg.totalYield);
                statMap.put("totalIncome", agg.totalIncome);
                
                cropStatsList.add(statMap);
            }

            // Sort by CPS descending
            cropStatsList.sort((a, b) -> Double.compare((Double) b.get("cps"), (Double) a.get("cps")));

            // Take top 5
            int limit = Math.min(5, cropStatsList.size());
            bestCropsResponse.put(season, cropStatsList.subList(0, limit));
        }

        return bestCropsResponse;
    }

    public Map<String, Double> calculateCropDistribution(String province, String district, String cropName, LocalDate startDate, LocalDate endDate) {
        List<CropLog> allLogs = getAllCropLogs();
        Map<String, Double> distribution = new HashMap<>();

        for (CropLog log : allLogs) {
            if (log.getCropName() != null && log.getFieldSize() != null && log.getFieldSize() > 0) {
                // Filter by Province
                if (province != null && !province.isEmpty() && !province.equalsIgnoreCase("all")) {
                    if (log.getUser() == null || log.getUser().getProvince() == null || !log.getUser().getProvince().equalsIgnoreCase(province)) {
                        continue;
                    }
                }

                // Filter by District
                if (district != null && !district.isEmpty() && !district.equalsIgnoreCase("all")) {
                    if (log.getUser() == null || log.getUser().getDistrict() == null || !log.getUser().getDistrict().equalsIgnoreCase(district)) {
                        continue;
                    }
                }

                // Filter by Crop Name
                if (cropName != null && !cropName.isEmpty() && !cropName.equalsIgnoreCase("all")) {
                    if (!log.getCropName().equalsIgnoreCase(cropName)) {
                        continue;
                    }
                }

                // Filter by Date Range
                if (startDate != null && log.getPlantedDate() != null && log.getPlantedDate().isBefore(startDate)) continue;
                if (endDate != null && log.getPlantedDate() != null && log.getPlantedDate().isAfter(endDate)) continue;

                String crop = log.getCropName().trim().toLowerCase();
                String capitalizedCrop = crop.substring(0, 1).toUpperCase() + crop.substring(1);
                
                distribution.put(capitalizedCrop, distribution.getOrDefault(capitalizedCrop, 0.0) + log.getFieldSize());
            }
        }
        
        return distribution;
    }

    // Original methods without parameters for backward compatibility or simple calls
    public Map<String, List<Map<String, Object>>> calculateBestCropsPerSeason() {
        return calculateBestCropsPerSeason(null, null, null, null, null);
    }

    public Map<String, Double> calculateCropDistribution() {
        return calculateCropDistribution(null, null, null, null, null);
    }

    public List<String> getUniqueCropNames() {
        return getAllCropLogs().stream()
                .filter(log -> log.getCropName() != null && !log.getCropName().isBlank())
                .map(log -> {
                    String name = log.getCropName().trim().toLowerCase();
                    return name.substring(0, 1).toUpperCase() + name.substring(1);
                })
                .distinct()
                .sorted()
                .toList();
    }
}
