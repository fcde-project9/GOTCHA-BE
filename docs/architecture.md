# GOTCHA Backend 서버 아키텍처

## 전체 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                   클라이언트                                      │
│                          (Web Browser / Mobile App)                              │
└───────────────────────────────────┬─────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              외부 서비스 연동                                      │
├─────────────────┬─────────────────┬─────────────────┬───────────────────────────┤
│   Kakao OAuth   │   Google OAuth  │   Naver OAuth   │      Kakao Map API        │
│   (로그인)       │   (로그인)       │   (로그인)       │   (좌표→주소 변환)         │
└─────────────────┴─────────────────┴─────────────────┴───────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            Spring Boot Application                               │
│                               (Java 21 / 3.5.9)                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                         Security Layer                                    │   │
│  │  ┌────────────────┐  ┌────────────────┐  ┌─────────────────────────────┐ │   │
│  │  │  CORS Filter   │→│ JWT Auth Filter │→│ OAuth2 Authentication       │ │   │
│  │  └────────────────┘  └────────────────┘  └─────────────────────────────┘ │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                    │                                             │
│                                    ▼                                             │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                          Controller Layer                                 │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────────┐  │   │
│  │  │   Auth   │ │   User   │ │   Shop   │ │  Review  │ │  File Upload   │  │   │
│  │  │Controller│ │Controller│ │Controller│ │Controller│ │   Controller   │  │   │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └────────────────┘  │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                    │                                             │
│                                    ▼                                             │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                           Service Layer                                   │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────────┐  │   │
│  │  │   Auth   │ │   User   │ │   Shop   │ │  Review  │ │    Favorite    │  │   │
│  │  │ Service  │ │ Service  │ │ Service  │ │ Service  │ │    Service     │  │   │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └────────────────┘  │   │
│  │                                                                           │   │
│  │  ┌──────────────────────┐  ┌───────────────────────────────────────────┐ │   │
│  │  │  OAuth2 User Service │  │ External: KakaoMapClient, S3UploadService │ │   │
│  │  └──────────────────────┘  └───────────────────────────────────────────┘ │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                    │                                             │
│                                    ▼                                             │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                         Repository Layer                                  │   │
│  │              (Spring Data JPA + Hibernate + PostgreSQL)                   │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────────┐  │   │
│  │  │   User   │ │   Shop   │ │  Review  │ │ Favorite │ │ RefreshToken   │  │   │
│  │  │Repository│ │Repository│ │Repository│ │Repository│ │  Repository    │  │   │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └────────────────┘  │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
                                    │
          ┌─────────────────────────┼─────────────────────────┐
          │                         │                         │
          ▼                         ▼                         ▼
┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐
│     PostgreSQL      │  │       AWS S3        │  │   Kakao Map API     │
│        (RDS)        │  │   (파일 스토리지)     │  │   (좌표→주소 변환)   │
│                     │  │                     │  │                     │
│  - users            │  │  - 리뷰 이미지        │  │  coord2address API  │
│  - shops            │  │  - 프로필 이미지      │  │                     │
│  - reviews          │  │  - 가게 이미지        │  │                     │
│  - favorites        │  │                     │  │                     │
│  - refresh_tokens   │  │                     │  │                     │
│  - comments         │  │                     │  │                     │
└─────────────────────┘  └─────────────────────┘  └─────────────────────┘
```

---

## 도메인 구조 (패키지 아키텍처)

```
com.gotcha
│
├── _global/                          # 전역 설정
│   ├── config/                       # Spring 설정
│   │   ├── SecurityConfig           # JWT + OAuth2 + CORS
│   │   ├── S3Config                 # AWS S3 클라이언트
│   │   ├── JpaConfig                # JPA Auditing
│   │   └── OpenApiConfig            # Swagger
│   │
│   ├── common/                       # 공통 DTO
│   │   ├── ApiResponse<T>           # 통일된 응답 형식
│   │   └── PageResponse<T>          # 페이징 응답
│   │
│   ├── entity/
│   │   └── BaseTimeEntity           # createdAt, updatedAt
│   │
│   ├── exception/                    # 전역 예외
│   │   ├── GlobalExceptionHandler
│   │   └── ErrorCode interface
│   │
│   ├── external/                     # 외부 API
│   │   └── kakao/
│   │       └── KakaoMapClient       # 좌표→주소 변환
│   │
│   └── util/
│       └── SecurityUtil             # 현재 사용자 정보
│
└── domain/                           # 비즈니스 도메인
    │
    ├── auth/                         # 인증 도메인
    │   ├── controller/
    │   ├── service/
    │   │   ├── AuthService
    │   │   ├── CustomOAuth2UserService
    │   │   └── SocialUnlinkService
    │   ├── entity/
    │   │   └── RefreshToken
    │   ├── dto/
    │   └── exception/
    │
    ├── user/                         # 사용자 도메인
    │   ├── controller/
    │   ├── service/
    │   ├── repository/
    │   ├── entity/
    │   │   ├── User
    │   │   ├── UserPermission
    │   │   └── WithdrawalSurvey
    │   ├── dto/
    │   └── exception/
    │
    ├── shop/                         # 가게 도메인
    │   ├── controller/
    │   ├── service/
    │   ├── repository/
    │   ├── entity/
    │   │   ├── Shop
    │   │   └── ShopReport
    │   ├── dto/
    │   └── exception/
    │
    ├── review/                       # 리뷰 도메인
    │   ├── controller/
    │   ├── service/
    │   ├── repository/
    │   ├── entity/
    │   │   ├── Review
    │   │   ├── ReviewImage
    │   │   └── ReviewLike
    │   ├── dto/
    │   └── exception/
    │
    ├── favorite/                     # 찜 도메인
    │   ├── service/
    │   ├── repository/
    │   ├── entity/
    │   │   └── Favorite
    │   └── exception/
    │
    ├── file/                         # 파일 업로드
    │   ├── controller/
    │   └── service/
    │       └── S3FileUploadService
    │
    └── [Phase 2]                     # 미구현
        ├── chat/
        ├── post/
        └── inquiry/
```

---

## 배포 인프라 아키텍처 (AWS)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                    AWS Cloud                                     │
│                                  (ap-northeast-2)                                │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │                              Route 53 (DNS)                              │    │
│  │                          api.gotcha.com → EC2                            │    │
│  └───────────────────────────────────┬─────────────────────────────────────┘    │
│                                      │                                           │
│                                      ▼                                           │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │                              EC2 Instance                                │    │
│  │                         (Amazon Linux 2 / Docker)                        │    │
│  │                                                                          │    │
│  │   ┌──────────────────────────────────────────────────────────────────┐  │    │
│  │   │                    Docker Container                               │  │    │
│  │   │                                                                   │  │    │
│  │   │   ┌─────────────────────────────────────────────────────────┐    │  │    │
│  │   │   │               Spring Boot Application                    │    │  │    │
│  │   │   │                    (Port 8080)                           │    │  │    │
│  │   │   │                                                          │    │  │    │
│  │   │   │  - Java 21 Runtime (eclipse-temurin:21-jre-alpine)      │    │  │    │
│  │   │   │  - JWT Authentication                                    │    │  │    │
│  │   │   │  - OAuth2 Client (Kakao, Google, Naver)                 │    │  │    │
│  │   │   └─────────────────────────────────────────────────────────┘    │  │    │
│  │   │                                                                   │  │    │
│  │   └──────────────────────────────────────────────────────────────────┘  │    │
│  │                                                                          │    │
│  └────────────────────────────┬─────────────────────────────────────────────┘    │
│                               │                                                   │
│          ┌────────────────────┼────────────────────┐                             │
│          │                    │                    │                             │
│          ▼                    ▼                    ▼                             │
│  ┌───────────────┐   ┌───────────────┐   ┌───────────────┐                      │
│  │      RDS      │   │      S3       │   │      ECR      │                      │
│  │  PostgreSQL   │   │    Bucket     │   │   Registry    │                      │
│  │               │   │               │   │               │                      │
│  │  - users      │   │  - images/    │   │  gotcha-be-   │                      │
│  │  - shops      │   │    reviews/   │   │  prod:latest  │                      │
│  │  - reviews    │   │    profiles/  │   │               │                      │
│  │  - favorites  │   │    shops/     │   │               │                      │
│  │  - tokens     │   │               │   │               │                      │
│  └───────────────┘   └───────────────┘   └───────────────┘                      │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 인증 플로우 (OAuth2 + JWT)

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│  Client  │     │  Backend │     │  OAuth   │     │    DB    │
│  (Web)   │     │ (Spring) │     │ Provider │     │(Postgres)│
└────┬─────┘     └────┬─────┘     └────┬─────┘     └────┬─────┘
     │                │                │                │
     │ 1. GET /oauth2/authorize/kakao  │                │
     │───────────────>│                │                │
     │                │                │                │
     │ 2. Redirect to Kakao Login Page │                │
     │<───────────────│                │                │
     │                │                │                │
     │ 3. User Login at Kakao          │                │
     │────────────────────────────────>│                │
     │                │                │                │
     │ 4. Callback with Auth Code      │                │
     │<────────────────────────────────│                │
     │                │                │                │
     │ 5. Redirect to /api/auth/callback/kakao          │
     │───────────────>│                │                │
     │                │                │                │
     │                │ 6. Exchange Code for Token      │
     │                │───────────────>│                │
     │                │                │                │
     │                │ 7. User Info   │                │
     │                │<───────────────│                │
     │                │                │                │
     │                │ 8. Save/Update User             │
     │                │────────────────────────────────>│
     │                │                │                │
     │                │ 9. Generate JWT (Access + Refresh)
     │                │────────────────────────────────>│
     │                │                │                │
     │ 10. Redirect with Tokens        │                │
     │<───────────────│                │                │
     │                │                │                │
```

---

## 데이터베이스 스키마

### 주요 테이블

| 테이블 | 설명 |
|--------|------|
| `users` | 사용자 정보 (Soft delete) |
| `refresh_tokens` | 리프레시 토큰 저장 |
| `shops` | 가챠샵 정보 |
| `shop_reports` | 가게 제보 |
| `reviews` | 리뷰 |
| `review_images` | 리뷰 이미지 |
| `review_likes` | 리뷰 좋아요 |
| `favorites` | 찜 (user_id + shop_id 유니크) |
| `comments` | 댓글 |
| `user_permissions` | 사용자 권한 |
| `user_permission_histories` | 권한 변경 이력 |
| `withdrawal_surveys` | 탈퇴 설문 |

### Phase 2 테이블 (미구현)

| 테이블 | 설명 |
|--------|------|
| `posts` | 커뮤니티 포스트 |
| `post_comments` | 포스트 댓글 |
| `chats` | 채팅 메시지 |
| `chat_rooms` | 채팅방 |
| `inquiries` | 문의 |

---

## 기술 스택

| 영역 | 기술 |
|------|------|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.5.9 |
| **Build Tool** | Gradle 8 |
| **ORM** | Spring Data JPA + Hibernate |
| **Database** | PostgreSQL 14+ (AWS RDS) |
| **Security** | Spring Security + OAuth2 Client + JWT (JJWT) |
| **File Storage** | AWS S3 (SDK v2) |
| **Container** | Docker |
| **Registry** | AWS ECR |
| **API Documentation** | Springdoc OpenAPI (Swagger) |
| **External API** | Kakao Map API, OAuth2 Providers |
| **Caching** | Caffeine Cache |
| **Logging** | SLF4J + Logback |
| **Testing** | JUnit 5, Testcontainers |

---

## API 엔드포인트 구조

### 인증 (Auth)
| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/oauth2/authorize/{provider}` | OAuth2 로그인 시작 | X |
| POST | `/api/auth/reissue` | 토큰 재발급 | X |
| POST | `/api/auth/logout` | 로그아웃 | O |
| POST | `/api/auth/token` | 토큰 교환 | X |

### 사용자 (User)
| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/users/me` | 내 정보 조회 | O |
| PUT | `/api/users/me` | 내 정보 수정 | O |
| DELETE | `/api/users/me` | 회원 탈퇴 | O |

### 가게 (Shop)
| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/shops/map` | 지도 영역 내 가게 목록 | X |
| GET | `/api/shops/nearby` | 50m 내 가게 조회 | X |
| GET | `/api/shops/{shopId}` | 가게 상세 조회 | X |
| POST | `/api/shops/save` | 가게 제보 | X |
| POST | `/api/shops/{shopId}/favorite` | 찜 추가 | O |
| DELETE | `/api/shops/{shopId}/favorite` | 찜 삭제 | O |

### 리뷰 (Review)
| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/shops/{shopId}/reviews` | 리뷰 목록 | X |
| POST | `/api/shops/{shopId}/reviews` | 리뷰 작성 | O |
| PUT | `/api/shops/{shopId}/reviews/{reviewId}` | 리뷰 수정 | O |
| DELETE | `/api/shops/{shopId}/reviews/{reviewId}` | 리뷰 삭제 | O |

### 파일 (File)
| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/files/upload` | 파일 업로드 (최대 50MB) | X |

---

## 보안 정책

### 인증 필요 없음
- `OPTIONS /*` (CORS preflight)
- `POST/GET /api/auth/**` (인증)
- `/oauth2/**`, `/login/oauth2/**` (OAuth2)
- `GET /api/shops/**` (가게 조회)
- `POST /api/shops/save`, `/api/shops/report` (제보)
- `POST /api/files/**` (파일 업로드)
- Swagger UI

### 인증 필요
- `/api/users/**` (사용자 정보)
- `POST/DELETE /api/shops/{id}/favorite` (찜)
- `POST/PUT/DELETE /api/shops/{id}/comments/**` (댓글)
- `POST/PUT/DELETE /api/shops/{id}/reviews/**` (리뷰)
- `POST/DELETE /api/shops/reviews/{id}/like` (좋아요)

### Admin 전용
- `/api/admin/**`

---

## 환경 설정

### 환경별 프로필

| 프로필 | DDL 모드 | SQL 출력 | 용도 |
|--------|----------|----------|------|
| `local` | update | true | 로컬 개발 |
| `dev` | update | false | 개발 서버 |
| `prod` | validate | false | 운영 서버 |

### 필수 환경변수

```bash
# Database
DATABASE_URL=jdbc:postgresql://...
DATABASE_USERNAME=...
DATABASE_PASSWORD=...

# JWT
JWT_SECRET=...
JWT_ACCESS_TOKEN_VALIDITY=3600000
JWT_REFRESH_TOKEN_VALIDITY=1209600000

# OAuth2 - Kakao
KAKAO_CLIENT_ID=...
KAKAO_CLIENT_SECRET=...
KAKAO_REST_API_KEY=...
KAKAO_ADMIN_KEY=...

# OAuth2 - Google
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...

# OAuth2 - Naver
NAVER_CLIENT_ID=...
NAVER_CLIENT_SECRET=...

# AWS
AWS_ACCESS_KEY_ID=...
AWS_SECRET_ACCESS_KEY=...
AWS_REGION=ap-northeast-2
AWS_S3_BUCKET_NAME=...
AWS_S3_PREFIX=...

# Frontend
FRONTEND_BASE_URL=...
```

---

## 주요 설계 결정사항

| 항목 | 결정 | 이유 |
|------|------|------|
| Social 테이블 | users에 통합 | 1인 1소셜 계정, 단순화 |
| Location 테이블 | shops에 통합 | 1:1 관계, 조회 성능 |
| 좌표 타입 | Double | Kakao API 호환, 충분한 정밀도 |
| 영업시간 | JSON (jsonb) | 요일별 관리 가능, 유연성 |
| 가게 제보 | 즉시 등록 | MVP 단순화, 추후 승인제 검토 |
| 게스트 | 조회만 가능 | 찜/댓글은 로그인 필수 |
| Comment vs Review | 댓글(익명), 리뷰(인증) | 가벼운 댓글 vs 이미지 포함 리뷰 |
| Soft Delete | is_deleted 플래그 | 사용자 데이터 추적성 유지 |
