package com.goviconnect.controller;

import com.goviconnect.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/admin/api/report")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping("/generate")
    public ResponseEntity<InputStreamResource> generateReport(
            @RequestParam(value = "province", required = false) String province,
            @RequestParam(value = "district", required = false) String district,
            @RequestParam(value = "crop", required = false) String crop,
            @RequestParam(value = "startDate", required = false) String startDateStr,
            @RequestParam(value = "endDate", required = false) String endDateStr) {

        log.info("Generating PDF report with filters: province={}, district={}, crop={}, startDate={}, endDate={}",
                province, district, crop, startDateStr, endDateStr);

        LocalDate startDate = (startDateStr != null && !startDateStr.isEmpty()) ? LocalDate.parse(startDateStr) : null;
        LocalDate endDate = (endDateStr != null && !endDateStr.isEmpty()) ? LocalDate.parse(endDateStr) : null;

        ByteArrayInputStream bis = reportService.generateAdminReport(province, district, crop, startDate, endDate);

        HttpHeaders headers = new HttpHeaders();
        String filename = "GoviConnect_Report_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";
        headers.add("Content-Disposition", "attachment; filename=" + filename);

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
    }
}
