package com.tinysteps.reportservice.repository;

import com.tinysteps.reportservice.entity.Report;
import com.tinysteps.reportservice.model.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {

    // Keep only essential basic queries - complex queries will use Specifications
    List<Report> findByUserId(String userId);

    List<Report> findByReportType(ReportType reportType);

    List<Report> findByUserIdAndReportType(String userId, ReportType reportType);
}
