package com.tinysteps.reportservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponseDto {
    private Long id;
    private String title;
    private ReportType reportType;
    private ReportFormat format;
    private String downloadUrl;
    private LocalDateTime generatedAt;
    private String status;
}