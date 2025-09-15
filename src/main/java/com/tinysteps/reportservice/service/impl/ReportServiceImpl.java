package com.tinysteps.reportservice.service.impl;

import com.tinysteps.reportservice.client.DoctorServiceClient;
import com.tinysteps.reportservice.client.PatientServiceClient;
import com.tinysteps.reportservice.client.ScheduleServiceClient;
import com.tinysteps.reportservice.client.UserServiceClient;
import com.tinysteps.reportservice.entity.Report;
import com.tinysteps.reportservice.model.*;
import com.tinysteps.reportservice.repository.ReportRepository;
import com.tinysteps.reportservice.service.ReportService;
import com.tinysteps.reportservice.service.generator.PdfReportGenerator;
import com.tinysteps.reportservice.service.generator.ExcelReportGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final ScheduleServiceClient scheduleServiceClient;
    private final PatientServiceClient patientServiceClient;
    private final DoctorServiceClient doctorServiceClient;
    private final UserServiceClient userServiceClient;
    private final PdfReportGenerator pdfReportGenerator;
    private final ExcelReportGenerator excelReportGenerator;
    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;
    
    @Value("${report.storage.path}")
    private String reportStoragePath;
    
    @Value("${report.download.base-url}")
    private String reportDownloadBaseUrl;
    
    @Value("${service.internal-secret}")
    private String internalSecret;
    
    @Value("${kafka.topics.report-events}")
    private String reportEventsTopic;

    @Override
    @Transactional
    public ReportResponseDto generateReport(ReportRequestDto reportRequest) {
        log.info("Generating report of type: {}", reportRequest.getReportType());
        
        // Create report entity
        Report report = Report.builder()
                .title(generateReportTitle(reportRequest))
                .reportType(reportRequest.getReportType())
                .format(reportRequest.getFormat())
                .userId(reportRequest.getUserId())
                .status("PROCESSING")
                .generatedAt(LocalDateTime.now())
                .build();
        
        report = reportRepository.save(report);
        
        // Generate unique filename
        String filename = generateFilename(report);
        Path reportPath = Paths.get(reportStoragePath, filename);
        
        // Ensure the reports directory exists
        try {
            Path reportsDir = Paths.get(reportStoragePath);
            if (!Files.exists(reportsDir)) {
                Files.createDirectories(reportsDir);
                log.info("Created reports directory: {}", reportsDir.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Failed to create reports directory: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create reports directory", e);
        }
        
        try {
            // Fetch data based on report type
            if (reportRequest.getReportType() == ReportType.APPOINTMENT_SUMMARY) {
                List<AppointmentDto> appointments = fetchAppointments(reportRequest);
                
                // Generate report based on format
                if (reportRequest.getFormat() == ReportFormat.PDF) {
                    pdfReportGenerator.generateAppointmentReport(appointments, reportPath.toString(), reportRequest);
                } else {
                    excelReportGenerator.generateAppointmentReport(appointments, reportPath.toString(), reportRequest);
                }
                
                // Update report status
                report.setStatus("COMPLETED");
                report.setFilePath(reportPath.toString());
                reportRepository.save(report);
                
                // Send notification via Kafka
                sendReportNotification(report);
                
                return mapToResponseDto(report);
            } else {
                throw new UnsupportedOperationException("Report type not supported yet: " + reportRequest.getReportType());
            }
        } catch (Exception e) {
            log.error("Failed to generate report", e);
            report.setStatus("FAILED");
            reportRepository.save(report);
            
            throw new RuntimeException("Failed to generate report: " + e.getMessage(), e);
        }
    }

    @Override
    public ReportResponseDto getReportById(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found with id: " + reportId));
        
        return mapToResponseDto(report);
    }
    
    private List<AppointmentDto> fetchAppointments(ReportRequestDto reportRequest) {
        List<AppointmentDto> appointments = scheduleServiceClient.getAppointments(
                reportRequest.getDoctorId(),
                reportRequest.getPatientId(),
                reportRequest.getBranchId(),
                reportRequest.getStartDate(),
                reportRequest.getEndDate()
        );
        
        // Enhance appointments with patient and doctor names
        return appointments.stream()
                .map(this::enhanceAppointmentWithNames)
                .toList();
    }
    
    private AppointmentDto enhanceAppointmentWithNames(AppointmentDto appointment) {
        try {
            // Fetch patient name
            if (appointment.getPatientId() != null) {
                patientServiceClient.getPatientById(appointment.getPatientId())
                    .ifPresent(patient -> {
                        if (patient.getUserId() != null) {
                            userServiceClient.getUserById(patient.getUserId())
                                .ifPresent(user -> appointment.setPatientName(user.getFullName()));
                        }
                    });
            }
            
            // Fetch doctor name
            if (appointment.getDoctorId() != null) {
                doctorServiceClient.getDoctorById(appointment.getDoctorId())
                    .ifPresent(doctor -> {
                        if (doctor.getUserId() != null) {
                            userServiceClient.getUserById(doctor.getUserId())
                                .ifPresent(user -> appointment.setDoctorName(user.getFullName()));
                        }
                    });
            }
            
        } catch (Exception e) {
            log.warn("Failed to enhance appointment {} with names: {}", appointment.getId(), e.getMessage());
        }
        
        return appointment;
    }
    
    private String generateReportTitle(ReportRequestDto reportRequest) {
        StringBuilder title = new StringBuilder();
        
        switch (reportRequest.getReportType()) {
            case APPOINTMENT_SUMMARY:
                title.append("Appointment Summary Report");
                break;
            case DOCTOR_SCHEDULE:
                title.append("Doctor Schedule Report");
                break;
            case PATIENT_HISTORY:
                title.append("Patient History Report");
                break;
        }
        
        if (reportRequest.getStartDate() != null && reportRequest.getEndDate() != null) {
            title.append(" (").append(reportRequest.getStartDate()).append(" to ").append(reportRequest.getEndDate()).append(")");
        }
        
        return title.toString();
    }
    
    private String generateFilename(Report report) {
        String extension = report.getFormat() == ReportFormat.PDF ? "pdf" : "xlsx";
        return String.format("%s_%s_%s.%s", 
                report.getReportType().toString().toLowerCase(),
                report.getId(),
                UUID.randomUUID().toString().substring(0, 8),
                extension);
    }
    
    private void sendReportNotification(Report report) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "REPORT_GENERATED");
        event.put("reportId", report.getId());
        event.put("reportType", report.getReportType().toString());
        event.put("userId", report.getUserId());
        event.put("timestamp", LocalDateTime.now());
        
        kafkaTemplate.send(reportEventsTopic, event);
        log.info("Sent report notification for report ID: {}", report.getId());
    }
    
    private ReportResponseDto mapToResponseDto(Report report) {
        String downloadUrl = null;
        if (report.getFilePath() != null && "COMPLETED".equals(report.getStatus())) {
            String filename = Paths.get(report.getFilePath()).getFileName().toString();
            downloadUrl = reportDownloadBaseUrl + "/" + filename;
        }
        
        return ReportResponseDto.builder()
                .id(report.getId())
                .title(report.getTitle())
                .reportType(report.getReportType())
                .format(report.getFormat())
                .downloadUrl(downloadUrl)
                .generatedAt(report.getGeneratedAt())
                .status(report.getStatus())
                .build();
    }
}