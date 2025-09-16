package com.tinysteps.reportservice.service;

import com.tinysteps.reportservice.model.ReportRequestDto;
import com.tinysteps.reportservice.model.ReportResponseDto;
import com.tinysteps.reportservice.model.ReportType;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    
    /**
     * Generates a report based on the provided request parameters
     * 
     * @param reportRequest the report generation request
     * @return the generated report response with download information
     */
    ReportResponseDto generateReport(ReportRequestDto reportRequest);
    
    /**
     * Retrieves a report by its ID
     * 
     * @param reportId the ID of the report to retrieve
     * @return the report response
     */
    ReportResponseDto getReportById(Long reportId);
    
    /**
     * Searches for reports within a date range with optional filtering
     * 
     * @param startDate the start date for the search range
     * @param endDate the end date for the search range
     * @param branchId optional branch ID filter (can be "all" for all branches)
     * @param userId optional user ID filter
     * @return list of matching reports
     */
    List<ReportResponseDto> searchReports(LocalDate startDate, LocalDate endDate, String branchId, String userId, ReportType reportType);
}