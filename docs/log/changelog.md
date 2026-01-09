# 문서 변경 로그

> 문서 추가/수정 시 이 파일에 기록합니다.

---

## 2026-01-09

### 추가
- `src/main/java/com/gotcha/domain/user/dto/UpdateProfileImageRequest.java` - 프로필 이미지 변경 Request DTO (GCS URL 검증)
- `application-local.yml` - user.default-profile-image-url 기본값 추가 (local 환경)
- `CustomOAuth2UserService.java` - 신규 가입 시 기본 프로필 이미지 자동 설정
- `UserService.java` - updateProfileImage() 메서드 (프로필 이미지 변경, 기존 커스텀 이미지 GCS 삭제)
- `UserService.java` - deleteProfileImage() 메서드 (프로필 이미지 삭제, 기본 이미지로 복구)
- `UserController.java` - PATCH /users/me/profile-image 엔드포인트 추가 (프로필 이미지 변경)
- `UserController.java` - DELETE /users/me/profile-image 엔드포인트 추가 (프로필 이미지 삭제)
- `docs/api-spec.md` - PATCH /users/me/profile-image API 명세 추가
- `docs/api-spec.md` - DELETE /users/me/profile-image API 명세 추가
- `.github/workflows/cicd-dev.yml` - USER_DEFAULT_PROFILE_IMAGE_URL_DEV 환경변수 추가

### 수정
- `application.yml` - user.default-profile-image-url 설정 추가 (환경변수 필수)
- `docs/entity-design.md` - withdrawal_surveys 테이블 구조 업데이트
  - 변경: reason (단일 Enum) → reasons (JSON 배열, 복수 선택 가능)
  - 변경: Enum 값 업데이트 (LOW_USAGE, INSUFFICIENT_INFO, INACCURATE_INFO, PRIVACY_CONCERN, HAS_OTHER_ACCOUNT, OTHER)
  - 추가: WithdrawalReason Enum 상세 설명
- `docs/entity-design.md` - users 테이블 업데이트
  - 추가: email 필드
  - 추가: 탈퇴 처리 설명 (soft delete, 개인정보 마스킹, 소셜 연동 해제, 재가입 허용)
- `docs/api-spec.md` - DELETE /users/me API 수정
  - 변경: reason (단일) → reasons (배열, 복수 선택)
  - 변경: Enum 값 현재 구현과 일치하도록 수정
  - 추가: 탈퇴 시 삭제되는 데이터 목록, 재가입 가능 안내
- `docs/business-rules.md` - 회원 탈퇴 섹션 전면 수정
  - 변경: 탈퇴 시 삭제되는 데이터 명시 (찜, 리뷰, 댓글 물리 삭제)
  - 추가: 소셜 연동 해제, 재가입 가능
  - 변경: 탈퇴 사유 Enum 현재 구현과 일치하도록 수정
- `docs/business-rules.md` - 리뷰 섹션 수정
  - 변경: 이미지 "1장" → "최대 10장"

---

## 2026-01-08

### 추가
- `src/main/java/com/gotcha/domain/user/entity/PermissionType.java` - 권한 타입 Enum (LOCATION, CAMERA, ALBUM)
- `src/main/java/com/gotcha/domain/user/entity/UserPermission.java` - 사용자 권한 동의 상태 Entity (일반 PK + UNIQUE 제약)
- `src/main/java/com/gotcha/domain/user/entity/UserPermissionHistory.java` - 사용자 권한 변경 이력 Entity (deviceInfo만 저장)
- `src/main/java/com/gotcha/domain/user/repository/UserPermissionRepository.java` - UserPermission Repository
- `src/main/java/com/gotcha/domain/user/repository/UserPermissionHistoryRepository.java` - UserPermissionHistory Repository
- `src/main/java/com/gotcha/domain/user/service/UserPermissionService.java` - 권한 확인 및 업데이트 Service
- `src/main/java/com/gotcha/domain/user/controller/UserPermissionController.java` - 권한 확인/업데이트 API
- `src/main/java/com/gotcha/domain/user/dto/UpdatePermissionRequest.java` - 권한 업데이트 Request DTO
- `src/main/java/com/gotcha/domain/user/dto/PermissionResponse.java` - 권한 응답 DTO
- `docs/entity-design.md` - review_images 테이블 추가 (리뷰 다중 이미지 지원)
- `docs/entity-design.md` - refresh_tokens 테이블 추가
- `docs/api-spec.md` - PUT /shops/{shopId}/reviews/{reviewId}, DELETE /shops/{shopId}/reviews/{reviewId} API 추가
- `docs/api-spec.md` - POST /auth/reissue API 명세 추가
- `docs/api-design.md` - POST /auth/reissue 엔드포인트 추가
- `docs/file-upload-guide.md` - 이미지 업로드 가이드 (프론트/백엔드 개발자용, GCS 사용법, 패턴 예시)
- `User.java` - isDeleted 필드 추가, delete() 메서드 (개인정보 마스킹 포함)
- `UserErrorCode.java` - ALREADY_DELETED (U005) 에러코드 추가
- `UserException.java` - alreadyDeleted() 팩토리 메서드 추가
- `UserService.java` - withdraw() 메서드 (설문 저장, 찜/리뷰/댓글 삭제, GCS 이미지 삭제, RefreshToken 삭제, soft delete)
- `UserController.java` - DELETE /users/me 엔드포인트 추가 (탈퇴 설문 포함)
- `AuthErrorCode.java` - USER_DELETED (A012) 에러코드 추가
- `AuthException.java` - userDeleted() 팩토리 메서드 추가
- `FavoriteRepository.java` - deleteByUserId() 메서드 추가
- `ReviewRepository.java` - findAllByUserId(), deleteByUserId() 메서드 추가
- `ReviewImageRepository.java` - deleteAllByReviewIdIn() 메서드 추가
- `CommentRepository.java` - deleteByUserId() 메서드 추가
- `SecurityUtil.java` - 탈퇴 사용자 API 접근 차단 로직 추가
- `CustomOAuth2UserService.java` - 탈퇴 사용자 로그인 차단 로직 추가

### 수정
- `docs/api-spec.md` - DELETE /users/me 수정 (탈퇴 설문 통합, POST /users/me/withdrawal-survey 제거)
- `docs/error-codes.md` - U005 에러코드 추가 (이미 탈퇴한 사용자)
- `docs/error-codes.md` - A012 에러코드 추가 (탈퇴 사용자 접근 차단)
- `docs/entity-design.md` - user_permissions, user_permission_histories 테이블 설계 추가 (ipAddress 제거, deviceInfo만 유지)
- `docs/auth-policy.md` - JWT 토큰 정책 업데이트
  - 변경: Access Token 1시간 → 15분, Refresh Token 14일 → 7일
  - 추가: Refresh Token 관리 정책 (DB 저장, 로그아웃 시 삭제)
  - 변경: 토큰 재발급 엔드포인트 /auth/refresh → /auth/reissue
  - 추가: 로그아웃 동작 설명
- `docs/entity-design.md` - reviews.image_url 필드 삭제 (review_images로 완전 이동)
- `docs/api-spec.md` - 리뷰 API imageUrl → imageUrls 배열로 변경 (최대 10개)
- `docs/api-spec.md` - 파일 업로드 API 스펙 업데이트 (POST /files/upload, folder 파라미터, 실제 구현과 일치)
- `docs/error-codes.md` - 인증 에러 코드 추가 (A007-A011)
- `docs/error-codes.md` - R003 설명 수정 (수정/삭제 통합), R005 추가 (이미지 개수 초과)
- `CLAUDE.md` - 개발 가이드 및 문서 목록에 file-upload-guide.md 추가
- `ReviewService.java` - updateReview() 메서드 개선 (GCS 안전 삭제: 삭제된 이미지만 GCS에서 제거)
- `ReviewService.java` - updateReview(), deleteReview() shopId 검증 추가 (보안 강화)
- `ReviewController.java` - updateReview(), deleteReview() shopId 파라미터 전달
- `CreateReviewRequest.java`, `UpdateReviewRequest.java` - imageUrls 스키마 설명 명확화 (null/빈 리스트 정책)
- `Review.java` - imageUrl 필드 및 updateImage() 메서드 삭제

---

## 2026-01-07

### 수정
- `CLAUDE.md` - 태스크 완료 조건 섹션 추가 (엣지 케이스 테스트, Confluence 문서화 필수)

---

## 2026-01-06

### 수정
- `docs/auth-policy.md` - OAuth2 소셜 로그인 플로우, 지원 provider, 관련 클래스, 환경변수 섹션 추가
- `docs/api-spec.md` - Figma v1fix 화면 기반 API 명세 전면 수정
  - 추가: POST /auth/logout, GET /auth/nickname/check, GET /shops/search, GET /shops/nearby, GET /users/me/shops, DELETE /users/me, POST /users/me/withdrawal-survey, POST /images
  - 변경: POST /shops/report (mainImageUrl 필수, address 제거, openTime 단일 시간대로 변경)
  - 변경: GET /shops/{shopId} openTime 형식 변경 (JSON → String)
- `docs/api-design.md` - 신규 API 엔드포인트 목록 추가
- `docs/business-rules.md` - Figma v1fix 화면 기반 비즈니스 규칙 수정
  - 변경: 닉네임 숫자 범위 0-9999 (코드와 일치)
  - 변경: 영업시간 형식 단일 시간대 (HH:mm-HH:mm)
  - 추가: 회원 탈퇴 규칙, 이미지 업로드 규칙
- `docs/entity-design.md` - Entity 설계 수정
  - 변경: shops.open_time 타입 JSON → String
  - 추가: users.is_deleted (soft delete)
  - 추가: withdrawal_surveys 테이블

---

## 2026-01-05

### 추가
- `docs/skills/gotcha-api.md` - API 개발 패턴 스킬
- `docs/skills/gotcha-entity.md` - Entity 작성 규칙 스킬
- `docs/skills/gotcha-test.md` - 테스트 작성 패턴 스킬
- `docs/skills/gotcha-config.md` - 설정/yml 환경변수화 규칙 스킬
- `docs/log/changelog.md` - 문서 변경 로그 (이 파일)

### 수정
- `CLAUDE.md` - 스킬 문서 목록 및 설정 규칙 섹션 추가
- `src/main/resources/application-*.yml` - CORS 설정 환경변수화

---

## 형식 가이드

```markdown
## YYYY-MM-DD

### 추가
- `파일경로` - 설명

### 수정
- `파일경로` - 변경 내용

### 삭제
- `파일경로` - 삭제 사유
```
