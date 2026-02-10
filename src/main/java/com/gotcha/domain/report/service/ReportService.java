package com.gotcha.domain.report.service;

import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.report.dto.CreateReportRequest;
import com.gotcha.domain.report.dto.ReportResponse;
import com.gotcha.domain.report.entity.Report;
import com.gotcha.domain.report.entity.ReportReason;
import com.gotcha.domain.report.entity.ReportTargetType;
import com.gotcha.domain.report.exception.ReportException;
import com.gotcha.domain.report.repository.ReportRepository;
import com.gotcha.domain.review.entity.Review;
import com.gotcha.domain.review.repository.ReviewRepository;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;

    /**
     * 리뷰 신고 또는 유저 신고 생성
     * - 본인 신고 불가
     * - 동일 대상 중복 신고 불가
     * - 기타(OTHER) 사유 선택 시 상세 내용 필수
     */
    @Transactional
    public ReportResponse createReport(CreateReportRequest request) {
        User reporter = securityUtil.getCurrentUser();

        validateRequest(request);
        validateTarget(request.targetType(), request.targetId(), reporter.getId());
        validateNotAlreadyReported(reporter.getId(), request.targetType(), request.targetId());

        Report report = Report.builder()
                .reporter(reporter)
                .targetType(request.targetType())
                .targetId(request.targetId())
                .reason(request.reason())
                .detail(request.detail())
                .build();

        reportRepository.save(report);

        log.info("Report created - reportId: {}, reporterId: {}, targetType: {}, targetId: {}",
                report.getId(), reporter.getId(), request.targetType(), request.targetId());

        return ReportResponse.from(report);
    }

    /**
     * 현재 로그인한 사용자의 신고 목록 조회
     */
    public List<ReportResponse> getMyReports() {
        Long userId = securityUtil.getCurrentUserId();

        return reportRepository.findAllByReporterIdWithReporter(userId).stream()
                .map(ReportResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 신고 취소
     * - 본인의 신고만 취소 가능
     * - PENDING 상태에서만 취소 가능 (이미 처리된 신고는 취소 불가)
     */
    @Transactional
    public void cancelReport(Long reportId) {
        Long userId = securityUtil.getCurrentUserId();

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> ReportException.notFound(reportId));

        if (!report.isOwnedBy(userId)) {
            throw ReportException.unauthorizedCancel();
        }

        if (!report.isPending()) {
            throw ReportException.alreadyProcessed();
        }

        report.cancel();

        log.info("Report cancelled - reportId: {}, userId: {}", reportId, userId);
    }

    /**
     * 기타(OTHER) 사유 선택 시, 상세 내용이 입력되었는지 검증
     */
    private void validateRequest(CreateReportRequest request) {
        if (request.reason() == ReportReason.OTHER &&
                (request.detail() == null || request.detail().isBlank())) {
            throw ReportException.detailRequiredForOther();
        }
    }

    /**
     * 신고 대상의 존재 여부, 본인 신고 여부 검증
     */
    private void validateTarget(ReportTargetType targetType, Long targetId, Long reporterId) {
        switch (targetType) {
            case REVIEW -> validateReviewTarget(targetId, reporterId);
            case USER -> validateUserTarget(targetId, reporterId);
        }
    }

    /**
     * 리뷰 신고 대상 검증 (리뷰 존재 여부, 본인 리뷰 여부)
     */
    private void validateReviewTarget(Long reviewId, Long reporterId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ReportException.targetNotFound("REVIEW", reviewId));

        if (review.getUser().getId().equals(reporterId)) {
            throw ReportException.cannotReportSelf();
        }
    }

    /**
     * 유저 신고 대상 검증 (유저 존재 여부, 본인 신고 여부)
     */
    private void validateUserTarget(Long userId, Long reporterId) {
        if (userId.equals(reporterId)) {
            throw ReportException.cannotReportSelf();
        }

        if (!userRepository.existsById(userId)) {
            throw ReportException.targetNotFound("USER", userId);
        }
    }

    /**
     * 동일 대상에 대한 중복 신고 여부 검증
     */
    private void validateNotAlreadyReported(Long reporterId, ReportTargetType targetType, Long targetId) {
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(reporterId, targetType, targetId)) {
            throw ReportException.alreadyReported();
        }
    }
}
