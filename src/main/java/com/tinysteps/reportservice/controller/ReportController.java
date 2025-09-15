package com.tinysteps.reportservice.controller;

import com.tinysteps.reportservice.model.ReportRequestDto;
import com.tinysteps.reportservice.model.ReportResponseDto;
import com.tinysteps.reportservice.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

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
}