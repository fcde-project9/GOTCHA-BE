package com.gotcha.domain.report.service;

import com.gotcha.domain.report.dto.AdminReportListResponse;
import com.gotcha.domain.report.dto.ReportDetailResponse;
import com.gotcha.domain.report.dto.UpdateReportStatusRequest;
import com.gotcha.domain.report.entity.Report;
import com.gotcha.domain.report.entity.ReportStatus;
import com.gotcha.domain.report.entity.ReportTargetType;
import com.gotcha.domain.report.exception.ReportException;
import com.gotcha.domain.report.repository.ReportRepository;
import com.gotcha.domain.review.repository.ReviewRepository;
import com.gotcha.domain.user.dto.UpdateUserStatusRequest;
import com.gotcha.domain.user.entity.UserStatus;
import com.gotcha.domain.user.service.AdminUserService;
import java.util.List;
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
    private final ReviewRepository reviewRepository;
    private final AdminUserService adminUserService;

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
     * - ACCEPTED: 신고 승인 (제재 정보가 있으면 대상 사용자에게 제재 적용 + 동일 대상 PENDING 신고 자동 승인)
     * - REJECTED: 신고 반려
     * - PENDING 상태의 신고만 변경 가능
     */
    @Transactional
    public ReportDetailResponse updateReportStatus(Long reportId, UpdateReportStatusRequest request) {
        validateStatusTransition(request.status());

        Report report = reportRepository.findByIdWithReporter(reportId)
                .orElseThrow(() -> ReportException.notFound(reportId));

        if (!report.isPending()) {
            throw ReportException.alreadyProcessed();
        }

        report.updateStatus(request.status());

        // 승인 시 제재 정보가 있으면 대상 사용자에게 제재 적용
        if (request.status() == ReportStatus.ACCEPTED && request.userStatus() != null) {
            Long targetUserId = resolveTargetUserId(report);
            if (targetUserId != null) {
                adminUserService.updateUserStatus(targetUserId,
                        new UpdateUserStatusRequest(request.userStatus(), request.suspensionHours()));
                autoAcceptPendingReports(report, reportId);
                log.info("Sanction applied via report - reportId: {}, targetUserId: {}, sanction: {}",
                        reportId, targetUserId, request.userStatus());
            }
        }

        log.info("Report status updated - reportId: {}, newStatus: {}", reportId, request.status());

        return ReportDetailResponse.from(report);
    }

    /**
     * 신고 대상의 사용자 ID를 추출
     * - USER: targetId가 곧 userId
     * - REVIEW: 리뷰 작성자의 userId
     * - SHOP: 사용자 제재 대상 아님 (null 반환)
     */
    private Long resolveTargetUserId(Report report) {
        return switch (report.getTargetType()) {
            case USER -> report.getTargetId();
            case REVIEW -> reviewRepository.findById(report.getTargetId())
                    .map(review -> review.getUser().getId())
                    .orElse(null);
            case SHOP -> null;
        };
    }

    /**
     * 동일 대상에 대한 나머지 PENDING 신고를 자동 ACCEPTED 처리
     */
    private void autoAcceptPendingReports(Report currentReport, Long excludeReportId) {
        List<Report> pendingReports = reportRepository.findAllByTargetTypeAndTargetIdAndStatus(
                currentReport.getTargetType(), currentReport.getTargetId(), ReportStatus.PENDING);

        int count = 0;
        for (Report pending : pendingReports) {
            if (!pending.getId().equals(excludeReportId)) {
                pending.updateStatus(ReportStatus.ACCEPTED);
                count++;
            }
        }

        if (count > 0) {
            log.info("Auto-accepted {} pending reports - targetType: {}, targetId: {}",
                    count, currentReport.getTargetType(), currentReport.getTargetId());
        }
    }

    private void validateStatusTransition(ReportStatus status) {
        if (status != ReportStatus.ACCEPTED && status != ReportStatus.REJECTED) {
            throw ReportException.invalidStatusTransition();
        }
    }
}
