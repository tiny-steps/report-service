package com.tinysteps.reportservice.controller;

import com.tinysteps.reportservice.model.ReportRequestDto;
import com.tinysteps.reportservice.model.ReportResponseDto;
import com.tinysteps.reportservice.model.ReportType;
import com.tinysteps.reportservice.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;
    
    @Value("${report.storage.path}")
    private String reportStoragePath;

    @PostMapping
    public ResponseEntity<ReportResponseDto> generateReport(@Valid @RequestBody ReportRequestDto reportRequest) {
        log.info("Received request to generate report of type: {}", reportRequest.getReportType());
        ReportResponseDto response = reportService.generateReport(reportRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<ReportResponseDto> getReport(@PathVariable Long reportId) {
        log.info("Retrieving report with ID: {}", reportId);
        ReportResponseDto response = reportService.getReportById(reportId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ReportResponseDto>> searchReports(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) ReportType reportType,
            @RequestParam(required = false) String branchId,
            @RequestParam(required = false) String userId) {
        log.info("Searching reports from {} to {} for branchId: {} and userId: {}",
                startDate, endDate, branchId, userId);
        List<ReportResponseDto> reports = reportService.searchReports(startDate, endDate, branchId, userId, reportType);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/{reportId}/download")
    public ResponseEntity<Resource> downloadReport(@PathVariable Long reportId) {
        log.info("Request to download report with ID: {}", reportId);
        
        // Get report details first
        ReportResponseDto report = reportService.getReportById(reportId);
        
        if (!"COMPLETED".equals(report.getStatus())) {
            log.error("Report {} is not completed. Status: {}", reportId, report.getStatus());
            return ResponseEntity.badRequest().build();
        }
        
        // Extract filename from download URL or construct it
        String filename;
        if (report.getDownloadUrl() != null) {
            filename = report.getDownloadUrl().substring(report.getDownloadUrl().lastIndexOf("/") + 1);
        } else {
            // Fallback: construct filename based on report details
            String extension = report.getFormat().toString().toLowerCase().equals("pdf") ? "pdf" : "xlsx";
            filename = String.format("%s_%s.%s", 
                report.getReportType().toString().toLowerCase(), 
                reportId, 
                extension);
        }
        
        Path filePath = Paths.get(reportStoragePath).resolve(filename).normalize();
        Resource resource = new FileSystemResource(filePath.toFile());
        
        if (!resource.exists()) {
            log.error("File not found: {}", filename);
            return ResponseEntity.notFound().build();
        }
        
        String contentType;
        try {
            contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = determineContentType(filename);
            }
        } catch (IOException e) {
            log.warn("Could not determine file type for {}", filename);
            contentType = determineContentType(filename);
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
    
    private String determineContentType(String filename) {
        if (filename.toLowerCase().endsWith(".pdf")) {
            return "application/pdf";
        } else if (filename.toLowerCase().endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else {
            return "application/octet-stream";
        }
    }
}
