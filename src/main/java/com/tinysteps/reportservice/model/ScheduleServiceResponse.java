package com.tinysteps.reportservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime; // Changed back from ZonedDateTime
import java.util.List;

/**
 * Response wrapper for the schedule service API
 * Matches the actual response structure from the schedule service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleServiceResponse {
    private int statusCode;
    private String status;
    private String message;
    private String timestamp; // Changed to String to avoid LocalDateTime parsing issues
    private DataWrapper data;
    private Object errors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataWrapper {
        private List<ScheduleAppointmentDto> content;
        private PageableInfo pageable;
        private boolean last;
        private int totalElements;
        private int totalPages;
        private boolean first;
        private int size;
        private int number;
        private SortInfo sort;
        private int numberOfElements;
        private boolean empty;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageableInfo {
        private int pageNumber;
        private int pageSize;
        private SortInfo sort;
        private int offset;
        private boolean paged;
        private boolean unpaged;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SortInfo {
        private boolean empty;
        private boolean unsorted;
        private boolean sorted;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleAppointmentDto {
        private String id;
        private String appointmentNumber;
        private String doctorId;
        private String patientId;
        private String sessionTypeId;
        private String sessionOfferingId;
        private String practiceId;
        private String appointmentDate;
        private String startTime;
        private String endTime;
        private String status;
        private String consultationType;
        private String notes;
        private String cancellationReason;
        private String checkedInAt; // Changed to String to avoid LocalDateTime parsing issues
        private Integer sessionDurationMinutes;
    }
}
