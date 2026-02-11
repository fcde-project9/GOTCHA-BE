package com.gotcha.domain.report.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gotcha.config.TestcontainersConfig;
import com.gotcha.domain.report.entity.Report;
import com.gotcha.domain.report.entity.ReportReason;
import com.gotcha.domain.report.entity.ReportStatus;
import com.gotcha.domain.report.entity.ReportTargetType;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfig.class)
class ReportRepositoryTest {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ShopRepository shopRepository;

    private User reporter;
    private User targetUser;
    private Review review;
    private Shop shop;

    @BeforeEach
    void setUp() {
        reporter = userRepository.save(User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("reporter123")
                .nickname("신고자")
                .build());

        targetUser = userRepository.save(User.builder()
                .socialType(SocialType.GOOGLE)
                .socialId("target123")
                .nickname("피신고자")
                .build());

        shop = shopRepository.save(Shop.builder()
                .name("가챠샵")
                .addressName("서울시 강남구")
                .latitude(37.4979)
                .longitude(127.0276)
                .createdBy(reporter)
                .build());

        review = reviewRepository.save(Review.builder()
                .shop(shop)
                .user(targetUser)
                .content("테스트 리뷰")
                .build());
    }

    @Nested
    @DisplayName("중복 신고 체크")
    class ExistsByReporterIdAndTargetTypeAndTargetId {

        @Test
        @DisplayName("신고가 존재하면 true 반환")
        void existsReport_Success() {
            // given
            reportRepository.save(Report.builder()
                    .reporter(reporter)
                    .targetType(ReportTargetType.REVIEW)
                    .targetId(review.getId())
                    .reason(ReportReason.REVIEW_ABUSE)
                    .build());

            // when
            boolean exists = reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatusNot(
                    reporter.getId(), ReportTargetType.REVIEW, review.getId(), ReportStatus.CANCELLED);

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("신고가 없으면 false 반환")
        void notExistsReport() {
            // when
            boolean exists = reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatusNot(
                    reporter.getId(), ReportTargetType.REVIEW, review.getId(), ReportStatus.CANCELLED);

            // then
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("다른 타입의 신고는 중복이 아님")
        void differentTargetType_NotDuplicate() {
            // given
            reportRepository.save(Report.builder()
                    .reporter(reporter)
                    .targetType(ReportTargetType.REVIEW)
                    .targetId(review.getId())
                    .reason(ReportReason.REVIEW_ABUSE)
                    .build());

            // when
            boolean exists = reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatusNot(
                    reporter.getId(), ReportTargetType.USER, review.getId(), ReportStatus.CANCELLED);

            // then
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("취소된 신고는 중복이 아님")
        void cancelledReport_NotDuplicate() {
            // given
            Report report = reportRepository.save(Report.builder()
                    .reporter(reporter)
                    .targetType(ReportTargetType.REVIEW)
                    .targetId(review.getId())
                    .reason(ReportReason.REVIEW_ABUSE)
                    .build());
            report.cancel();
            reportRepository.save(report);

            // when
            boolean exists = reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatusNot(
                    reporter.getId(), ReportTargetType.REVIEW, review.getId(), ReportStatus.CANCELLED);

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("본인 신고 목록 조회")
    class FindAllByReporterIdWithReporter {

        @Test
        @DisplayName("본인 신고 목록 조회 성공")
        void findMyReports_Success() {
            // given
            reportRepository.save(Report.builder()
                    .reporter(reporter)
                    .targetType(ReportTargetType.REVIEW)
                    .targetId(review.getId())
                    .reason(ReportReason.REVIEW_ABUSE)
                    .build());

            reportRepository.save(Report.builder()
                    .reporter(reporter)
                    .targetType(ReportTargetType.USER)
                    .targetId(targetUser.getId())
                    .reason(ReportReason.USER_PRIVACY)
                    .build());

            // when
            List<Report> reports = reportRepository.findAllByReporterIdWithReporter(reporter.getId(), ReportStatus.CANCELLED);

            // then
            assertThat(reports).hasSize(2);
        }

        @Test
        @DisplayName("신고 없으면 빈 리스트 반환")
        void findMyReports_Empty() {
            // when
            List<Report> reports = reportRepository.findAllByReporterIdWithReporter(reporter.getId(), ReportStatus.CANCELLED);

            // then
            assertThat(reports).isEmpty();
        }
    }

    @Nested
    @DisplayName("신고 상세 조회")
    class FindByIdWithReporter {

        @Test
        @DisplayName("신고 상세 조회 성공")
        void findReport_Success() {
            // given
            Report saved = reportRepository.save(Report.builder()
                    .reporter(reporter)
                    .targetType(ReportTargetType.REVIEW)
                    .targetId(review.getId())
                    .reason(ReportReason.REVIEW_ABUSE)
                    .detail("욕설이 포함되어 있습니다")
                    .build());

            // when
            Optional<Report> found = reportRepository.findByIdWithReporter(saved.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getReporter().getId()).isEqualTo(reporter.getId());
            assertThat(found.get().getDetail()).isEqualTo("욕설이 포함되어 있습니다");
        }

        @Test
        @DisplayName("존재하지 않는 신고 조회 시 빈 Optional 반환")
        void findReport_NotFound() {
            // when
            Optional<Report> found = reportRepository.findByIdWithReporter(999999L);

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("관리자용 신고 목록 필터링 조회")
    class FindAllWithFilters {

        @BeforeEach
        void setupReports() {
            reportRepository.save(Report.builder()
                    .reporter(reporter)
                    .targetType(ReportTargetType.REVIEW)
                    .targetId(review.getId())
                    .reason(ReportReason.REVIEW_ABUSE)
                    .build());

            Report acceptedReport = Report.builder()
                    .reporter(reporter)
                    .targetType(ReportTargetType.USER)
                    .targetId(targetUser.getId())
                    .reason(ReportReason.USER_PRIVACY)
                    .build();
            acceptedReport.updateStatus(ReportStatus.ACCEPTED);
            reportRepository.save(acceptedReport);
        }

        @Test
        @DisplayName("필터 없이 전체 조회")
        void findAll_NoFilter() {
            // when
            Page<Report> page = reportRepository.findAllWithFilters(
                    null, null,
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

            // then
            assertThat(page.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("타겟 타입으로 필터링")
        void findAll_FilterByTargetType() {
            // when
            Page<Report> page = reportRepository.findAllWithFilters(
                    ReportTargetType.REVIEW, null,
                    PageRequest.of(0, 20));

            // then
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getTargetType()).isEqualTo(ReportTargetType.REVIEW);
        }

        @Test
        @DisplayName("상태로 필터링")
        void findAll_FilterByStatus() {
            // when
            Page<Report> page = reportRepository.findAllWithFilters(
                    null, ReportStatus.PENDING,
                    PageRequest.of(0, 20));

            // then
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getStatus()).isEqualTo(ReportStatus.PENDING);
        }

        @Test
        @DisplayName("타겟 타입과 상태 모두로 필터링")
        void findAll_FilterByBoth() {
            // when
            Page<Report> page = reportRepository.findAllWithFilters(
                    ReportTargetType.USER, ReportStatus.ACCEPTED,
                    PageRequest.of(0, 20));

            // then
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getTargetType()).isEqualTo(ReportTargetType.USER);
            assertThat(page.getContent().get(0).getStatus()).isEqualTo(ReportStatus.ACCEPTED);
        }

        @Test
        @DisplayName("페이지네이션 동작 확인")
        void findAll_Pagination() {
            // when
            Page<Report> page = reportRepository.findAllWithFilters(
                    null, null,
                    PageRequest.of(0, 1));

            // then
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Entity 메서드")
    class EntityMethods {

        @Test
        @DisplayName("신고 취소")
        void cancelReport() {
            // given
            Report report = reportRepository.save(Report.builder()
                    .reporter(reporter)
                    .targetType(ReportTargetType.REVIEW)
                    .targetId(review.getId())
                    .reason(ReportReason.REVIEW_ABUSE)
                    .build());

            // when
            report.cancel();
            reportRepository.flush();

            // then
            Report found = reportRepository.findById(report.getId()).get();
            assertThat(found.getStatus()).isEqualTo(ReportStatus.CANCELLED);
        }

        @Test
        @DisplayName("신고 상태 변경")
        void updateStatus() {
            // given
            Report report = reportRepository.save(Report.builder()
                    .reporter(reporter)
                    .targetType(ReportTargetType.REVIEW)
                    .targetId(review.getId())
                    .reason(ReportReason.REVIEW_ABUSE)
                    .build());

            // when
            report.updateStatus(ReportStatus.ACCEPTED);
            reportRepository.flush();

            // then
            Report found = reportRepository.findById(report.getId()).get();
            assertThat(found.getStatus()).isEqualTo(ReportStatus.ACCEPTED);
        }

        @Test
        @DisplayName("isPending 상태 확인")
        void isPending() {
            // given
            Report pendingReport = Report.builder()
                    .reporter(reporter)
                    .targetType(ReportTargetType.REVIEW)
                    .targetId(review.getId())
                    .reason(ReportReason.REVIEW_ABUSE)
                    .build();

            Report acceptedReport = Report.builder()
                    .reporter(reporter)
                    .targetType(ReportTargetType.USER)
                    .targetId(targetUser.getId())
                    .reason(ReportReason.USER_PRIVACY)
                    .build();
            acceptedReport.updateStatus(ReportStatus.ACCEPTED);

            // then
            assertThat(pendingReport.isPending()).isTrue();
            assertThat(acceptedReport.isPending()).isFalse();
        }

        @Test
        @DisplayName("isOwnedBy 소유자 확인")
        void isOwnedBy() {
            // given
            Report report = reportRepository.save(Report.builder()
                    .reporter(reporter)
                    .targetType(ReportTargetType.REVIEW)
                    .targetId(review.getId())
                    .reason(ReportReason.REVIEW_ABUSE)
                    .build());

            // then
            assertThat(report.isOwnedBy(reporter.getId())).isTrue();
            assertThat(report.isOwnedBy(targetUser.getId())).isFalse();
        }
    }
}
