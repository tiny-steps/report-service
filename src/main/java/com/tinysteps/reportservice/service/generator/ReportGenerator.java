package com.tinysteps.reportservice.service.generator;

import com.tinysteps.reportservice.model.AppointmentDto;
import com.tinysteps.reportservice.model.ReportRequestDto;

import java.util.List;

/**
 * Interface for report generation implementations
 */
public interface ReportGenerator {
    
    /**
     * Generates an appointment report with the provided data
     * 
     * @param appointments list of appointments to include in the report
     * @param outputPath path where the report file should be saved
     * @param requestDto the original report request with parameters
     */
    void generateAppointmentReport(List<AppointmentDto> appointments, String outputPath, ReportRequestDto requestDto);
}