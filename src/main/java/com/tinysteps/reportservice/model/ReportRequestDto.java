package com.tinysteps.reportservice.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequestDto {
    @NotNull
    private ReportType reportType;

    @NotNull
    private ReportFormat format;

    private String userId;
    private String doctorId;
    private String patientId;
    private String branchId;
    private LocalDate startDate;
    private LocalDate endDate;
}
