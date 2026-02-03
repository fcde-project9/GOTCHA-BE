# 보안 강화 로드맵

## 현황 분석

### 구현 완료

| 항목 | 설명 | 구현 위치 |
|------|------|----------|
| JWT 인증 | Access Token 15분, Refresh Token 7일 | `JwtTokenProvider`, `JwtAuthenticationFilter` |
| OAuth2 소셜 로그인 | 카카오, 구글, 네이버 | `CustomOAuth2UserService` |
| CORS 설정 | 환경변수 기반 허용 도메인 | `SecurityConfig` |
| Stateless 세션 | CSRF 불필요, 서버 부하 감소 | `SecurityConfig` |
| 엔드포인트 권한 | Public/Authenticated/Admin 구분 | `SecurityConfig` |
| 본인 확인 | 리소스 수정/삭제 시 작성자 검증 | 각 Service |
| SQL Injection 방지 | JPA 사용으로 파라미터 바인딩 | Repository 레이어 |
| 금칙어 필터링 | 닉네임 욕설/비속어 차단 | `ForbiddenWordService` |

### 미흡한 부분

| 항목 | 위험도 | 현재 상태 |
|------|--------|----------|
| OAuth 토큰 코드 재사용 방지 | 높음 | 미구현 |
| Rate Limiting | 높음 | 미구현 |
| Audit Log | 중간 | 미구현 |
| 로그인 시도 제한 | 중간 | 미구현 |
| IP 블랙리스트 | 낮음 | 미구현 |

---

## 보안 테스트 결과 (2025-01-20)

### 테스트 환경

- API 서버: `https://api.dev.gotcha.it.com`
- 테스트 방법: 동일 API에 연속 요청하여 429 응답 여부 확인

### Rate Limiting 테스트 결과

| API | 요청 수 | 응답 코드 | 429 응답 | 결과 |
|-----|--------|----------|---------|------|
| GET /api/users/me | 20회 | 모두 401 | 0회 | ❌ 취약 |
| POST /api/auth/token | 10회 | 모두 400 | 0회 | ❌ 취약 |
| POST /api/auth/reissue | 10회 | 모두 401 | 0회 | ❌ 취약 |
| GET /api/auth/callback/kakao | 10회 | 모두 302 | 0회 | ❌ 취약 |

### 인증 테스트 결과

| API | 테스트 | 응답 | 결과 |
|-----|--------|------|------|
| GET /api/users/me | 토큰 없이 접근 | A001 (로그인 필요) | ✅ 정상 |
| GET /api/users/me/favorites | 토큰 없이 접근 | A001 (로그인 필요) | ✅ 정상 |

### 결론

- **Rate Limiting**: 모든 API에 미적용 → 무제한 호출 가능 ⚠️
- **인증 체크**: 정상 동작 ✅
- **미테스트**: OAuth 코드 재사용, 다른 사용자 리소스 수정 (로그인 필요)

---

## 긴급: OAuth 토큰 코드 재사용 방지

### 현재 문제

OAuth 로그인 성공 시 암호화된 토큰 코드를 URL 파라미터로 전달하는데, 이 코드의 재사용을 방지하는 로직이 없음.

```
현재 플로우:
1. 로그인 성공 → 암호화된 코드 생성
2. 프론트엔드로 리다이렉트: /callback?code=암호화코드
3. 프론트가 POST /api/auth/token { code: 암호화코드 }
4. 토큰 반환

문제점:
- 3번을 여러 번 호출해도 매번 성공 (재사용 가능)
- 암호화 코드가 탈취되면 무제한 사용 가능
```

### 코드 위치

| 파일 | 메서드 | 재사용 방지 |
|------|--------|------------|
| `OAuthTokenCookieService.java` | `exchangeCode()` | ✅ 쿠키 삭제 |
| `OAuthTokenCookieService.java` | `decryptTokens()` | ❌ 없음 (현재 사용 중) |

### 위험도

| 요소 | 상태 |
|------|------|
| 암호화 강도 | ✅ AES-256-GCM (강력) |
| 탈취 경로 | 브라우저 히스토리, 서버 로그, 네트워크 |
| 유효 기간 | ❌ 무제한 |
| 재사용 | ❌ 무제한 |

### 해결 방안

**방법 1: Cache 기반 일회용 코드 (권장)**

```java
private final Cache<String, TokenData> tokenCache = Caffeine.newBuilder()
    .expireAfterWrite(30, TimeUnit.SECONDS)  // 30초 TTL
    .maximumSize(10_000)
    .build();

public String encryptTokens(String accessToken, String refreshToken, boolean isNewUser) {
    String code = UUID.randomUUID().toString();
    TokenData tokenData = new TokenData(accessToken, refreshToken, isNewUser);
    tokenCache.put(code, tokenData);
    return code;  // 암호화 대신 일회용 코드 반환
}

public TokenData decryptTokens(String code) {
    TokenData data = tokenCache.getIfPresent(code);
    if (data != null) {
        tokenCache.invalidate(code);  // 즉시 삭제 (1회용)
    }
    return data;
}
```

**방법 2: 암호화 코드에 만료 시간 포함**

```java
public class TokenData {
    private String accessToken;
    private String refreshToken;
    private boolean newUser;
    private long expiresAt;  // 만료 시간 추가
}
```

### 작업 목록

- [ ] Caffeine Cache 의존성 확인/추가
- [ ] `OAuthTokenCookieService`에 Cache 기반 일회용 코드 방식 적용
- [ ] 기존 암호화 방식 제거 또는 유지 (선택)
- [ ] TTL 설정 (30초 권장)
- [ ] 테스트 작성

---

## Phase 1: Rate Limiting (API 남용 방지)

### 목적

- **DDoS 방어**: 대량 요청으로 인한 서버 과부하 방지
- **Brute Force 방어**: 로그인 무차별 대입 공격 방지
- **API 남용 방지**: 크롤링, 스팸 등 비정상 사용 차단
- **비용 보호**: 클라우드 리소스 과다 사용 방지

### 구현 범위

| 엔드포인트 | 제한 | 기준 | 비고 |
|-----------|------|------|------|
| 일반 API | 100 req/min | IP | 조회 API |
| 로그인 | 5 req/min | IP | Brute Force 방어 |
| 회원가입 | 3 req/min | IP | 스팸 계정 방지 |
| 파일 업로드 | 10 req/min | User | 저장소 남용 방지 |
| 리뷰/댓글 작성 | 10 req/min | User | 스팸 방지 |

### 기술 선택지

| 기술 | 장점 | 단점 | 적합 상황 |
|------|------|------|----------|
| **Bucket4j + Caffeine** | 간단, 의존성 적음 | 단일 서버만 지원 | 소규모, 단일 인스턴스 |
| **Bucket4j + Redis** | 분산 환경 지원 | Redis 의존성 추가 | 다중 인스턴스 |
| **Resilience4j** | Circuit Breaker 등 추가 기능 | 학습 곡선 | 복잡한 장애 대응 필요 시 |
| **Nginx/API Gateway** | 애플리케이션 부하 없음 | 인프라 설정 필요 | 대규모 트래픽 |

**권장**: 현재 규모에서는 **Bucket4j + Caffeine** 시작, 확장 시 Redis 전환

### 작업 목록

- [ ] 의존성 추가 (`bucket4j-core`, `caffeine`)
- [ ] `RateLimitConfig` 설정 클래스 생성
- [ ] `RateLimitFilter` 또는 Interceptor 구현
- [ ] 엔드포인트별 제한 설정
- [ ] 429 Too Many Requests 응답 처리
- [ ] `RateLimitErrorCode` 추가
- [ ] 단위 테스트 작성
- [ ] 통합 테스트 작성

### 응답 형식

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "R001",
    "message": "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."
  }
}
```

### 참고 헤더

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1704067260
Retry-After: 60
```

---

## Phase 2: Audit Log (감사 로그)

### 목적

- **보안 이벤트 추적**: 누가, 언제, 무엇을 했는지 기록
- **문제 원인 분석**: 장애/보안 사고 발생 시 원인 파악
- **법적 대응 근거**: 분쟁 시 증거 자료
- **사용자 행동 분석**: 어뷰징 패턴 감지

### 로깅 대상 이벤트

| 카테고리 | 이벤트 | 저장 정보 | 보존 기간 |
|----------|--------|----------|----------|
| **인증** | 로그인 성공/실패 | userId, IP, UA, socialType | 1년 |
| **인증** | 로그아웃 | userId, IP | 1년 |
| **인증** | 토큰 재발급 | userId, IP | 6개월 |
| **계정** | 회원가입 | userId, socialType | 영구 |
| **계정** | 회원탈퇴 | userId, 탈퇴사유 | 영구 |
| **계정** | 닉네임 변경 | userId, 이전/이후 닉네임 | 1년 |
| **콘텐츠** | 리뷰 삭제 | reviewId, 작성자, 내용 요약 | 1년 |
| **콘텐츠** | 댓글 삭제 | commentId, 작성자, 내용 | 1년 |
| **관리** | 신고 처리 | reportId, 처리자, 조치내용 | 영구 |

### 엔티티 설계

```java
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_created_at", columnList = "created_at")
})
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;           // nullable (비로그인 액션)

    @Enumerated(EnumType.STRING)
    private AuditAction action;    // LOGIN, LOGOUT, WITHDRAW, etc.

    private String ipAddress;
    private String userAgent;

    @Column(columnDefinition = "TEXT")
    private String detail;         // JSON 형태 추가 정보

    private LocalDateTime createdAt;
}
```

### 구현 방식 선택지

| 방식 | 장점 | 단점 |
|------|------|------|
| **AOP** | 비침투적, 횡단 관심사 분리 | 세밀한 제어 어려움 |
| **Event 기반** | 느슨한 결합, 비동기 가능 | 이벤트 누락 가능성 |
| **직접 호출** | 명확한 제어 | 코드 중복, 결합도 증가 |

**권장**: **Event 기반** (Spring ApplicationEvent) + 중요 액션은 직접 호출

### 작업 목록

- [ ] `AuditLog` 엔티티 생성
- [ ] `AuditAction` enum 정의
- [ ] `AuditLogRepository` 생성
- [ ] `AuditLogService` 구현
- [ ] `AuditEvent` 및 `AuditEventListener` 구현
- [ ] 주요 서비스에 이벤트 발행 추가
- [ ] (선택) 관리자 조회 API
- [ ] 테스트 작성

---

## Phase 3: 로그인 보안 강화

### 목적

- **Brute Force 방어**: 무차별 대입 공격으로 인한 계정 탈취 방지
- **크리덴셜 스터핑 방어**: 유출된 계정 정보 대입 공격 방지

### 구현 범위

| 기능 | 설명 | 우선순위 |
|------|------|---------|
| 로그인 실패 제한 | 5회 실패 시 15분 차단 | 높음 |
| 실패 알림 | 이메일/푸시로 알림 | 낮음 |
| 의심 로그인 감지 | 새 기기/위치 감지 | 낮음 |

### 동작 흐름

```
1. 로그인 시도
2. IP 기준 실패 횟수 조회 (Cache)
3. 차단 상태면 → 즉시 거부 (A013)
4. 로그인 검증
5. 실패 시 → 카운트 증가, 5회 도달 시 차단
6. 성공 시 → 카운트 초기화
```

### 기술 구현

```java
// Caffeine Cache 사용 예시
Cache<String, Integer> loginAttempts = Caffeine.newBuilder()
    .expireAfterWrite(15, TimeUnit.MINUTES)
    .maximumSize(10_000)
    .build();

public void checkLoginAttempt(String ip) {
    Integer attempts = loginAttempts.getIfPresent(ip);
    if (attempts != null && attempts >= 5) {
        throw AuthException.tooManyLoginAttempts();
    }
}

public void recordFailedAttempt(String ip) {
    loginAttempts.asMap().merge(ip, 1, Integer::sum);
}

public void clearAttempts(String ip) {
    loginAttempts.invalidate(ip);
}
```

### 에러 코드 추가

| 코드 | 메시지 | HTTP 상태 |
|------|--------|----------|
| A013 | 로그인 시도 횟수를 초과했습니다. 15분 후 다시 시도해주세요. | 429 |

### 작업 목록

- [ ] `LoginAttemptService` 구현
- [ ] Caffeine Cache 설정
- [ ] OAuth2 로그인 플로우에 적용
- [ ] `AuthErrorCode` A013 추가
- [ ] 테스트 작성

---

## Phase 4: 추가 보안 (선택)

### 4-1. 파일 업로드 보안

| 항목 | 현재 | 개선 |
|------|------|------|
| 확장자 검증 | 화이트리스트 | 유지 |
| 파일 크기 | 10MB 제한 | 유지 |
| Content-Type 검증 | 미구현 | Magic Number 검증 추가 |
| 악성코드 스캔 | 미구현 | ClamAV 연동 (선택) |

### 4-2. IP 블랙리스트

```java
@Entity
public class IpBlacklist {
    private String ipAddress;
    private String reason;
    private LocalDateTime blockedAt;
    private LocalDateTime expiresAt;  // null이면 영구 차단
}
```

- 수동 차단: 관리자가 직접 추가
- 자동 차단: Rate Limit 반복 위반 시

### 4-3. 신고 기능 연동

- 신고 N건 누적 시 콘텐츠 자동 숨김
- 신고 다수 사용자 모니터링 대상 등록

---

## 우선순위 및 로드맵

```
긴급: OAuth 토큰 코드 재사용 방지
├── 의존성: Caffeine Cache
├── 예상 작업량: 소
└── 효과: 높음 (토큰 탈취 방지)

Phase 1: Rate Limiting
├── 의존성: 없음
├── 예상 작업량: 중
└── 효과: 높음 (기본 보안)

Phase 2: Audit Log
├── 의존성: 없음
├── 예상 작업량: 중
└── 효과: 중간 (추적/분석)

Phase 3: 로그인 보안
├── 의존성: Phase 1 (Cache 설정 공유 가능)
├── 예상 작업량: 소
└── 효과: 중간 (계정 보호)

Phase 4: 추가 보안
├── 의존성: Phase 2 (로그 기반)
├── 예상 작업량: 대
└── 효과: 낮음 (고급 보안)
```

---

## 관련 문서

- [인증/권한 정책](./auth-policy.md)
- [보안 체크리스트](./security-checklist.md)
- [에러 코드](./error-codes.md)
