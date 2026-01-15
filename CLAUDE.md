# GOTCHA Backend

가챠샵(뽑기) 위치 정보 제공 웹 어플리케이션

## 기술 스택

- Java 21 / Spring Boot 3.5.9 / Gradle
- Spring Data JPA / PostgreSQL
- Spring Security OAuth2 (카카오, 구글, 네이버, 애플)
- 카카오 지도 API

## 빌드 & 실행

```bash
./gradlew build      # 빌드
./gradlew bootRun    # 실행
./gradlew test       # 테스트
```

## 패키지 구조

```
com.gotcha
├── _global/          # config, exception, common
└── domain/{feature}/ # controller, service, repository, entity, dto
```

## 코딩 컨벤션

- 들여쓰기: 4 spaces / 줄 길이: 120자
- 클래스: PascalCase / 메서드·변수: camelCase
- DTO: `*Request`, `*Response` 접미사
- 패턴: `docs/coding-patterns.md` 참조

## Git

- 브랜치: `feature/`, `fix/`, `refactor/`
- PR 대상: `dev`
- PR 전: `./gradlew spotlessApply`

## 개발 가이드

**코드 작성 전 관련 문서를 반드시 먼저 읽을 것:**

| 작업 | 참조 문서 |
|------|----------|
| API 구현 | `docs/api-spec.md`, `docs/error-codes.md`, `docs/skills/gotcha-api.md` |
| Entity 추가/수정 | `docs/entity-design.md`, `docs/skills/gotcha-entity.md` |
| 인증/권한 구현 | `docs/auth-policy.md` |
| 비즈니스 로직 | `docs/business-rules.md` |
| 이미지 업로드 구현 | `docs/file-upload-guide.md` |
| 설계 확인 | `docs/decisions.md`, `docs/flow.md` |
| Repository/테스트 | `docs/repository-edge-cases.md`, `docs/skills/gotcha-test.md` |
| 설정/yml 변경 | `docs/skills/gotcha-config.md` |

## 설정 규칙

- **환경변수 필수**: yml에 URL, 도메인, 키 등 하드코딩 금지
- **형식**: `${ENV_VAR}` 또는 `${ENV_VAR:default}` (local만 기본값 허용)
- **상세**: `docs/skills/gotcha-config.md` 참조

## 문서 목록

| 문서 | 설명 |
|------|------|
| `docs/entity-design.md` | Entity 구조 (V1 + V2) |
| `docs/api-spec.md` | API 상세 명세 (Request/Response) |
| `docs/api-design.md` | API 엔드포인트 개요 |
| `docs/business-rules.md` | 비즈니스 규칙 |
| `docs/auth-policy.md` | 인증/권한 정책 |
| `docs/error-codes.md` | 에러 코드 정의 |
| `docs/flow.md` | 화면/검색 플로우 |
| `docs/decisions.md` | 설계 결정 사항 |
| `docs/coding-patterns.md` | 코딩 패턴 |
| `docs/repository-edge-cases.md` | Repository 엣지 케이스 |
| `docs/file-upload-guide.md` | 이미지 업로드 가이드 (GCS 사용법) |

## 스킬 문서 (자동화용)

| 스킬 | 설명 |
|------|------|
| `docs/skills/gotcha-api.md` | API 개발 패턴 (Controller/Service/DTO) |
| `docs/skills/gotcha-entity.md` | Entity 작성 규칙 (Builder, BaseTimeEntity) |
| `docs/skills/gotcha-test.md` | 테스트 작성 패턴 (Repository/Service/Controller) |
| `docs/skills/gotcha-config.md` | 설정/yml 관리 규칙 (환경변수화) |

## 태스크 완료 조건

모든 기능 개발 태스크는 다음 조건을 충족해야 합니다:

### 1. 엣지 케이스 테스트 (필수)
- 정상 케이스 테스트
- 경계값 테스트 (최소/최대/빈값)
- 예외 상황 테스트 (null, 잘못된 입력 등)
- 동시성 이슈 가능성 검토
- 의존성 실패 시나리오 (외부 API, DB 등)

### 2. 빌드 & 테스트
- `./gradlew build` 성공
- `./gradlew test` 모든 테스트 통과

### 3. Confluence 문서화 (필수)
작업 완료 시 Confluence에 상세 문서 작성:
- 구현 개요
- 변경된 파일 목록
- API 변경 사항 (있는 경우)
- 테스트 케이스 목록
- 엣지 케이스 처리 방법

## 문서 변경 로그

문서 추가/수정/삭제 시 `docs/log/changelog.md`에 기록
