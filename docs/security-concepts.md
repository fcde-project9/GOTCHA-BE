# 보안 핵심 개념

## 1. Rate Limiting (요청 제한)

### 개념

API 요청 횟수를 제한하여 서버를 보호하는 기술

### Token Bucket 알고리즘

가장 널리 사용되는 Rate Limiting 알고리즘

```
┌─────────────────────────────────┐
│  버킷 (용량: 100)                │
│  ████████████░░░░░░░░ (현재 60) │
│                                 │
│  - 요청마다 토큰 1개 소모         │
│  - 일정 속도로 토큰 보충 (10/초)  │
│  - 토큰 없으면 요청 거부 (429)    │
└─────────────────────────────────┘
```

**동작 방식**:
1. 버킷에 일정량의 토큰이 있음
2. 요청이 들어오면 토큰 1개 소모
3. 토큰이 없으면 요청 거부 (429 응답)
4. 일정 속도로 토큰이 다시 채워짐

**장점**: 순간적인 버스트 트래픽 허용하면서도 평균 요청률 제한

### 왜 중요한가?

| 위협 | Rate Limiting 없을 때 | 있을 때 |
|------|---------------------|--------|
| DDoS 공격 | 서버 다운 | 공격자만 차단 |
| 크롤링/스크래핑 | 데이터 무단 수집 | 수집 속도 제한 |
| API 남용 | 클라우드 비용 폭발 | 비용 예측 가능 |
| Brute Force | 계정 탈취 가능 | 공격 무력화 |

### IP 기준 vs User 기준

| 기준 | 장점 | 단점 | 사용 시점 |
|------|------|------|----------|
| **IP** | 비로그인도 적용 | 공유 IP 문제 (회사, 카페) | 로그인 전 API |
| **User** | 정확한 식별 | 로그인 필요 | 로그인 후 API |
| **IP + User** | 정확도 높음 | 구현 복잡 | 민감한 API |

**공유 IP 문제**: 회사에서 100명이 같은 IP 사용 → IP 기준 제한 시 다 같이 차단됨

---

## 2. 공격 유형

### Brute Force (무차별 대입)

비밀번호를 무작위로 대입하는 공격

```
시도 1: password
시도 2: password1
시도 3: password123
시도 4: 123456
...
시도 10000: correctPassword (성공)
```

**방어**: 로그인 실패 횟수 제한 (5회 실패 → 15분 차단)

### Credential Stuffing (크리덴셜 스터핑)

다른 사이트에서 유출된 계정 정보를 대입하는 공격

```
1. 해커가 A 사이트 해킹 → 100만 개 계정 유출
2. 유출된 ID/PW로 B, C, D 사이트 로그인 시도
3. 같은 비밀번호 사용한 사용자 계정 탈취
```

**방어**:
- Rate Limiting
- 이상 로그인 감지 (새 기기, 새 위치)
- 2FA (2단계 인증)

### DDoS (분산 서비스 거부)

여러 컴퓨터에서 동시에 대량 요청을 보내 서버를 마비시키는 공격

```
봇넷 (감염된 PC 수천대)
    ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓
    서버 (처리 불가 → 다운)
```

**방어**:
- Rate Limiting (기본)
- CDN/WAF (Cloudflare, AWS WAF)
- 트래픽 분산 (로드밸런서)

### 소셜 로그인의 보안 이점

| 공격 | 일반 로그인 | 소셜 로그인 |
|------|-----------|-----------|
| Brute Force | 취약 | **면역** (비밀번호 없음) |
| Credential Stuffing | 취약 | **면역** (비밀번호 없음) |
| 피싱 | 취약 | 부분 방어 (OAuth 리다이렉트) |

---

## 3. HTTP 상태 코드 (보안 관련)

### 인증/인가 관련

| 코드 | 이름 | 의미 | 사용 시점 |
|------|------|------|----------|
| **401** | Unauthorized | 인증 필요 | 토큰 없음/만료 |
| **403** | Forbidden | 권한 없음 | 로그인했지만 권한 부족 |
| **429** | Too Many Requests | 요청 과다 | Rate Limit 초과 |

### 401 vs 403 차이

```
401 Unauthorized
- "너 누구야? 로그인부터 해"
- 토큰이 없거나 만료됨
- 해결: 로그인/토큰 재발급

403 Forbidden
- "누군지는 알겠는데, 권한이 없어"
- 로그인은 했지만 해당 리소스 접근 불가
- 해결: 권한 요청/다른 계정 사용
```

### 429 응답 시 포함할 헤더

```http
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 100        # 최대 허용 요청 수
X-RateLimit-Remaining: 0      # 남은 요청 수
X-RateLimit-Reset: 1704067260 # 리셋 시간 (Unix timestamp)
Retry-After: 60               # 재시도까지 대기 시간 (초)
```

---

## 4. Audit Log (감사 로그)

### 일반 로그 vs Audit Log

| 구분 | 일반 로그 | Audit Log |
|------|----------|-----------|
| **목적** | 디버깅, 에러 추적 | 누가 무엇을 했는지 기록 |
| **저장소** | 파일, ELK | DB (영구 보존) |
| **보존 기간** | 7~30일 | 1년 이상 |
| **예시** | `NullPointerException at...` | `유저 42가 리뷰 123 삭제` |
| **법적 가치** | 참고 자료 | 증거 자료 |

### 기록해야 할 이벤트

**필수 (법적/보안)**:
- 로그인/로그아웃
- 회원가입/탈퇴
- 권한 변경
- 개인정보 조회/수정

**권장 (운영)**:
- 콘텐츠 삭제
- 신고 처리
- 관리자 액션

### 저장해야 할 정보

```json
{
  "userId": 42,
  "action": "DELETE_REVIEW",
  "targetId": 123,
  "ipAddress": "123.45.67.89",
  "userAgent": "Mozilla/5.0...",
  "detail": {
    "reviewContent": "삭제된 리뷰 내용 요약",
    "reason": "사용자 요청"
  },
  "createdAt": "2025-01-19T15:30:00"
}
```

---

## 5. 캐시 (Cache)

### 로컬 캐시 vs 분산 캐시

```
로컬 캐시 (Caffeine)
┌─────────┐
│ Server  │ ← 캐시가 서버 메모리에 저장
│ [Cache] │
└─────────┘

분산 캐시 (Redis)
┌─────────┐     ┌─────────┐
│ Server1 │ ←→  │  Redis  │ ← 별도 서버에 캐시 저장
└─────────┘     └─────────┘
      ↑              ↑
┌─────────┐          │
│ Server2 │ ←────────┘
└─────────┘
```

### 비교

| 항목 | Caffeine (로컬) | Redis (분산) |
|------|----------------|--------------|
| 속도 | 매우 빠름 (ns) | 빠름 (ms) |
| 서버 간 공유 | 불가 | 가능 |
| 재시작 시 | 초기화됨 | 유지됨 |
| 인프라 | 불필요 | Redis 서버 필요 |
| 적합 상황 | 단일 서버 | 다중 서버 |

### GOTCHA 적용

현재 단일 서버 구조 → **Caffeine 권장**

```java
// Caffeine 설정 예시
Cache<String, Integer> loginAttempts = Caffeine.newBuilder()
    .expireAfterWrite(15, TimeUnit.MINUTES)  // 15분 후 만료
    .maximumSize(10_000)                      // 최대 1만 개 항목
    .build();
```

---

## 6. AOP (Aspect-Oriented Programming)

### 개념

횡단 관심사(여러 곳에서 반복되는 로직)를 분리하는 프로그래밍 기법

### 횡단 관심사 예시

```
UserService          ReviewService        ShopService
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ 로깅         │    │ 로깅         │    │ 로깅         │  ← 반복!
│ 권한 체크    │    │ 권한 체크    │    │ 권한 체크    │  ← 반복!
│ 트랜잭션     │    │ 트랜잭션     │    │ 트랜잭션     │  ← 반복!
│──────────────│    │──────────────│    │──────────────│
│ 비즈니스로직  │    │ 비즈니스로직  │    │ 비즈니스로직  │
└──────────────┘    └──────────────┘    └──────────────┘
```

### AOP 적용 후

```
┌─────────────────────────────────────────┐
│            Aspect (공통 로직)            │
│  - 로깅                                  │
│  - 권한 체크                             │
│  - 트랜잭션                              │
└─────────────────────────────────────────┘
              ↓ 자동 적용
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ UserService  │  │ReviewService │  │ ShopService  │
│ 비즈니스로직  │  │ 비즈니스로직  │  │ 비즈니스로직  │
└──────────────┘  └──────────────┘  └──────────────┘
```

### Audit Log에 AOP 적용

```java
// 1. 어노테이션 정의
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    AuditAction action();
}

// 2. Aspect 정의
@Aspect
@Component
public class AuditAspect {

    @AfterReturning("@annotation(auditable)")
    public void logAudit(JoinPoint jp, Auditable auditable) {
        // 자동으로 감사 로그 기록
        auditLogService.log(auditable.action(), ...);
    }
}

// 3. 사용
@Auditable(action = AuditAction.DELETE_REVIEW)
public void deleteReview(Long reviewId) {
    // 비즈니스 로직만 작성
    // 감사 로그는 AOP가 자동 처리
}
```

**장점**: 비즈니스 로직에 로깅 코드가 섞이지 않음

---

## 7. Spring Security 필터 체인

### 요청 처리 흐름

```
HTTP 요청
    ↓
┌─────────────────────────┐
│ SecurityFilterChain     │
│ ┌─────────────────────┐ │
│ │ CorsFilter          │ │ ← CORS 처리
│ ├─────────────────────┤ │
│ │ JwtAuthFilter       │ │ ← JWT 토큰 검증
│ ├─────────────────────┤ │
│ │ RateLimitFilter     │ │ ← Rate Limiting (추가 예정)
│ ├─────────────────────┤ │
│ │ AuthorizationFilter │ │ ← 권한 체크
│ └─────────────────────┘ │
└─────────────────────────┘
    ↓
Controller
```

### 필터 순서가 중요한 이유

```
잘못된 순서: AuthFilter → RateLimitFilter
- 인증 실패해도 Rate Limit 카운트 증가
- 공격자가 무한 요청 가능

올바른 순서: RateLimitFilter → AuthFilter
- Rate Limit 먼저 체크
- 초과 시 인증 로직 실행 안 함 (서버 부하 감소)
```

---

## 관련 문서

- [보안 강화 로드맵](./security-roadmap.md)
- [보안 체크리스트](./security-checklist.md)
- [인증/권한 정책](./auth-policy.md)
