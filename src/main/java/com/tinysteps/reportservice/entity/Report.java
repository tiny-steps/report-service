package com.tinysteps.reportservice.entity;

import com.tinysteps.reportservice.model.ReportFormat;
import com.tinysteps.reportservice.model.ReportType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportType reportType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportFormat format;

    @Column
    private String filePath;

    @Column(nullable = false)
    private String userId;

    @Column
    private String branchId; // Can store "all" for all branches or specific branchId

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    @Column
    private String parameters;

    @Column
    private Long fileSize;

    @Column
    private String status;
}
