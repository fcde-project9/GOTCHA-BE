# 코딩 패턴 가이드

## 정적 팩토리 메서드 패턴

### 1. Entity → DTO 변환: `from()`

```java
@Getter
@Builder
public class ShopResponse {

    private Long id;
    private String name;
    private String address;

    public static ShopResponse from(Shop shop) {
        return ShopResponse.builder()
                .id(shop.getId())
                .name(shop.getName())
                .address(shop.getAddress())
                .build();
    }
}
```

**Record 사용 시:**
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

### 2. DTO → Entity 변환: `toEntity()`

```java
@Getter
public class CreateShopRequest {

    private String name;
    private String address;

    public Shop toEntity() {
        return Shop.builder()
                .name(name)
                .address(address)
                .build();
    }
}
```

### 3. 다중 Entity 변환

```java
public record ShopDetailResponse(
    Long id,
    String name,
    OwnerSummary owner
) {
    public static ShopDetailResponse from(Shop shop, User owner) {
        return new ShopDetailResponse(
            shop.getId(),
            shop.getName(),
            OwnerSummary.from(owner)
        );
    }

    public record OwnerSummary(Long id, String name) {
        public static OwnerSummary from(User user) {
            return new OwnerSummary(user.getId(), user.getName());
        }
    }
}
```

### 4. 공통 응답 클래스: `success()`, `error()`

```java
// 성공 응답
return ApiResponse.success(ShopResponse.from(shop));

// 에러 응답
return ApiResponse.error(ShopErrorCode.SHOP_NOT_FOUND);
```

---

## 도메인 예외 패턴

### 정적 팩토리 메서드로 예외 생성

생성자를 `private`으로 제한하고, 정적 메서드로만 예외를 생성합니다.

```java
public class ShopException extends BusinessException {

    // 생성자는 private
    private ShopException(ErrorCode errorCode) {
        super(errorCode);
    }

    private ShopException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    // 정적 팩토리 메서드
    public static ShopException notFound() {
        return new ShopException(ShopErrorCode.SHOP_NOT_FOUND);
    }

    public static ShopException notFound(Long shopId) {
        return new ShopException(ShopErrorCode.SHOP_NOT_FOUND, "ID: " + shopId);
    }

    public static ShopException alreadyClosed() {
        return new ShopException(ShopErrorCode.ALREADY_CLOSED);
    }

    public static ShopException unauthorized() {
        return new ShopException(ShopErrorCode.UNAUTHORIZED);
    }
}
```

### 사용 예시

```java
// 기존 방식 (지양)
throw new ShopException(ShopErrorCode.SHOP_NOT_FOUND);

// 정적 팩토리 메서드 방식 (권장)
throw ShopException.notFound();
throw ShopException.notFound(shopId);  // 추가 정보 포함
```

### 장점

| 항목 | 설명 |
|------|------|
| 가독성 | `notFound()`가 `new ShopException(ShopErrorCode.SHOP_NOT_FOUND)`보다 명확 |
| 일관성 | 예외 생성 방식 통일 |
| 캡슐화 | ErrorCode 내부 구현 숨김 |
| 확장성 | 추가 정보 오버로드 메서드 제공 가능 |

---

## Entity Builder 패턴

### 기본 구조

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Builder
    public Shop(String name, String address) {
        this.name = name;
        this.address = address;
    }
}
```

### Null 안전 기본값 설정

```java
@Builder
public Shop(String name, Boolean isActive) {
    this.name = name;
    this.isActive = isActive != null ? isActive : true;
}
```

---

## 패턴 요약

| 용도 | 메서드명 | 위치 | 예시 |
|------|---------|------|------|
| Entity → DTO | `from()` | Response DTO | `ShopResponse.from(shop)` |
| DTO → Entity | `toEntity()` | Request DTO | `request.toEntity()` |
| 성공 응답 | `success()` | ApiResponse | `ApiResponse.success(data)` |
| 에러 응답 | `error()` | ApiResponse | `ApiResponse.error(errorCode)` |
| 도메인 예외 | `notFound()` 등 | 도메인 Exception | `ShopException.notFound()` |
| Null-safe 추출 | `getXxxOrNull()` | 유틸/Principal | `getUserIdOrNull(principal)` |
