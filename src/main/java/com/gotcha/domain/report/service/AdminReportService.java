package com.gotcha.domain.report.service;

import com.gotcha.domain.report.dto.AdminReportListResponse;
import com.gotcha.domain.report.dto.ReportDetailResponse;
import com.gotcha.domain.report.dto.UpdateReportStatusRequest;
import com.gotcha.domain.report.entity.Report;
import com.gotcha.domain.report.entity.ReportStatus;
import com.gotcha.domain.report.entity.ReportTargetType;
import com.gotcha.domain.report.exception.ReportException;
import com.gotcha.domain.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReportService {

    private final ReportRepository reportRepository;

    /**
     * 신고 목록 조회 (관리자 전용)
     * - targetType, status로 필터링 가능
     * - 페이징 지원
     */
    public AdminReportListResponse getReports(ReportTargetType targetType, ReportStatus status, Pageable pageable) {
        Page<Report> reportPage = reportRepository.findAllWithFilters(targetType, status, pageable);

        Page<ReportDetailResponse> responsePage = reportPage.map(ReportDetailResponse::from);

        return AdminReportListResponse.from(responsePage);
    }

    /**
     * 신고 상세 정보 조회 (관리자 전용)
     */
    public ReportDetailResponse getReport(Long reportId) {
        Report report = reportRepository.findByIdWithReporter(reportId)
                .orElseThrow(() -> ReportException.notFound(reportId));

        return ReportDetailResponse.from(report);
    }

    /**
     * 신고 상태 변경 (관리자 전용)
     * - ACCEPTED: 신고 승인
     * - REJECTED: 신고 반려
     * (추후 승인 시 자동 제재 로직 추가 가능)
     */
    @Transactional
    public ReportDetailResponse updateReportStatus(Long reportId, UpdateReportStatusRequest request) {
        Report report = reportRepository.findByIdWithReporter(reportId)
                .orElseThrow(() -> ReportException.notFound(reportId));

        report.updateStatus(request.status());

        log.info("Report status updated - reportId: {}, newStatus: {}", reportId, request.status());

        return ReportDetailResponse.from(report);
    }
}
