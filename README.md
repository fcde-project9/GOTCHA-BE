# GOTCHA! Backend – 가챠샵 지도 서비스

가챠샵(Gacha Shop)을 지도 기반으로 탐색하고, 리스트 및 상세 페이지를 통해 매장 정보를 확인할 수 있는 모바일 웹 서비스의 백엔드 API 서버입니다.

🔗 서비스 주소: [https://gotcha.it.com](https://gotcha.it.com)

---

## 주요 기능

- 🗺️ 카카오맵 기반 가챠샵 위치 탐색 API
- 🏪 매장 상세 정보 및 리뷰 CRUD
- ⭐ 즐겨찾기 기능
- 📝 새 업체 제보
- 🔐 OAuth2 소셜 로그인 (카카오, 구글, 네이버)
- 📷 이미지 업로드 (AWS S3)

### 예정 기능

- 📲 앱 출시 지원 (iOS / Android)
- 🏠 사장님 페이지 API

---

## Tech Stack

- **Framework**: Spring Boot 3.5.9
- **Language**: Java 21
- **ORM**: Spring Data JPA
- **Database**: PostgreSQL
- **Security**: Spring Security + OAuth2 Client
- **Storage**: AWS S3
- **Documentation**: Swagger (springdoc-openapi)
- **Infrastructure**: AWS EC2

🔗 프론트엔드 깃헙 레포지토리: [GOTCHA-FE](https://github.com/fcde-project9/GOTCHA-FE)

---

## CI/CD

GitHub Actions를 사용한 자동 배포 파이프라인

| 브랜치 | 환경       | 설명               |
| ------ | ---------- | ------------------ |
| `dev`  | Development | 개발 서버          |
| `main` | Production  | 운영 서버          |

**배포 프로세스**: 코드 푸시 → 빌드 → 테스트 → EC2 배포

---

## 개발 환경 설정

### 필수 요구사항

- Java 21 이상
- Gradle 8.x
- PostgreSQL 15+

### 빌드 및 실행

1. 의존성 설치 및 빌드

   ```bash
   ./gradlew build
   ```

2. 환경 변수 설정

   ```bash
   cp .env.example .env
   # .env 파일을 열어 실제 값 입력
   ```

3. 로컬 서버 실행

   ```bash
   ./gradlew bootRun
   ```

   API 서버: [http://localhost:8080](http://localhost:8080)
   Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

4. 테스트 실행

   ```bash
   ./gradlew test
   ```

---

## 프로젝트 폴더 구조

```
src/main/java/com/gotcha/
├── _global/                    # 전역 설정 및 공통 모듈
│   ├── common/                 # 공통 응답 객체 (ApiResponse, PageResponse)
│   ├── config/                 # 설정 (Security, JPA, OpenAPI 등)
│   ├── entity/                 # 공통 엔티티 (BaseTimeEntity)
│   ├── exception/              # 전역 예외 처리
│   ├── external/               # 외부 API 클라이언트 (카카오맵)
│   └── util/                   # 유틸리티 클래스
│
└── domain/                     # 도메인별 모듈
    ├── auth/                   # 인증 (OAuth2, JWT)
    ├── shop/                   # 매장 관리
    ├── review/                 # 리뷰 관리
    ├── favorite/               # 즐겨찾기
    ├── file/                   # 파일 업로드
    └── ...                     # 기타 도메인
```

---

## API 문서

Swagger UI를 통해 API 문서를 확인할 수 있습니다.

- **로컬**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **개발 서버**: [https://api-dev.gotcha.it.com/swagger-ui.html](https://api-dev.gotcha.it.com/swagger-ui.html)

### 주요 API 엔드포인트

| 도메인   | 엔드포인트          | 설명                    |
| -------- | ------------------- | ----------------------- |
| Auth     | `POST /api/auth/*`  | 소셜 로그인 및 토큰 관리 |
| Shop     | `GET /api/shops/*`  | 매장 조회 및 검색        |
| Review   | `POST /api/reviews` | 리뷰 작성/수정/삭제      |
| Favorite | `POST /api/favorites` | 즐겨찾기 등록/해제     |
| File     | `POST /api/files`   | 이미지 업로드           |

---

## 문서 활용 (docs 폴더)

프로젝트의 `docs/` 폴더에는 개발 시 참조하는 표준 문서가 포함되어 있습니다.

### 문서 목록

| 파일                        | 설명                          |
| --------------------------- | ----------------------------- |
| `entity-design.md`          | Entity 구조 설계              |
| `api-spec.md`               | API 상세 명세 (Request/Response) |
| `api-design.md`             | API 엔드포인트 개요           |
| `business-rules.md`         | 비즈니스 규칙                 |
| `auth-policy.md`            | 인증/권한 정책                |
| `error-codes.md`            | 에러 코드 정의                |
| `coding-patterns.md`        | 코딩 패턴 가이드              |
| `file-upload-guide.md`      | 이미지 업로드 가이드          |
| `repository-edge-cases.md`  | Repository 엣지 케이스        |

### 스킬 문서 (자동화용)

| 파일                        | 설명                          |
| --------------------------- | ----------------------------- |
| `skills/gotcha-api.md`      | API 개발 패턴                 |
| `skills/gotcha-entity.md`   | Entity 작성 규칙              |
| `skills/gotcha-test.md`     | 테스트 작성 패턴              |
| `skills/gotcha-config.md`   | 설정/yml 관리 규칙            |

---

## Git 브랜치 전략

| 브랜치 타입     | 네이밍          | 설명                |
| --------------- | --------------- | ------------------- |
| Feature         | `feature/*`     | 새 기능 개발        |
| Bug Fix         | `fix/*`         | 버그 수정           |
| Refactor        | `refactor/*`    | 코드 리팩토링       |
| Documentation   | `docs/*`        | 문서 작업           |

**PR 대상 브랜치**: `dev`
**PR 전 필수**: `./gradlew spotlessApply`
