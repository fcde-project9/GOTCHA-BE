# GOTCHA API 개발 스킬

## 필수 문서 확인
API 개발 전 반드시 읽을 것:
- `docs/api-spec.md` - API 상세 명세
- `docs/error-codes.md` - 에러 코드 정의
- `docs/coding-patterns.md` - 코딩 패턴
- `docs/auth-policy.md` - 인증/권한 정책

## Controller 작성 규칙

### 기본 구조 (인터페이스 분리 패턴)

Controller는 `*ControllerApi` 인터페이스를 implements 합니다.
Swagger 어노테이션은 인터페이스에만 작성합니다.

```java
@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
public class ShopController implements ShopControllerApi {

    private final ShopService shopService;

    @Override
    @GetMapping("/{shopId}")
    public ApiResponse<ShopDetailResponse> getShop(@PathVariable Long shopId) {
        return ApiResponse.success(shopService.getShop(shopId));
    }
}
```

### 응답 형식
- 모든 응답은 `ApiResponse<T>` 사용
- 성공: `ApiResponse.success(data)`
- 에러: 예외를 던지면 GlobalExceptionHandler에서 처리

## Service 작성 규칙

### 기본 구조
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShopService {

    private final ShopRepository shopRepository;

    public ShopDetailResponse getShop(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(ShopException::notFound);
        return ShopDetailResponse.from(shop);
    }

    @Transactional
    public ShopResponse createShop(CreateShopRequest request) {
        Shop shop = request.toEntity();
        return ShopResponse.from(shopRepository.save(shop));
    }
}
```

## DTO 작성 규칙

### Response DTO - `from()` 정적 팩토리
```java
public record ShopResponse(
    Long id,
    String name,
    String address
) {
    public static ShopResponse from(Shop shop) {
        return new ShopResponse(
            shop.getId(),
            shop.getName(),
            shop.getAddress()
        );
    }
}
```

### Request DTO - `toEntity()` 메서드
```java
public record CreateShopRequest(
    @NotBlank String name,
    @NotBlank String address
) {
    public Shop toEntity() {
        return Shop.builder()
                .name(name)
                .address(address)
                .build();
    }
}
```

## 예외 처리 규칙

### 도메인 예외 - 정적 팩토리 메서드
```java
public class ShopException extends BusinessException {
    private ShopException(ErrorCode errorCode) {
        super(errorCode);
    }

    public static ShopException notFound() {
        return new ShopException(ShopErrorCode.SHOP_NOT_FOUND);
    }

    public static ShopException alreadyClosed() {
        return new ShopException(ShopErrorCode.ALREADY_CLOSED);
    }
}
```

### 사용
```java
// 권장
throw ShopException.notFound();

// 지양
throw new ShopException(ShopErrorCode.SHOP_NOT_FOUND);
```

## 에러 코드 규칙

### 도메인별 코드 체계
| 도메인 | 접두사 | 예시 |
|--------|--------|------|
| Common | C | C001 |
| Auth | A | A001 |
| User | U | U001 |
| Shop | S | S001 |
| Favorite | F | F001 |
| Comment | CM | CM001 |
| Review | R | R001 |

### ErrorCode 구현
```java
@Getter
@RequiredArgsConstructor
public enum ShopErrorCode implements ErrorCode {
    SHOP_NOT_FOUND("S001", "가게를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    ALREADY_CLOSED("S002", "이미 폐업한 가게입니다", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
```

## Swagger 인터페이스 분리 패턴

### 왜 인터페이스로 분리하는가?
- Controller 가독성 향상 (Swagger 어노테이션이 많아지면 코드가 복잡해짐)
- 문서화와 구현의 관심사 분리
- SpringDoc이 리플렉션으로 인터페이스 어노테이션을 자동 추출

### Api 인터페이스 작성
```java
@Tag(name = "Shop", description = "가게 API")
public interface ShopControllerApi {

    @Operation(summary = "가게 상세 조회", description = "가게 상세 정보를 조회합니다")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "가게를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "S001",
                                        "message": "가게를 찾을 수 없습니다"
                                      }
                                    }
                                    """)
                    )
            )
    })
    ApiResponse<ShopDetailResponse> getShop(@PathVariable Long shopId);
}
```

### Controller 구현
```java
@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
public class ShopController implements ShopControllerApi {

    private final ShopService shopService;

    @Override
    @GetMapping("/{shopId}")
    public ApiResponse<ShopDetailResponse> getShop(@PathVariable Long shopId) {
        return ApiResponse.success(shopService.getShop(shopId));
    }
}
```

### 파일 구조
```
domain/shop/controller/
├── ShopController.java      # 구현 (Spring 어노테이션만)
└── ShopControllerApi.java   # 인터페이스 (Swagger 어노테이션)
```

### 필수 에러 응답
인증이 필요한 API는 다음 에러 응답을 포함해야 합니다:
- `401 (A001)`: 로그인이 필요합니다
- `401 (A012)`: 탈퇴한 사용자입니다 (getCurrentUser 사용 시)

## 완료 체크리스트
- [ ] ControllerApi 인터페이스 작성 (Swagger 어노테이션, 에러 응답 포함)
- [ ] Controller 작성 (인터페이스 implements)
- [ ] Service 작성 (@Transactional 적용)
- [ ] Request/Response DTO 작성
- [ ] ErrorCode 추가 (없는 경우)
- [ ] Exception 클래스 추가 (없는 경우)
- [ ] 테스트 코드 작성
- [ ] docs/api-spec.md 업데이트
