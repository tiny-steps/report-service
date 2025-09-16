package com.tinysteps.reportservice.specification;

import com.tinysteps.reportservice.entity.Report;
import com.tinysteps.reportservice.model.ReportType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReportSpecification {

    public static Specification<Report> byUserId(String userId) {
        return (root, query, cb) ->
                userId == null || userId.trim().isEmpty() ? null : cb.equal(root.get("userId"), userId);
    }

    public static Specification<Report> byBranchId(String branchId) {
        return (root, query, cb) ->
                branchId == null || branchId.trim().isEmpty() ? null : cb.equal(root.get("branchId"), branchId);
    }

    public static Specification<Report> byReportType(ReportType reportType) {
        return (root, query, cb) ->
                reportType == null ? null : cb.equal(root.get("reportType"), reportType);
    }

    public static Specification<Report> byGeneratedAtBetween(LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> {
            if (startDate != null && endDate != null) {
                LocalDateTime startDateTime = startDate.atStartOfDay();
                LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
                return cb.between(root.get("generatedAt"), startDateTime, endDateTime);
            } else if (startDate != null) {
                LocalDateTime startDateTime = startDate.atStartOfDay();
                return cb.greaterThanOrEqualTo(root.get("generatedAt"), startDateTime);
            } else if (endDate != null) {
                LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
                return cb.lessThanOrEqualTo(root.get("generatedAt"), endDateTime);
            } else {
                return null;
            }
        };
    }

    public static Specification<Report> byStatus(String status) {
        return (root, query, cb) ->
                status == null || status.trim().isEmpty() ? null : cb.equal(root.get("status"), status);
    }

    // Composite specifications for common combinations
    public static Specification<Report> byBranchIdAndUserId(String branchId, String userId) {
        return Specification.where(byBranchId(branchId)).and(byUserId(userId));
    }

    public static Specification<Report> byBranchIdAndReportType(String branchId, ReportType reportType) {
        return Specification.where(byBranchId(branchId)).and(byReportType(reportType));
    }

    public static Specification<Report> byUserIdAndReportType(String userId, ReportType reportType) {
        return Specification.where(byUserId(userId)).and(byReportType(reportType));
    }

    public static Specification<Report> byDateRangeAndBranchId(LocalDate startDate, LocalDate endDate, String branchId) {
        return Specification.where(byGeneratedAtBetween(startDate, endDate)).and(byBranchId(branchId));
    }

    public static Specification<Report> byDateRangeAndUserId(LocalDate startDate, LocalDate endDate, String userId) {
        return Specification.where(byGeneratedAtBetween(startDate, endDate)).and(byUserId(userId));
    }

    public static Specification<Report> byDateRangeAndReportType(LocalDate startDate, LocalDate endDate, ReportType reportType) {
        return Specification.where(byGeneratedAtBetween(startDate, endDate)).and(byReportType(reportType));
    }
}