package com.tinysteps.reportservice.service.generator;

import com.tinysteps.reportservice.model.AppointmentDto;
import com.tinysteps.reportservice.model.ReportRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class ExcelReportGenerator implements ReportGenerator {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void generateAppointmentReport(List<AppointmentDto> appointments, String outputPath, ReportRequestDto requestDto) {
        log.info("Generating Excel appointment report with {} appointments", appointments.size());
        
        try (Workbook workbook = new XSSFWorkbook()) {
            // Create sheet for report parameters
            Sheet paramSheet = workbook.createSheet("Report Info");
            createParametersSheet(paramSheet, requestDto);
            
            // Create sheet for appointments
            Sheet appointmentSheet = workbook.createSheet("Appointments");
            createAppointmentsSheet(appointmentSheet, appointments);
            
            // Auto-size columns
            for (int i = 0; i < 7; i++) {
                appointmentSheet.autoSizeColumn(i);
            }
            
            // Write to file
            try (FileOutputStream outputStream = new FileOutputStream(outputPath)) {
                workbook.write(outputStream);
            }
            
            log.info("Excel report successfully generated at: {}", outputPath);
            
        } catch (IOException e) {
            log.error("Failed to create Excel file", e);
            throw new RuntimeException("Failed to create Excel file: " + e.getMessage(), e);
        }
    }
    
    private void createParametersSheet(Sheet sheet, ReportRequestDto requestDto) {
        // Create header style
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        
        // Create title
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Appointment Summary Report");
        titleCell.setCellStyle(headerStyle);
        
        // Create parameters
        int rowNum = 2;
        
        Row headerRow = sheet.createRow(rowNum++);
        Cell headerCell = headerRow.createCell(0);
        headerCell.setCellValue("Report Parameters");
        headerCell.setCellStyle(headerStyle);
        
        // Date range
        if (requestDto.getStartDate() != null && requestDto.getEndDate() != null) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("Date Range:");
            row.createCell(1).setCellValue(
                    requestDto.getStartDate() + " to " + requestDto.getEndDate());
        }
        
        // Doctor filter
        if (requestDto.getDoctorId() != null) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("Doctor ID:");
            row.createCell(1).setCellValue(requestDto.getDoctorId());
        }
        
        // Patient filter
        if (requestDto.getPatientId() != null) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("Patient ID:");
            row.createCell(1).setCellValue(requestDto.getPatientId());
        }
        
        // Branch filter
        if (requestDto.getBranchId() != null) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("Branch ID:");
            row.createCell(1).setCellValue(requestDto.getBranchId());
        }
        
        // Generated at
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Generated At:");
        row.createCell(1).setCellValue(java.time.LocalDateTime.now().format(DATE_TIME_FORMATTER));
        
        // Auto-size columns
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }
    
    private void createAppointmentsSheet(Sheet sheet, List<AppointmentDto> appointments) {
        // Create header style
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Appointment Number", "Patient", "Doctor", "Type", "Date & Time", "Status", "Notes"};
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Create data rows
        int rowNum = 1;
        for (AppointmentDto appointment : appointments) {
            Row row = sheet.createRow(rowNum++);
            
            row.createCell(0).setCellValue(appointment.getAppointmentNumber() != null ? appointment.getAppointmentNumber() : appointment.getId() != null ? appointment.getId() : "");
            row.createCell(1).setCellValue(appointment.getPatientName() != null ? appointment.getPatientName() : appointment.getPatientId() != null ? appointment.getPatientId() : "");
            row.createCell(2).setCellValue(appointment.getDoctorName() != null ? appointment.getDoctorName() : appointment.getDoctorId() != null ? appointment.getDoctorId() : "");
            row.createCell(3).setCellValue(appointment.getConsultationType() != null ? appointment.getConsultationType() : "");
            row.createCell(4).setCellValue(appointment.getFormattedAppointmentTime());
            row.createCell(5).setCellValue(appointment.getStatus() != null ? appointment.getStatus() : "");
            row.createCell(6).setCellValue(appointment.getNotes() != null ? appointment.getNotes() : "");
        }
        
        // Create summary row
        Row summaryRow = sheet.createRow(rowNum + 1);
        Cell summaryLabelCell = summaryRow.createCell(0);
        summaryLabelCell.setCellValue("Total Appointments:");
        summaryLabelCell.setCellStyle(headerStyle);
        
        Cell summaryValueCell = summaryRow.createCell(1);
        summaryValueCell.setCellValue(appointments.size());
    }
}