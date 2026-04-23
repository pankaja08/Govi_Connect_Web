package com.goviconnect.service;

import com.goviconnect.entity.User;
import com.goviconnect.enums.Role;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final UserService userService;
    private final CropService cropService;

    public ByteArrayInputStream generateAdminReport(String province, String district, String cropName, LocalDate startDate, LocalDate endDate) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.BLACK);
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(22, 101, 52)); // gc-dark
            Font normalBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font smallItalic = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);

            // Header
            Paragraph title = new Paragraph("GOVI CONNECT - Agricultural Insights", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph period = new Paragraph("Report Generated on: " + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), smallItalic);
            period.setAlignment(Element.ALIGN_CENTER);
            period.setSpacingAfter(20);
            document.add(period);

            // Filter Summary
            Paragraph filters = new Paragraph();
            filters.add(new Chunk("Filters Applied: ", normalBold));
            if (province != null && !province.equalsIgnoreCase("all")) filters.add(new Chunk("Province: " + province + " | ", normal));
            filters.add(new Chunk("District: " + (district == null ? "All" : district) + " | ", normal));
            filters.add(new Chunk("Crop: " + (cropName == null ? "All" : cropName) + " | ", normal));
            filters.add(new Chunk("Date Range: " + (startDate == null ? "Any" : startDate) + " to " + (endDate == null ? "Any" : endDate), normal));
            filters.setSpacingAfter(20);
            document.add(filters);

            // Section 1: Platform Overview
            document.add(new Paragraph("1. Platform Overview", subTitleFont));
            document.add(new Paragraph("Summary of platform activity and user registration metrics.", smallItalic));
            document.add(new Paragraph(" ", normal));

            PdfPTable overviewTable = new PdfPTable(4);
            overviewTable.setWidthPercentage(100);
            overviewTable.setSpacingBefore(10);
            overviewTable.setSpacingAfter(20);

            addTableCell(overviewTable, "Total Users", normalBold, true);
            addTableCell(overviewTable, "Farmers", normalBold, true);
            addTableCell(overviewTable, "Agri Officers", normalBold, true);
            addTableCell(overviewTable, "Districts Involved", normalBold, true);

            List<User> allUsers = userService.getAllUsers();
            
            // Apply Filters to Overview if applicable
            List<User> filteredUsers = allUsers.stream()
                .filter(u -> province == null || province.equalsIgnoreCase("all") || (u.getProvince() != null && u.getProvince().equalsIgnoreCase(province)))
                .filter(u -> district == null || district.equalsIgnoreCase("all") || (u.getDistrict() != null && u.getDistrict().equalsIgnoreCase(district)))
                .filter(u -> {
                    if (startDate == null && endDate == null) return true;
                    if (u.getCreatedAt() == null) return true;
                    if (startDate != null && u.getCreatedAt().isBefore(startDate)) return false;
                    if (endDate != null && u.getCreatedAt().isAfter(endDate)) return false;
                    return true;
                })
                .toList();

            long farmersCount = filteredUsers.stream().filter(u -> u.getRole() == Role.USER).count();
            long officersCount = filteredUsers.stream().filter(u -> u.getRole() == Role.AGRI_OFFICER).count();
            long distinctDistricts = filteredUsers.stream().map(User::getDistrict).filter(d -> d != null && !d.isBlank()).distinct().count();

            addTableCell(overviewTable, String.valueOf(filteredUsers.size()), normal, false);
            addTableCell(overviewTable, String.valueOf(farmersCount), normal, false);
            addTableCell(overviewTable, String.valueOf(officersCount), normal, false);
            addTableCell(overviewTable, String.valueOf(distinctDistricts), normal, false);

            document.add(overviewTable);

            // Section 2: Crop Performance Leaderboard
            document.add(new Paragraph("2. Seasonal Crop Performance (CPS)", subTitleFont));
            document.add(new Paragraph("The Crop Performance Score (CPS) is calculated as: (Income/Acre) x (Yield/Acre).", smallItalic));
            document.add(new Paragraph(" ", normal));

            Map<String, List<Map<String, Object>>> bestCrops = cropService.calculateBestCropsPerSeason(province, district, cropName, startDate, endDate);

            for (String season : bestCrops.keySet()) {
                Paragraph seasonHeader = new Paragraph("Season: " + season.toUpperCase(), normalBold);
                seasonHeader.setSpacingBefore(10);
                document.add(seasonHeader);

                PdfPTable cropTable = new PdfPTable(5);
                cropTable.setWidthPercentage(100);
                cropTable.setSpacingBefore(5);
                cropTable.setSpacingAfter(15);
                cropTable.setWidths(new float[]{3, 2, 2, 2, 2});

                addTableCell(cropTable, "Crop Name", normalBold, true);
                addTableCell(cropTable, "Total Acres", normalBold, true);
                addTableCell(cropTable, "Total Yield (Kg)", normalBold, true);
                addTableCell(cropTable, "Total Income (LKR)", normalBold, true);
                addTableCell(cropTable, "CPS Score", normalBold, true);

                for (Map<String, Object> stat : bestCrops.get(season)) {
                    addTableCell(cropTable, (String) stat.get("cropName"), normal, false);
                    addTableCell(cropTable, String.format("%.2f", (Double) stat.get("totalAcres")), normal, false);
                    addTableCell(cropTable, String.format("%.2f", (Double) stat.get("totalYield")), normal, false);
                    addTableCell(cropTable, String.format("%.2f", (Double) stat.get("totalIncome")), normal, false);
                    addTableCell(cropTable, String.format("%.2f", (Double) stat.get("cps")), normal, false);
                }
                document.add(cropTable);
            }

            // Section 3: Land Distribution breakdown
            document.add(new Paragraph("3. Land Distribution Analysis", subTitleFont));
            document.add(new Paragraph("Detailed breakdown of acreage allocated per crop variety.", smallItalic));
            document.add(new Paragraph(" ", normal));

            Map<String, Double> distribution = cropService.calculateCropDistribution(province, district, cropName, startDate, endDate);
            
            PdfPTable distTable = new PdfPTable(2);
            distTable.setWidthPercentage(60);
            distTable.setSpacingBefore(10);
            distTable.setHorizontalAlignment(Element.ALIGN_LEFT);

            addTableCell(distTable, "Crop Variety", normalBold, true);
            addTableCell(distTable, "Allocated Land (Acres)", normalBold, true);

            double totalAcres = 0;
            for (Map.Entry<String, Double> entry : distribution.entrySet()) {
                addTableCell(distTable, entry.getKey(), normal, false);
                addTableCell(distTable, String.format("%.2f", entry.getValue()), normal, false);
                totalAcres += entry.getValue();
            }
            
            // Total Footer
            addTableCell(distTable, "TOTAL", normalBold, true);
            addTableCell(distTable, String.format("%.2f", totalAcres), normalBold, true);

            document.add(distTable);

            document.close();

        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void addTableCell(PdfPTable table, String text, Font font, boolean isHeader) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        if (isHeader) {
            cell.setBackgroundColor(new Color(243, 244, 246)); // gray-100
        }
        table.addCell(cell);
    }
}
