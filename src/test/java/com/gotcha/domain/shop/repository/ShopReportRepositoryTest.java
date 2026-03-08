package com.gotcha.domain.shop.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gotcha.config.TestcontainersConfig;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.entity.ShopSuggestion;
import com.gotcha.domain.shop.entity.SuggestionReason;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import java.util.List;
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
class ShopSuggestionRepositoryTest {

    @Autowired
    private ShopSuggestionRepository shopSuggestionRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private UserRepository userRepository;

    private Shop shop;
    private User suggester;

    @BeforeEach
    void setUp() {
        User creator = userRepository.save(User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("creator123")
                .nickname("제보자")
                .build());

        shop = shopRepository.save(Shop.builder()
                .name("가챠샵")
                .addressName("서울시 강남구")
                .latitude(37.4979)
                .longitude(127.0276)
                .createdBy(creator)
                .build());

        suggester = userRepository.save(User.builder()
                .socialType(SocialType.GOOGLE)
                .socialId("suggester123")
                .nickname("제안자")
                .build());
    }

    @Test
    @DisplayName("가게 정보 수정 제안 저장")
    void save() {
        // given
        ShopSuggestion suggestion = ShopSuggestion.builder()
                .shop(shop)
                .suggester(suggester)
                .reasons(List.of(SuggestionReason.WRONG_BUSINESS_HOURS, SuggestionReason.WRONG_ADDRESS))
                .build();

        // when
        ShopSuggestion saved = shopSuggestionRepository.save(suggestion);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getReasons()).containsExactlyInAnyOrder(
                SuggestionReason.WRONG_BUSINESS_HOURS, SuggestionReason.WRONG_ADDRESS);
        assertThat(saved.getSuggester().getNickname()).isEqualTo("제안자");
        assertThat(saved.getShop().getId()).isEqualTo(shop.getId());
    }

    @Test
    @DisplayName("ID로 제안 조회")
    void findById() {
        // given
        ShopSuggestion saved = shopSuggestionRepository.save(ShopSuggestion.builder()
                .shop(shop)
                .suggester(suggester)
                .reasons(List.of(SuggestionReason.WRONG_PHOTO))
                .build());

        // when & then
        assertThat(shopSuggestionRepository.findById(saved.getId())).isPresent();
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회 시 빈 Optional 반환")
    void findById_NotFound() {
        assertThat(shopSuggestionRepository.findById(999999L)).isEmpty();
    }

    @Test
    @DisplayName("가게 삭제 시 해당 가게의 제안 기록 모두 삭제")
    void deleteAllByShopId() {
        // given
        User anotherCreator = userRepository.save(User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("creator456")
                .nickname("다른제보자")
                .build());
        Shop anotherShop = shopRepository.save(Shop.builder()
                .name("다른가챠샵")
                .addressName("서울시 서초구")
                .latitude(37.4920)
                .longitude(127.0276)
                .createdBy(anotherCreator)
                .build());

        shopSuggestionRepository.save(ShopSuggestion.builder()
                .shop(shop)
                .suggester(suggester)
                .reasons(List.of(SuggestionReason.WRONG_ADDRESS))
                .build());
        shopSuggestionRepository.save(ShopSuggestion.builder()
                .shop(shop)
                .suggester(suggester)
                .reasons(List.of(SuggestionReason.WRONG_PHOTO))
                .build());
        ShopSuggestion otherShopSuggestion = shopSuggestionRepository.save(ShopSuggestion.builder()
                .shop(anotherShop)
                .suggester(suggester)
                .reasons(List.of(SuggestionReason.WRONG_BUSINESS_HOURS))
                .build());

        // when
        shopSuggestionRepository.deleteAllByShopId(shop.getId());

        // then
        List<ShopSuggestion> remaining = shopSuggestionRepository.findAll();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getId()).isEqualTo(otherShopSuggestion.getId());
    }

    @Test
    @DisplayName("회원 탈퇴 시 해당 사용자의 제안 기록 모두 삭제")
    void deleteBySuggesterId() {
        // given
        User anotherSuggester = userRepository.save(User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("suggester456")
                .nickname("다른제안자")
                .build());

        shopSuggestionRepository.save(ShopSuggestion.builder()
                .shop(shop)
                .suggester(suggester)
                .reasons(List.of(SuggestionReason.WRONG_BUSINESS_HOURS))
                .build());
        ShopSuggestion otherSuggestion = shopSuggestionRepository.save(ShopSuggestion.builder()
                .shop(shop)
                .suggester(anotherSuggester)
                .reasons(List.of(SuggestionReason.WRONG_ADDRESS))
                .build());

        // when
        shopSuggestionRepository.deleteBySuggesterId(suggester.getId());

        // then
        List<ShopSuggestion> remaining = shopSuggestionRepository.findAll();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getId()).isEqualTo(otherSuggestion.getId());
    }
}
