# GOTCHA 테스트 작성 스킬

## 필수 문서 확인
테스트 작성 전 반드시 읽을 것:
- `docs/repository-edge-cases.md` - Repository 엣지 케이스

## Repository 테스트

### 기본 구조
```java
@DataJpaTest
@ActiveProfiles("test")
class ShopRepositoryTest {

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("주변 가게 조회 - 반경 내 가게만 반환")
    void findNearbyShops_withinRadius_returnsShops() {
        // given
        Shop shop = createShop("테스트 가게", 37.5665, 126.9780);
        em.persist(shop);
        em.flush();
        em.clear();

        // when
        List<Shop> result = shopRepository.findNearbyShops(37.5665, 126.9780, 1.0);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("테스트 가게");
    }

    private Shop createShop(String name, Double lat, Double lng) {
        return Shop.builder()
                .name(name)
                .latitude(lat)
                .longitude(lng)
                .build();
    }
}
```

### 엣지 케이스 테스트
- 빈 결과 처리
- Null 파라미터 처리
- 경계값 테스트
- 대량 데이터 페이징

## Service 테스트

### 기본 구조
```java
@ExtendWith(MockitoExtension.class)
class ShopServiceTest {

    @InjectMocks
    private ShopService shopService;

    @Mock
    private ShopRepository shopRepository;

    @Test
    @DisplayName("가게 조회 - 존재하는 경우 반환")
    void getShop_exists_returnsShop() {
        // given
        Long shopId = 1L;
        Shop shop = createShop(shopId, "테스트 가게");
        given(shopRepository.findById(shopId)).willReturn(Optional.of(shop));

        // when
        ShopDetailResponse result = shopService.getShop(shopId);

        // then
        assertThat(result.id()).isEqualTo(shopId);
        assertThat(result.name()).isEqualTo("테스트 가게");
    }

    @Test
    @DisplayName("가게 조회 - 존재하지 않는 경우 예외")
    void getShop_notExists_throwsException() {
        // given
        Long shopId = 999L;
        given(shopRepository.findById(shopId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> shopService.getShop(shopId))
                .isInstanceOf(ShopException.class);
    }

    private Shop createShop(Long id, String name) {
        return Shop.builder()
                .name(name)
                .build();
    }
}
```

## Controller 테스트

### 기본 구조
```java
@WebMvcTest(ShopController.class)
@AutoConfigureMockMvc(addFilters = false)  // Security 필터 비활성화
class ShopControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShopService shopService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/shops/{id} - 성공")
    void getShop_success() throws Exception {
        // given
        Long shopId = 1L;
        ShopDetailResponse response = new ShopDetailResponse(shopId, "테스트 가게", "주소");
        given(shopService.getShop(shopId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/shops/{id}", shopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(shopId))
                .andExpect(jsonPath("$.data.name").value("테스트 가게"));
    }

    @Test
    @DisplayName("POST /api/shops - 유효성 검증 실패")
    void createShop_invalidRequest_badRequest() throws Exception {
        // given
        CreateShopRequest request = new CreateShopRequest("", "");  // 빈 값

        // when & then
        mockMvc.perform(post("/api/shops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
```

## 테스트 명명 규칙

### 메서드명
```
{메서드명}_{시나리오}_{예상결과}
```

예시:
- `findById_exists_returnsShop`
- `findById_notExists_returnsEmpty`
- `save_validInput_success`
- `save_duplicateName_throwsException`

### @DisplayName
```java
@DisplayName("가게 조회 - 존재하는 경우 반환")
@DisplayName("가게 조회 - 존재하지 않는 경우 예외 발생")
```

## 테스트 프로파일

### application-test.yml
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
```

## Given-When-Then 패턴

```java
@Test
void methodName_scenario_expectedResult() {
    // given - 테스트 데이터 준비

    // when - 테스트 대상 실행

    // then - 결과 검증
}
```

## 완료 체크리스트
- [ ] Repository 테스트 (정상 케이스)
- [ ] Repository 테스트 (엣지 케이스)
- [ ] Service 테스트 (정상 케이스)
- [ ] Service 테스트 (예외 케이스)
- [ ] Controller 테스트 (필요시)
- [ ] @DisplayName 작성
- [ ] Given-When-Then 패턴 적용
