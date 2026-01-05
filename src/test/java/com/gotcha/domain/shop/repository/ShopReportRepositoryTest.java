package com.gotcha.domain.shop.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gotcha.config.TestcontainersConfig;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.entity.ShopReport;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfig.class)
class ShopReportRepositoryTest {

    @Autowired
    private ShopReportRepository shopReportRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private UserRepository userRepository;

    private Shop shop;
    private User reporter;

    @BeforeEach
    void setUp() {
        User creator = userRepository.save(User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("creator123")
                .nickname("제보자")
                .build());

        shop = shopRepository.save(Shop.builder()
                .name("가챠샵")
                .address("서울시 강남구")
                .latitude(37.4979)
                .longitude(127.0276)
                .createdBy(creator)
                .build());

        reporter = userRepository.save(User.builder()
                .socialType(SocialType.GOOGLE)
                .socialId("reporter123")
                .nickname("신고자")
                .build());
    }

    @Test
    @DisplayName("샵 제보 저장")
    void save() {
        // given
        ShopReport report = ShopReport.builder()
                .shop(shop)
                .reporter(reporter)
                .reportTitle("new")
                .reportContent("새로운 가챠샵 발견")
                .isAnonymous(false)
                .build();

        // when
        ShopReport savedReport = shopReportRepository.save(report);

        // then
        assertThat(savedReport.getId()).isNotNull();
        assertThat(savedReport.getReportTitle()).isEqualTo("new");
        assertThat(savedReport.getReporter().getNickname()).isEqualTo("신고자");
    }

    @Test
    @DisplayName("ID로 샵 제보 조회")
    void findById() {
        // given
        ShopReport report = ShopReport.builder()
                .shop(shop)
                .reporter(reporter)
                .reportTitle("update")
                .reportContent("영업시간 변경")
                .isAnonymous(true)
                .build();
        ShopReport savedReport = shopReportRepository.save(report);

        // when
        Optional<ShopReport> found = shopReportRepository.findById(savedReport.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getReportTitle()).isEqualTo("update");
        assertThat(found.get().getIsAnonymous()).isTrue();
    }

    @Test
    @DisplayName("샵 제보 삭제")
    void delete() {
        // given
        ShopReport report = ShopReport.builder()
                .shop(shop)
                .reporter(reporter)
                .reportTitle("duplicate")
                .reportContent("중복 등록된 가게")
                .build();
        ShopReport savedReport = shopReportRepository.save(report);
        Long reportId = savedReport.getId();

        // when
        shopReportRepository.deleteById(reportId);

        // then
        assertThat(shopReportRepository.findById(reportId)).isEmpty();
    }
}
