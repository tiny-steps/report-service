package com.tinysteps.reportservice.service;

import com.tinysteps.reportservice.model.ReportRequestDto;
import com.tinysteps.reportservice.model.ReportResponseDto;

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
}