package com.tinysteps.reportservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDto {
    private String id;
    private String appointmentNumber;
    private String patientId;
    private String patientName;
    private String doctorId;
    private String doctorName;
    private String sessionTypeId;
    private String sessionId; // This will map to sessionOfferingId from schedule service
    private String sessionTypeName; // Name of the session type (e.g., "General Consultation")
    private String sessionOfferingPrice; // Price of the specific session offering
    private String appointmentType;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private String consultationType;
    private String notes;
    private String cancellationReason;
    private LocalDateTime checkedInAt;
    private Integer sessionDurationMinutes;
    private String durationFormatted; // Formatted duration string (e.g., "30 minutes")

    /**
     * Converts from ScheduleServiceResponse.ScheduleAppointmentDto to AppointmentDto
     */
    public static AppointmentDto fromScheduleAppointment(ScheduleServiceResponse.ScheduleAppointmentDto scheduleDto) {
        return AppointmentDto.builder()
                .id(scheduleDto.getId())
                .appointmentNumber(scheduleDto.getAppointmentNumber())
                .patientId(scheduleDto.getPatientId())
                .doctorId(scheduleDto.getDoctorId())
                .sessionTypeId(scheduleDto.getSessionTypeId())
                .sessionId(scheduleDto.getSessionOfferingId())
                .appointmentType(scheduleDto.getConsultationType())
                .appointmentDate(scheduleDto.getAppointmentDate() != null ?
                    LocalDate.parse(scheduleDto.getAppointmentDate()) : null)
                .startTime(scheduleDto.getStartTime() != null ?
                    LocalTime.parse(scheduleDto.getStartTime()) : null)
                .endTime(scheduleDto.getEndTime() != null ?
                    LocalTime.parse(scheduleDto.getEndTime()) : null)
                .status(scheduleDto.getStatus())
                .consultationType(scheduleDto.getConsultationType())
                .notes(scheduleDto.getNotes())
                .cancellationReason(scheduleDto.getCancellationReason())
                .checkedInAt(scheduleDto.getCheckedInAt() != null ?
                        LocalDateTime.parse(scheduleDto.getCheckedInAt()) : null)
                .sessionDurationMinutes(scheduleDto.getSessionDurationMinutes())
                .build();
    }

    /**
     * Gets formatted appointment time for display
     */
    public String getFormattedAppointmentTime() {
        if (appointmentDate != null && startTime != null) {
            return appointmentDate.toString() + " " + startTime.toString();
        }
        return "";
    }
}
