package com.gotcha.domain.report.repository;

import com.gotcha.domain.report.entity.Report;
import com.gotcha.domain.report.entity.ReportStatus;
import com.gotcha.domain.report.entity.ReportTargetType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType, Long targetId);

    @Query("SELECT r FROM Report r JOIN FETCH r.reporter WHERE r.reporter.id = :reporterId AND r.status != 'CANCELLED' ORDER BY r.createdAt DESC")
    List<Report> findAllByReporterIdWithReporter(@Param("reporterId") Long reporterId);

    @Query("SELECT r FROM Report r JOIN FETCH r.reporter WHERE r.id = :id")
    Optional<Report> findByIdWithReporter(@Param("id") Long id);

    @Query("SELECT r FROM Report r JOIN FETCH r.reporter " +
           "WHERE (:targetType IS NULL OR r.targetType = :targetType) " +
           "AND (:status IS NULL OR r.status = :status)")
    Page<Report> findAllWithFilters(
            @Param("targetType") ReportTargetType targetType,
            @Param("status") ReportStatus status,
            Pageable pageable);

    @Query("SELECT COUNT(r) FROM Report r " +
           "WHERE (:targetType IS NULL OR r.targetType = :targetType) " +
           "AND (:status IS NULL OR r.status = :status)")
    long countWithFilters(
            @Param("targetType") ReportTargetType targetType,
            @Param("status") ReportStatus status);
}
