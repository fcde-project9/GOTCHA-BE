# 에러 코드 정의

## 코드 체계

```
{도메인}{숫자3자리}

도메인:
- C : Common (공통)
- A : Auth (인증)
- U : User (사용자)
- S : Shop (가게)
- F : Favorite (찜)
- CM : Comment (댓글)
- R : Review (리뷰)
- FL : File (파일)
- RP : Report (신고)
```

---

## 공통 에러 (C)

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| C001 | 400 | 잘못된 요청입니다 | 요청 형식 오류 |
| C002 | 400 | 필수 파라미터가 누락되었습니다 | 필수 값 누락 |
| C003 | 400 | 유효하지 않은 파라미터입니다 | 값 범위/형식 오류 |
| C004 | 500 | 서버 오류가 발생했습니다 | 내부 서버 오류 |
| C005 | 404 | 요청한 리소스를 찾을 수 없습니다 | 잘못된 URL |

---

## 인증 에러 (A)

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| A001 | 401 | 로그인이 필요합니다 | 인증 토큰 없음 |
| A002 | 403 | 권한이 없습니다 | 접근 권한 부족 |
| A003 | 401 | 토큰이 만료되었습니다 | Access Token 만료 |
| A004 | 401 | 유효하지 않은 토큰입니다 | 토큰 검증 실패 |
| A005 | 401 | 소셜 로그인에 실패했습니다 | 소셜 API 오류 |
| A006 | 400 | 지원하지 않는 소셜 로그인입니다 | 잘못된 provider |
| A007 | 401 | 로그인을 취소했습니다 | OAuth 취소 |
| A008 | 401 | OAuth 토큰이 유효하지 않습니다 | OAuth 토큰 오류 |
| A009 | 401 | OAuth 응답을 처리할 수 없습니다 | OAuth 응답 오류 |
| A010 | 401 | 리프레시 토큰을 찾을 수 없습니다 | DB에 토큰 없음 |
| A011 | 401 | 리프레시 토큰이 만료되었습니다 | Refresh Token 만료 |
| A012 | 401 | 탈퇴한 사용자입니다 | 탈퇴 사용자 로그인/API 접근 시도 |

---

## 사용자 에러 (U)

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| U001 | 409 | 이미 사용 중인 닉네임입니다 | 닉네임 중복 |
| U002 | 400 | 닉네임 형식이 올바르지 않습니다 | 길이/문자 제한 위반 |
| U003 | 400 | 사용할 수 없는 닉네임입니다 | 금지어 포함 |
| U004 | 404 | 사용자를 찾을 수 없습니다 | 존재하지 않는 사용자 |
| U005 | 400 | 이미 탈퇴한 사용자입니다 | 탈퇴 처리된 사용자 재탈퇴 시도 |

---

## 가게 에러 (S)

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| S001 | 404 | 가게를 찾을 수 없습니다 | 존재하지 않는 가게 |
| S002 | 409 | 이미 등록된 가게입니다 | 중복 제보 |
| S003 | 400 | 가게명은 2-100자여야 합니다 | 이름 길이 오류 |
| S004 | 400 | 유효하지 않은 좌표입니다 | 위도/경도 범위 오류 |
| S005 | 400 | 검색 반경은 최대 5000m입니다 | 반경 초과 |
| S006 | 500 | 카카오 API 호출 중 오류가 발생했습니다 | 카카오 API 오류 |
| S007 | 404 | 해당 좌표의 주소를 찾을 수 없습니다 | 주소 조회 실패 |
| S008 | 403 | 가게를 수정/삭제할 권한이 없습니다 | ADMIN 아닌 사용자의 가게 수정/삭제 시도 |

---

## 찜 에러 (F)

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| F001 | 409 | 이미 찜한 가게입니다 | 중복 찜 |
| F002 | 404 | 찜 정보를 찾을 수 없습니다 | 찜하지 않은 가게 삭제 시도 |
| F003 | 403 | 본인의 찜만 삭제할 수 있습니다 | 타인 찜 삭제 시도 |

---

## 댓글 에러 (CM)

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| CM001 | 404 | 댓글을 찾을 수 없습니다 | 존재하지 않는 댓글 |
| CM002 | 400 | 댓글은 1-500자여야 합니다 | 길이 오류 |
| CM003 | 403 | 본인의 댓글만 수정할 수 있습니다 | 타인 댓글 수정 |
| CM004 | 403 | 본인의 댓글만 삭제할 수 있습니다 | 타인 댓글 삭제 |
| CM005 | 400 | 수정 가능 기간이 지났습니다 | 24시간 경과 |

---

## 리뷰 에러 (R)

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| R001 | 404 | 리뷰를 찾을 수 없습니다 | 존재하지 않는 리뷰 |
| R002 | 400 | 리뷰는 10-1000자여야 합니다 | 길이 오류 |
| R003 | 403 | 본인의 리뷰만 수정/삭제할 수 있습니다 | 타인 리뷰 수정/삭제 |
| R005 | 400 | 이미지는 최대 10개까지 첨부 가능합니다 | 이미지 개수 초과 |
| R006 | 409 | 이미 좋아요한 리뷰입니다 | 중복 좋아요 |
| R007 | 404 | 좋아요를 찾을 수 없습니다 | 좋아요하지 않은 리뷰 취소 시도 |

---

## 파일 에러 (FL)

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| FL001 | 400 | 파일이 비어있습니다 | 빈 파일 업로드 |
| FL002 | 400 | 파일 크기가 너무 큽니다 | 크기 제한 초과 (20MB) |
| FL003 | 400 | 지원하지 않는 파일 형식입니다 | 허용되지 않은 확장자 |
| FL004 | 500 | 파일 업로드에 실패했습니다 | S3 업로드 오류 |
| FL005 | 500 | 파일 삭제에 실패했습니다 | S3 삭제 오류 |

---

## 신고 에러 (RP)

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| RP001 | 404 | 신고를 찾을 수 없습니다 | 존재하지 않는 신고 |
| RP002 | 409 | 이미 신고한 대상입니다 | 중복 신고 |
| RP003 | 404 | 신고 대상을 찾을 수 없습니다 | 존재하지 않는 리뷰/가게/유저 신고 |
| RP004 | 400 | 본인을 신고할 수 없습니다 | 본인 리뷰/프로필 신고 시도 |
| RP005 | 400 | 기타 사유 선택 시 상세 내용을 입력해주세요 | *_OTHER 사유에 detail 누락 |
| RP006 | 403 | 본인의 신고만 취소할 수 있습니다 | 타인 신고 취소 시도 |
| RP007 | 400 | 이미 처리된 신고는 취소할 수 없습니다 | PENDING 아닌 신고 취소 시도 |
| RP008 | 403 | 관리자만 접근할 수 있습니다 | 비관리자 관리 기능 접근 |
| RP009 | 400 | 해당 신고 대상에 사용할 수 없는 사유입니다 | targetType과 reason prefix 불일치 |
| RP010 | 400 | 허용되지 않는 상태 변경입니다 | ACCEPTED/REJECTED 외 상태로 변경 시도 또는 PENDING 아닌 신고 상태 변경 |

---

## 구현 예시

### ErrorCode 인터페이스

```java
public interface ErrorCode {
    HttpStatus getStatus();
    String getCode();
    String getMessage();
}
```

### 도메인별 ErrorCode Enum

```java
@Getter
@RequiredArgsConstructor
public enum ShopErrorCode implements ErrorCode {

    SHOP_NOT_FOUND(NOT_FOUND, "S001", "가게를 찾을 수 없습니다"),
    SHOP_ALREADY_EXISTS(CONFLICT, "S002", "이미 등록된 가게입니다"),
    INVALID_SHOP_NAME(BAD_REQUEST, "S003", "가게명은 2-100자여야 합니다"),
    INVALID_COORDINATES(BAD_REQUEST, "S004", "유효하지 않은 좌표입니다"),
    RADIUS_TOO_LARGE(BAD_REQUEST, "S005", "검색 반경은 최대 5000m입니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

### 도메인 Exception

```java
public class ShopException extends BusinessException {

    private ShopException(ErrorCode errorCode) {
        super(errorCode);
    }

    public static ShopException notFound() {
        return new ShopException(ShopErrorCode.SHOP_NOT_FOUND);
    }

    public static ShopException alreadyExists() {
        return new ShopException(ShopErrorCode.SHOP_ALREADY_EXISTS);
    }

    public static ShopException invalidName() {
        return new ShopException(ShopErrorCode.INVALID_SHOP_NAME);
    }
}
```

### 사용 예시

```java
@Service
public class ShopService {

    public Shop findById(Long id) {
        return shopRepository.findById(id)
            .orElseThrow(ShopException::notFound);
    }

    public void validateName(String name) {
        if (name.length() < 2 || name.length() > 100) {
            throw ShopException.invalidName();
        }
    }
}
```
