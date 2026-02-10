package com.gotcha.domain.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.report.dto.CreateReportRequest;
import com.gotcha.domain.report.dto.ReportResponse;
import com.gotcha.domain.report.entity.Report;
import com.gotcha.domain.report.entity.ReportReason;
import com.gotcha.domain.report.entity.ReportStatus;
import com.gotcha.domain.report.entity.ReportTargetType;
import com.gotcha.domain.report.exception.ReportException;
import com.gotcha.domain.report.repository.ReportRepository;
import com.gotcha.domain.review.entity.Review;
import com.gotcha.domain.review.repository.ReviewRepository;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.repository.ShopRepository;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private ReportService reportService;

    private User reporter;
    private User targetUser;
    private Review review;
    private Shop shop;

    @BeforeEach
    void setUp() {
        reporter = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("reporter123")
                .nickname("신고자")
                .build();
        ReflectionTestUtils.setField(reporter, "id", 1L);

        targetUser = User.builder()
                .socialType(SocialType.GOOGLE)
                .socialId("target123")
                .nickname("피신고자")
                .build();
        ReflectionTestUtils.setField(targetUser, "id", 2L);

        shop = Shop.builder()
                .name("가챠샵")
                .addressName("서울시 강남구")
                .latitude(37.4979)
                .longitude(127.0276)
                .createdBy(reporter)
                .build();
        ReflectionTestUtils.setField(shop, "id", 1L);

        review = Review.builder()
                .shop(shop)
                .user(targetUser)
                .content("테스트 리뷰")
                .build();
        ReflectionTestUtils.setField(review, "id", 1L);
    }

    @Nested
    @DisplayName("신고 생성")
    class CreateReport {

        @Test
        @DisplayName("리뷰 신고 성공")
        void createReviewReport_Success() {
            // given
            CreateReportRequest request = new CreateReportRequest(
                    ReportTargetType.REVIEW,
                    review.getId(),
                    ReportReason.REVIEW_ABUSE,
                    null
            );

            when(securityUtil.getCurrentUser()).thenReturn(reporter);
            when(reviewRepository.findById(review.getId())).thenReturn(Optional.of(review));
            when(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                    reporter.getId(), ReportTargetType.REVIEW, review.getId())).thenReturn(false);
            when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
                Report saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 1L);
                return saved;
            });

            // when
            ReportResponse response = reportService.createReport(request);

            // then
            assertThat(response.targetType()).isEqualTo(ReportTargetType.REVIEW);
            assertThat(response.targetId()).isEqualTo(review.getId());
            assertThat(response.reason()).isEqualTo(ReportReason.REVIEW_ABUSE);
            assertThat(response.status()).isEqualTo(ReportStatus.PENDING);

            ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
            verify(reportRepository).save(captor.capture());
            Report savedReport = captor.getValue();
            assertThat(savedReport.getReporter().getId()).isEqualTo(reporter.getId());
        }

        @Test
        @DisplayName("유저 신고 성공")
        void createUserReport_Success() {
            // given
            CreateReportRequest request = new CreateReportRequest(
                    ReportTargetType.USER,
                    targetUser.getId(),
                    ReportReason.USER_PRIVACY,
                    "스팸 메시지를 보냅니다"
            );

            when(securityUtil.getCurrentUser()).thenReturn(reporter);
            when(userRepository.existsById(targetUser.getId())).thenReturn(true);
            when(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                    reporter.getId(), ReportTargetType.USER, targetUser.getId())).thenReturn(false);
            when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
                Report saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 1L);
                return saved;
            });

            // when
            ReportResponse response = reportService.createReport(request);

            // then
            assertThat(response.targetType()).isEqualTo(ReportTargetType.USER);
            assertThat(response.detail()).isEqualTo("스팸 메시지를 보냅니다");
        }

        @Test
        @DisplayName("기타 사유 선택 시 상세 내용 필수")
        void createReport_OtherReasonWithoutDetail_Fail() {
            // given
            CreateReportRequest request = new CreateReportRequest(
                    ReportTargetType.REVIEW,
                    review.getId(),
                    ReportReason.REVIEW_OTHER,
                    null
            );

            when(securityUtil.getCurrentUser()).thenReturn(reporter);

            // when & then
            assertThatThrownBy(() -> reportService.createReport(request))
                    .isInstanceOf(ReportException.class)
                    .hasMessageContaining("기타 사유 선택 시 상세 내용을 입력해주세요");
        }

        @Test
        @DisplayName("기타 사유 선택 시 빈 상세 내용도 실패")
        void createReport_OtherReasonWithBlankDetail_Fail() {
            // given
            CreateReportRequest request = new CreateReportRequest(
                    ReportTargetType.REVIEW,
                    review.getId(),
                    ReportReason.REVIEW_OTHER,
                    "   "
            );

            when(securityUtil.getCurrentUser()).thenReturn(reporter);

            // when & then
            assertThatThrownBy(() -> reportService.createReport(request))
                    .isInstanceOf(ReportException.class)
                    .hasMessageContaining("기타 사유 선택 시 상세 내용을 입력해주세요");
        }

        @Test
        @DisplayName("본인 리뷰 신고 불가")
        void createReport_SelfReviewReport_Fail() {
            // given
            Review myReview = Review.builder()
                    .shop(shop)
                    .user(reporter)
                    .content("내 리뷰")
                    .build();
            ReflectionTestUtils.setField(myReview, "id", 2L);

            CreateReportRequest request = new CreateReportRequest(
                    ReportTargetType.REVIEW,
                    myReview.getId(),
                    ReportReason.REVIEW_ABUSE,
                    null
            );

            when(securityUtil.getCurrentUser()).thenReturn(reporter);
            when(reviewRepository.findById(myReview.getId())).thenReturn(Optional.of(myReview));

            // when & then
            assertThatThrownBy(() -> reportService.createReport(request))
                    .isInstanceOf(ReportException.class)
                    .hasMessageContaining("본인을 신고할 수 없습니다");
        }

        @Test
        @DisplayName("본인 유저 신고 불가")
        void createReport_SelfUserReport_Fail() {
            // given
            CreateReportRequest request = new CreateReportRequest(
                    ReportTargetType.USER,
                    reporter.getId(),
                    ReportReason.USER_INAPPROPRIATE_NICKNAME,
                    null
            );

            when(securityUtil.getCurrentUser()).thenReturn(reporter);

            // when & then
            assertThatThrownBy(() -> reportService.createReport(request))
                    .isInstanceOf(ReportException.class)
                    .hasMessageContaining("본인을 신고할 수 없습니다");
        }

        @Test
        @DisplayName("이미 신고한 대상 중복 신고 불가")
        void createReport_AlreadyReported_Fail() {
            // given
            CreateReportRequest request = new CreateReportRequest(
                    ReportTargetType.REVIEW,
                    review.getId(),
                    ReportReason.REVIEW_ABUSE,
                    null
            );

            when(securityUtil.getCurrentUser()).thenReturn(reporter);
            when(reviewRepository.findById(review.getId())).thenReturn(Optional.of(review));
            when(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                    reporter.getId(), ReportTargetType.REVIEW, review.getId())).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> reportService.createReport(request))
                    .isInstanceOf(ReportException.class)
                    .hasMessageContaining("이미 신고한 대상입니다");
        }

        @Test
        @DisplayName("존재하지 않는 리뷰 신고 불가")
        void createReport_ReviewNotFound_Fail() {
            // given
            CreateReportRequest request = new CreateReportRequest(
                    ReportTargetType.REVIEW,
                    999L,
                    ReportReason.REVIEW_ABUSE,
                    null
            );

            when(securityUtil.getCurrentUser()).thenReturn(reporter);
            when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reportService.createReport(request))
                    .isInstanceOf(ReportException.class)
                    .hasMessageContaining("신고 대상을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("존재하지 않는 유저 신고 불가")
        void createReport_UserNotFound_Fail() {
            // given
            CreateReportRequest request = new CreateReportRequest(
                    ReportTargetType.USER,
                    999L,
                    ReportReason.USER_INAPPROPRIATE_NICKNAME,
                    null
            );

            when(securityUtil.getCurrentUser()).thenReturn(reporter);
            when(userRepository.existsById(999L)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> reportService.createReport(request))
                    .isInstanceOf(ReportException.class)
                    .hasMessageContaining("신고 대상을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("본인 신고 목록 조회")
    class GetMyReports {

        @Test
        @DisplayName("본인 신고 목록 조회 성공")
        void getMyReports_Success() {
            // given
            Report report1 = Report.builder()
                    .reporter(reporter)
                    .targetType(ReportTargetType.REVIEW)
                    .targetId(review.getId())
                    .reason(ReportReason.REVIEW_ABUSE)
                    .build();
            ReflectionTestUtils.setField(report1, "id", 1L);

            Report report2 = Report.builder()
                    .reporter(reporter)
                    .targetType(ReportTargetType.USER)
                    .targetId(targetUser.getId())
                    .reason(ReportReason.USER_PRIVACY)
                    .build();
            ReflectionTestUtils.setField(report2, "id", 2L);

            when(securityUtil.getCurrentUserId()).thenReturn(reporter.getId());
            when(reportRepository.findAllByReporterIdWithReporter(reporter.getId()))
                    .thenReturn(List.of(report1, report2));

            // when
            List<ReportResponse> reports = reportService.getMyReports();

            // then
            assertThat(reports).hasSize(2);
        }

        @Test
        @DisplayName("신고 목록이 없으면 빈 리스트 반환")
        void getMyReports_Empty() {
            // given
            when(securityUtil.getCurrentUserId()).thenReturn(reporter.getId());
            when(reportRepository.findAllByReporterIdWithReporter(reporter.getId()))
                    .thenReturn(List.of());

            // when
            List<ReportResponse> reports = reportService.getMyReports();

            // then
            assertThat(reports).isEmpty();
        }
    }

    @Nested
    @DisplayName("신고 취소")
    class CancelReport {

        @Test
        @DisplayName("신고 취소 성공")
        void cancelReport_Success() {
            // given
            Report report = Report.builder()
                    .reporter(reporter)
                    .targetType(ReportTargetType.REVIEW)
                    .targetId(review.getId())
                    .reason(ReportReason.REVIEW_ABUSE)
                    .build();
            ReflectionTestUtils.setField(report, "id", 1L);

            when(securityUtil.getCurrentUserId()).thenReturn(reporter.getId());
            when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

            // when
            reportService.cancelReport(1L);

            // then
            assertThat(report.getStatus()).isEqualTo(ReportStatus.CANCELLED);
        }

        @Test
        @DisplayName("존재하지 않는 신고 취소 불가")
        void cancelReport_NotFound_Fail() {
            // given
            when(securityUtil.getCurrentUserId()).thenReturn(reporter.getId());
            when(reportRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reportService.cancelReport(999L))
                    .isInstanceOf(ReportException.class)
                    .hasMessageContaining("신고를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("타인의 신고 취소 불가")
        void cancelReport_Unauthorized_Fail() {
            // given
            Report report = Report.builder()
                    .reporter(targetUser)
                    .targetType(ReportTargetType.REVIEW)
                    .targetId(review.getId())
                    .reason(ReportReason.REVIEW_ABUSE)
                    .build();
            ReflectionTestUtils.setField(report, "id", 1L);

            when(securityUtil.getCurrentUserId()).thenReturn(reporter.getId());
            when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

            // when & then
            assertThatThrownBy(() -> reportService.cancelReport(1L))
                    .isInstanceOf(ReportException.class)
                    .hasMessageContaining("본인의 신고만 취소할 수 있습니다");
        }

        @Test
        @DisplayName("이미 처리된 신고 취소 불가")
        void cancelReport_AlreadyProcessed_Fail() {
            // given
            Report report = Report.builder()
                    .reporter(reporter)
                    .targetType(ReportTargetType.REVIEW)
                    .targetId(review.getId())
                    .reason(ReportReason.REVIEW_ABUSE)
                    .build();
            report.updateStatus(ReportStatus.ACCEPTED);
            ReflectionTestUtils.setField(report, "id", 1L);

            when(securityUtil.getCurrentUserId()).thenReturn(reporter.getId());
            when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

            // when & then
            assertThatThrownBy(() -> reportService.cancelReport(1L))
                    .isInstanceOf(ReportException.class)
                    .hasMessageContaining("이미 처리된 신고는 취소할 수 없습니다");
        }
    }
}
