package com.gotcha.domain.shop.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gotcha._global.exception.GlobalExceptionHandler;
import com.gotcha.domain.favorite.service.FavoriteService;
import com.gotcha.domain.shop.dto.CreateShopRequest;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.service.ShopService;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ShopControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @InjectMocks
    private ShopController shopController;

    @Mock
    private ShopService shopService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FavoriteService favoriteService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(shopController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(converter)
                .build();
    }

    @Nested
    @DisplayName("POST /api/shops/save - 가게 생성")
    class SaveShop {

        @Test
        @DisplayName("비회원도 가게를 생성할 수 있다 (createdBy = null)")
        void shouldAllowNonMemberToCreateShop() throws Exception {
            // given
            CreateShopRequest request = new CreateShopRequest(
                    "테스트 가게",
                    "https://example.com/image.jpg",
                    "테스트 힌트",
                    Map.of("Mon", "10:00~22:00")
            );

            Shop shop = createShop(1L, "테스트 가게", null);

            given(shopService.createShop(
                    anyString(),
                    anyDouble(),
                    anyDouble(),
                    anyString(),
                    anyString(),
                    any(),
                    isNull()
            )).willReturn(shop);

            // when & then
            mockMvc.perform(post("/api/shops/save")
                            .param("latitude", "37.5172")
                            .param("longitude", "127.0473")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("테스트 가게"));
        }

        @Test
        @DisplayName("가게명이 없으면 400 에러를 반환한다")
        void shouldReturn400WhenNameIsBlank() throws Exception {
            // given
            CreateShopRequest request = new CreateShopRequest(
                    "",  // 빈 가게명
                    "https://example.com/image.jpg",
                    "테스트 힌트",
                    null
            );

            // when & then
            mockMvc.perform(post("/api/shops/save")
                            .param("latitude", "37.5172")
                            .param("longitude", "127.0473")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("가게명이 2자 미만이면 400 에러를 반환한다")
        void shouldReturn400WhenNameIsTooShort() throws Exception {
            // given
            CreateShopRequest request = new CreateShopRequest(
                    "가",  // 1자
                    null,
                    null,
                    null
            );

            // when & then
            mockMvc.perform(post("/api/shops/save")
                            .param("latitude", "37.5172")
                            .param("longitude", "127.0473")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("위도가 없으면 400 에러를 반환한다")
        void shouldReturn400WhenLatitudeIsMissing() throws Exception {
            // given
            CreateShopRequest request = new CreateShopRequest(
                    "테스트 가게",
                    null,
                    null,
                    null
            );

            // when & then
            mockMvc.perform(post("/api/shops/save")
                            .param("longitude", "127.0473")  // latitude 누락
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("경도가 없으면 400 에러를 반환한다")
        void shouldReturn400WhenLongitudeIsMissing() throws Exception {
            // given
            CreateShopRequest request = new CreateShopRequest(
                    "테스트 가게",
                    null,
                    null,
                    null
            );

            // when & then
            mockMvc.perform(post("/api/shops/save")
                            .param("latitude", "37.5172")  // longitude 누락
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("위도가 범위를 벗어나면 400 에러를 반환한다")
        void shouldReturn400WhenLatitudeOutOfRange() throws Exception {
            // given
            CreateShopRequest request = new CreateShopRequest(
                    "테스트 가게",
                    null,
                    null,
                    null
            );

            // when & then
            mockMvc.perform(post("/api/shops/save")
                            .param("latitude", "91.0")  // 범위 초과
                            .param("longitude", "127.0473")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("경도가 범위를 벗어나면 400 에러를 반환한다")
        void shouldReturn400WhenLongitudeOutOfRange() throws Exception {
            // given
            CreateShopRequest request = new CreateShopRequest(
                    "테스트 가게",
                    null,
                    null,
                    null
            );

            // when & then
            mockMvc.perform(post("/api/shops/save")
                            .param("latitude", "37.5172")
                            .param("longitude", "181.0")  // 범위 초과
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("찾아가는 힌트가 500자를 초과하면 400 에러를 반환한다")
        void shouldReturn400WhenLocationHintTooLong() throws Exception {
            // given
            String longHint = "a".repeat(501);
            CreateShopRequest request = new CreateShopRequest(
                    "테스트 가게",
                    null,
                    longHint,
                    null
            );

            // when & then
            mockMvc.perform(post("/api/shops/save")
                            .param("latitude", "37.5172")
                            .param("longitude", "127.0473")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    private Shop createShop(Long id, String name, User createdBy) {
        Shop shop = Shop.builder()
                .name(name)
                .addressName("테스트 주소")
                .latitude(37.5172)
                .longitude(127.0473)
                .createdBy(createdBy)
                .build();
        ReflectionTestUtils.setField(shop, "id", id);
        return shop;
    }

    private User createUser(Long id, String nickname) {
        User user = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("kakao-" + id)
                .nickname(nickname)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
