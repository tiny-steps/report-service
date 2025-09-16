package com.tinysteps.reportservice.service.impl;

import com.tinysteps.reportservice.client.DoctorServiceClient;
import com.tinysteps.reportservice.client.PatientServiceClient;
import com.tinysteps.reportservice.client.ScheduleServiceClient;
import com.tinysteps.reportservice.client.SessionServiceClient;
import com.tinysteps.reportservice.client.UserServiceClient;
import com.tinysteps.reportservice.entity.Report;
import com.tinysteps.reportservice.model.*;
import com.tinysteps.reportservice.repository.ReportRepository;
import com.tinysteps.reportservice.service.ReportService;
import com.tinysteps.reportservice.service.generator.PdfReportGenerator;
import com.tinysteps.reportservice.service.generator.ExcelReportGenerator;
import com.tinysteps.reportservice.specification.ReportSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    private final SessionServiceClient sessionServiceClient;
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
                .branchId(reportRequest.getBranchId() != null ? reportRequest.getBranchId() : "all")
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
            // Fetch patient name with timeout handling
            if (appointment.getPatientId() != null) {
                log.debug("Fetching patient details for patientId: {}", appointment.getPatientId());
                try {
                    patientServiceClient.getPatientById(appointment.getPatientId())
                        .ifPresentOrElse(patient -> {
                            log.debug("Found patient: {}, userId: {}", patient.getId(), patient.getUserId());
                            if (patient.getUserId() != null) {
                                try {
                                    userServiceClient.getUserById(patient.getUserId())
                                        .ifPresentOrElse(user -> {
                                            String patientName = user.getFullName();
                                            log.debug("Setting patient name: {}", patientName);
                                            appointment.setPatientName(patientName);
                                        }, () -> {
                                            log.warn("User not found for userId: {}", patient.getUserId());
                                            appointment.setPatientName("Patient ID: " + appointment.getPatientId());
                                        });
                                } catch (Exception e) {
                                    log.warn("Timeout or error fetching user for userId: {}. Using fallback.", patient.getUserId(), e);
                                    appointment.setPatientName("Patient ID: " + appointment.getPatientId());
                                }
                            } else {
                                log.warn("Patient {} has no userId", patient.getId());
                                appointment.setPatientName("Patient ID: " + appointment.getPatientId());
                            }
                        }, () -> {
                            log.warn("Patient not found for patientId: {}", appointment.getPatientId());
                            appointment.setPatientName("Patient ID: " + appointment.getPatientId());
                        });
                } catch (Exception e) {
                    log.warn("Timeout or error fetching patient for patientId: {}. Using fallback.", appointment.getPatientId(), e);
                    appointment.setPatientName("Patient ID: " + appointment.getPatientId());
                }
            }

            // Fetch doctor name with timeout handling
            if (appointment.getDoctorId() != null) {
                log.debug("Fetching doctor details for doctorId: {}", appointment.getDoctorId());
                try {
                    doctorServiceClient.getDoctorById(appointment.getDoctorId())
                        .ifPresentOrElse(doctor -> {
                            String doctorName = doctor.getFullName();
                            log.debug("Setting doctor name: {}", doctorName);
                            appointment.setDoctorName(doctorName);
                        }, () -> {
                            log.warn("Doctor not found for doctorId: {}", appointment.getDoctorId());
                            appointment.setDoctorName("Doctor ID: " + appointment.getDoctorId());
                        });
                } catch (Exception e) {
                    log.warn("Timeout or error fetching doctor for doctorId: {}. Using fallback.", appointment.getDoctorId(), e);
                    appointment.setDoctorName("Doctor ID: " + appointment.getDoctorId());
                }
            }

            // Fetch session type name with timeout handling
            if (appointment.getSessionTypeId() != null) {
                log.debug("Fetching session type details for sessionTypeId: {}", appointment.getSessionTypeId());
                try {
                    sessionServiceClient.getSessionTypeById(appointment.getSessionTypeId())
                        .ifPresentOrElse(sessionType -> {
                            String sessionTypeName = sessionType.getName();
                            log.debug("Setting session type name: {}", sessionTypeName);
                            appointment.setSessionTypeName(sessionTypeName);
                        }, () -> {
                            log.warn("Session type not found for sessionTypeId: {}", appointment.getSessionTypeId());
                            appointment.setSessionTypeName("Session Type ID: " + appointment.getSessionTypeId());
                        });
                } catch (Exception e) {
                    log.warn("Timeout or error fetching session type for sessionTypeId: {}. Using fallback.", appointment.getSessionTypeId(), e);
                    appointment.setSessionTypeName("Session Type ID: " + appointment.getSessionTypeId());
                }
            }

            // Fetch session offering price with timeout handling
            if (appointment.getSessionId() != null) {
                log.debug("Fetching session offering details for sessionId: {}", appointment.getSessionId());
                try {
                    sessionServiceClient.getSessionOfferingById(appointment.getSessionId())
                        .ifPresentOrElse(sessionOffering -> {
                            String price = sessionOffering.getPrice() != null ?
                                "$" + sessionOffering.getPrice().toString() : "N/A";
                            log.debug("Setting session offering price: {}", price);
                            appointment.setSessionOfferingPrice(price);
                        }, () -> {
                            log.warn("Session offering not found for sessionId: {}", appointment.getSessionId());
                            appointment.setSessionOfferingPrice("N/A");
                        });
                } catch (Exception e) {
                    log.warn("Timeout or error fetching session offering for sessionId: {}. Using fallback.", appointment.getSessionId(), e);
                    appointment.setSessionOfferingPrice("N/A");
                }
            }

            // Calculate and format duration
            if (appointment.getStartTime() != null && appointment.getEndTime() != null) {
                long minutes = ChronoUnit.MINUTES.between(appointment.getStartTime(), appointment.getEndTime());
                String durationFormatted = minutes + " minutes";
                log.debug("Calculated duration: {}", durationFormatted);
                appointment.setDurationFormatted(durationFormatted);
            } else if (appointment.getSessionDurationMinutes() != null) {
                // Fallback to session duration if start/end times are not available
                String durationFormatted = appointment.getSessionDurationMinutes() + " minutes";
                log.debug("Using session duration: {}", durationFormatted);
                appointment.setDurationFormatted(durationFormatted);
            }

        } catch (Exception e) {
            log.error("Failed to enhance appointment {} with names: {}", appointment.getId(), e.getMessage(), e);
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

    @Override
    public List<ReportResponseDto> searchReports(LocalDate startDate, LocalDate endDate, String branchId, String userId, ReportType reportType) {
        log.info("Searching reports from {} to {} for branchId: {}, userId: {}, reportType: {}",
                startDate, endDate, branchId, userId, reportType);

        // Build specification dynamically based on provided parameters
        Specification<Report> spec = Specification.where(null);

        // Add date range filter if provided
        if (startDate != null || endDate != null) {
            spec = spec.and(ReportSpecification.byGeneratedAtBetween(startDate, endDate));
        }

        // Add branch filter if provided
        if (branchId != null && !branchId.trim().isEmpty()) {
            spec = spec.and(ReportSpecification.byBranchId(branchId));
        }

        // Add user filter if provided
        if (userId != null && !userId.trim().isEmpty()) {
            spec = spec.and(ReportSpecification.byUserId(userId));
        }

        // Add report type filter if provided
        if (reportType != null) {
            spec = spec.and(ReportSpecification.byReportType(reportType));
        }

        // Execute query with specification
        List<Report> reports = reportRepository.findAll(spec);

        return reports.stream()
                .map(this::mapToResponseDto)
                .toList();
    }
}
