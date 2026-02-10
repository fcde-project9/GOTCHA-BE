package com.gotcha.domain.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.gotcha.domain.report.dto.AdminReportListResponse;
import com.gotcha.domain.report.dto.ReportDetailResponse;
import com.gotcha.domain.report.dto.UpdateReportStatusRequest;
import com.gotcha.domain.report.entity.Report;
import com.gotcha.domain.report.entity.ReportReason;
import com.gotcha.domain.report.entity.ReportStatus;
import com.gotcha.domain.report.entity.ReportTargetType;
import com.gotcha.domain.report.exception.ReportException;
import com.gotcha.domain.report.repository.ReportRepository;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private AdminReportService adminReportService;

    private User reporter;
    private Report report;

    @BeforeEach
    void setUp() {
        reporter = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("reporter123")
                .nickname("신고자")
                .build();
        ReflectionTestUtils.setField(reporter, "id", 1L);

        report = Report.builder()
                .reporter(reporter)
                .targetType(ReportTargetType.REVIEW)
                .targetId(1L)
                .reason(ReportReason.ABUSE)
                .detail("욕설 포함")
                .build();
        ReflectionTestUtils.setField(report, "id", 1L);
    }

    @Nested
    @DisplayName("신고 목록 조회")
    class GetReports {

        @Test
        @DisplayName("전체 신고 목록 조회 성공")
        void getReports_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Report> reportPage = new PageImpl<>(List.of(report), pageable, 1);

            when(reportRepository.findAllWithFilters(null, null, pageable)).thenReturn(reportPage);

            // when
            AdminReportListResponse response = adminReportService.getReports(null, null, pageable);

            // then
            assertThat(response.reports()).hasSize(1);
            assertThat(response.totalElements()).isEqualTo(1);
            assertThat(response.page()).isEqualTo(0);
        }

        @Test
        @DisplayName("타겟 타입 필터링 조회")
        void getReports_FilterByTargetType() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            Page<Report> reportPage = new PageImpl<>(List.of(report), pageable, 1);

            when(reportRepository.findAllWithFilters(ReportTargetType.REVIEW, null, pageable))
                    .thenReturn(reportPage);

            // when
            AdminReportListResponse response = adminReportService.getReports(
                    ReportTargetType.REVIEW, null, pageable);

            // then
            assertThat(response.reports()).hasSize(1);
            assertThat(response.reports().get(0).targetType()).isEqualTo(ReportTargetType.REVIEW);
        }

        @Test
        @DisplayName("상태 필터링 조회")
        void getReports_FilterByStatus() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            Page<Report> reportPage = new PageImpl<>(List.of(report), pageable, 1);

            when(reportRepository.findAllWithFilters(null, ReportStatus.PENDING, pageable))
                    .thenReturn(reportPage);

            // when
            AdminReportListResponse response = adminReportService.getReports(
                    null, ReportStatus.PENDING, pageable);

            // then
            assertThat(response.reports()).hasSize(1);
        }

        @Test
        @DisplayName("빈 결과 반환")
        void getReports_Empty() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            Page<Report> emptyPage = Page.empty(pageable);

            when(reportRepository.findAllWithFilters(null, null, pageable)).thenReturn(emptyPage);

            // when
            AdminReportListResponse response = adminReportService.getReports(null, null, pageable);

            // then
            assertThat(response.reports()).isEmpty();
            assertThat(response.totalElements()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("신고 상세 조회")
    class GetReport {

        @Test
        @DisplayName("신고 상세 조회 성공")
        void getReport_Success() {
            // given
            when(reportRepository.findByIdWithReporter(1L)).thenReturn(Optional.of(report));

            // when
            ReportDetailResponse response = adminReportService.getReport(1L);

            // then
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.reporterId()).isEqualTo(reporter.getId());
            assertThat(response.reporterNickname()).isEqualTo("신고자");
            assertThat(response.targetType()).isEqualTo(ReportTargetType.REVIEW);
            assertThat(response.reason()).isEqualTo(ReportReason.ABUSE);
            assertThat(response.reasonDescription()).isEqualTo("욕설/비방");
            assertThat(response.detail()).isEqualTo("욕설 포함");
            assertThat(response.status()).isEqualTo(ReportStatus.PENDING);
            assertThat(response.statusDescription()).isEqualTo("처리 대기");
        }

        @Test
        @DisplayName("존재하지 않는 신고 조회 시 실패")
        void getReport_NotFound_Fail() {
            // given
            when(reportRepository.findByIdWithReporter(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminReportService.getReport(999L))
                    .isInstanceOf(ReportException.class)
                    .hasMessageContaining("신고를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("신고 상태 변경")
    class UpdateReportStatus {

        @Test
        @DisplayName("신고 승인 성공")
        void updateStatus_Accept_Success() {
            // given
            UpdateReportStatusRequest request = new UpdateReportStatusRequest(ReportStatus.ACCEPTED);
            when(reportRepository.findByIdWithReporter(1L)).thenReturn(Optional.of(report));

            // when
            ReportDetailResponse response = adminReportService.updateReportStatus(1L, request);

            // then
            assertThat(response.status()).isEqualTo(ReportStatus.ACCEPTED);
            assertThat(response.statusDescription()).isEqualTo("승인");
        }

        @Test
        @DisplayName("신고 반려 성공")
        void updateStatus_Reject_Success() {
            // given
            UpdateReportStatusRequest request = new UpdateReportStatusRequest(ReportStatus.REJECTED);
            when(reportRepository.findByIdWithReporter(1L)).thenReturn(Optional.of(report));

            // when
            ReportDetailResponse response = adminReportService.updateReportStatus(1L, request);

            // then
            assertThat(response.status()).isEqualTo(ReportStatus.REJECTED);
            assertThat(response.statusDescription()).isEqualTo("반려");
        }

        @Test
        @DisplayName("존재하지 않는 신고 상태 변경 시 실패")
        void updateStatus_NotFound_Fail() {
            // given
            UpdateReportStatusRequest request = new UpdateReportStatusRequest(ReportStatus.ACCEPTED);
            when(reportRepository.findByIdWithReporter(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminReportService.updateReportStatus(999L, request))
                    .isInstanceOf(ReportException.class)
                    .hasMessageContaining("신고를 찾을 수 없습니다");
        }
    }
}
