# 문서 변경 로그

> 문서 추가/수정 시 이 파일에 기록합니다.

---

## 2026-02-02

### 추가
- `docs/architecture.md` - 백엔드 서버 아키텍처 문서 신규 작성
  - 추가: 전체 시스템 아키텍처 다이어그램 (클라이언트 → 외부 서비스 → Spring Boot → DB/S3)
  - 추가: 도메인 구조 (패키지 아키텍처) - _global, domain별 구조
  - 추가: 배포 인프라 아키텍처 (AWS: Route53, EC2, RDS, S3, ECR)
  - 추가: 인증 플로우 다이어그램 (OAuth2 + JWT)
  - 추가: 데이터베이스 스키마 (주요 테이블, Phase 2 테이블)
  - 추가: 기술 스택 상세 목록
  - 추가: API 엔드포인트 구조 (Auth, User, Shop, Review, File)
  - 추가: 보안 정책 (인증 필요/불필요 엔드포인트)
  - 추가: 환경 설정 (프로필별 설정, 필수 환경변수)
  - 추가: 주요 설계 결정사항

### 수정
- `README.md` - 프로젝트 README 전면 개편
  - 추가: 주요 기능 섹션 (API 기능, 예정 기능)
  - 추가: Tech Stack 상세화 (Framework, Language, ORM, Database, Security, Storage, Documentation, Infrastructure)
  - 추가: CI/CD 파이프라인 설명 (GitHub Actions, dev/main 브랜치별 환경)
  - 추가: 개발 환경 설정 가이드 (필수 요구사항, 빌드 및 실행 방법)
  - 추가: 프로젝트 폴더 구조 (_global, domain 모듈별 설명)
  - 추가: API 문서 섹션 (Swagger UI 링크, 주요 API 엔드포인트 테이블)
  - 추가: docs 폴더 문서 목록 (문서 및 스킬 문서)
  - 추가: Git 브랜치 전략 (feature, fix, refactor, docs)
  - 추가: 환경 변수 목록 (.env.example 참조)
  - 변경: 프론트엔드 레포지토리 링크 추가

---

## 2026-01-27

### 수정
- `src/main/resources/application-local.yml` - 기본 이미지 URL 환경변수에 기본값 추가
  - 변경: USER_DEFAULT_PROFILE_IMAGE_URL에 기본값 추가 (local 환경)
  - 변경: SHOP_DEFAULT_IMAGE_URL에 기본값 추가 (local 환경)
- `docs/skills/gotcha-config.md` - application-local.yml 예시 업데이트
  - 추가: user.default-profile-image-url 설정 (기본값 포함)
  - 추가: shop.default-image-url 설정 (기본값 포함)
- `docs/skills/gotcha-config.md` - .env 파일 예시 업데이트
  - 추가: USER_DEFAULT_PROFILE_IMAGE_URL 환경변수
  - 추가: SHOP_DEFAULT_IMAGE_URL 환경변수

---

## 2026-01-21

### 수정
- `src/main/resources/application.yml` - 파일 업로드 크기 제한 증가
  - 변경: max-file-size 20MB → 50MB (아이폰 고화질 사진 지원)
  - 변경: max-request-size 20MB → 50MB
- `src/main/java/com/gotcha/domain/file/service/S3FileUploadService.java` - 파일 크기 검증 로직 수정
  - 변경: MAX_FILE_SIZE 20MB → 50MB
- `docs/file-upload-guide.md` - 파일 크기 제한 문서 업데이트
  - 변경: 최대 파일 크기 20MB → 50MB (2곳)
- `docs/aws-setup-guide.md` - Nginx 설정에 파일 업로드 크기 제한 추가
  - 추가: client_max_body_size 50M 설정 (413 Request Entity Too Large 에러 방지)

---

## 2026-01-18

### 수정
- `src/main/java/com/gotcha/domain/shop/service/ShopService.java` - 영업시간 구분자 다중 지원
  - 추가: splitTimeRange() 메서드 - 영업시간 범위 문자열 분리
  - 변경: `-`, ` - `, `~` 구분자 모두 지원 (정규식 `\s*[-~]\s*`)
  - 예시: "10:00-22:00", "00:00 - 24:00", "08:00~22:30" 모두 파싱 가능
- `docs/api-spec.md` - openTime 형식 설명 업데이트
  - 변경: HH:mm-HH:mm 또는 HH:mm~HH:mm 형식 지원 명시
- `docs/entity-design.md` - open_time 필드 설명 업데이트
  - 변경: 구분자 `-` 또는 `~` 지원 명시
- `docs/business-rules.md` - 영업시간 형식 설명 업데이트
  - 변경: 구분자 `-` 또는 `~` 지원, 공백 포함 가능 명시
  - 추가: openTime/openStatus 매핑 예시 표

---

## 2026-01-17

### 수정
- `src/main/java/com/gotcha/domain/shop/service/ShopService.java` - getOpenStatus() 휴무 처리 로직 수정
  - 변경: null과 빈 문자열 처리 순서 변경 (빈 문자열 우선 체크)
  - 변경: null이면 빈 문자열("") 반환, 빈 문자열("")이면 "휴무" 반환
  - 의미: 빈 문자열은 휴무일, null은 영업시간 정보 없음
- `docs/business-rules.md` - 영업 상태 판단 로직 업데이트
  - 변경: 휴무일 표시 방법 (null → 빈 문자열)
  - 변경: null은 영업시간 정보 없음을 의미하도록 변경
  - 변경: openTime 예시 업데이트 (Tue:null → Tue:"")
- `docs/api-spec.md` - openTime 예시 업데이트
  - 변경: GET /shops/map Response 예시의 openTime (Tue:null → Tue:"")
  - 변경: GET /shops/{shopId} Response 예시의 openTime (Tue:null → Tue:"")
- `docs/entity-design.md` - shops.open_time 필드 설명 업데이트
  - 변경: 휴무일 표시 방법 명시 (빈 문자열(""), null은 정보 없음)
  - 변경: 예시 업데이트 (Tue:null → Tue:"")

---

## 2026-01-15

### 추가
- `src/main/java/com/gotcha/domain/review/repository/ReviewImageRepository.java` - 가게별 리뷰 이미지 전체 조회 메서드
  - 추가: findAllByShopIdOrderByCreatedAtDesc() - 리뷰 생성일시 기준 최신순 정렬
- `src/main/java/com/gotcha/domain/review/dto/ReviewImageListResponse.java` - 리뷰 이미지 목록 Response DTO
- `src/main/java/com/gotcha/domain/review/service/ReviewService.java` - 가게 리뷰 이미지 조회 기능
  - 추가: getShopReviewImages() - 가게의 모든 리뷰 이미지 조회 (최신순)
- `src/main/java/com/gotcha/domain/review/controller/ReviewController.java` - 리뷰 이미지 조회 API
  - 추가: GET /api/shops/{shopId}/reviews/images - 가게 리뷰 이미지 전체 조회
- `src/test/java/com/gotcha/domain/review/repository/ReviewImageRepositoryTest.java` - 리뷰 이미지 Repository 테스트

### 수정
- `src/main/java/com/gotcha/domain/shop/service/ShopService.java` - 영업 상태 로직 개편
  - 변경: isOpenNow() 제거, getOpenStatus() 추가 (Boolean → String)
  - 변경: openStatus 4가지 상태 지원 ("영업 중", "영업 종료", "휴무", "")
  - 변경: openTime JSON 형식의 시간 구분자 ~ → - (예: "10:00~22:00" → "10:00-22:00")
  - 변경: 한국 시간(Asia/Seoul) 기준으로 영업 상태 판단
  - 변경: 요일별 영업시간 처리 (Mon, Tue, Wed, Thu, Fri, Sat, Sun)
  - 변경: 휴무일(빈 문자열) 처리 로직 추가, null은 정보 없음으로 처리
- `src/main/java/com/gotcha/domain/shop/dto/ShopDetailResponse.java` - 영업 상태 필드 변경
  - 변경: Boolean isOpen → String openStatus
  - 변경: @Schema allowableValues 추가 ("영업 중", "영업 종료", "휴무", "")
- `src/main/java/com/gotcha/domain/shop/dto/ShopMapResponse.java` - 영업 상태 필드 변경
  - 변경: Boolean isOpen → String openStatus
- `src/main/java/com/gotcha/domain/favorite/dto/FavoriteShopResponse.java` - 영업 상태 필드 변경
  - 변경: Boolean isOpen → String openStatus
- `src/main/java/com/gotcha/domain/user/dto/MyShopResponse.java` - 영업 상태 필드 변경
  - 변경: Boolean isOpen → String openStatus
- `src/main/java/com/gotcha/domain/favorite/service/FavoriteService.java` - openStatus 사용
  - 변경: isOpenNow() → getOpenStatus() 호출
- `src/main/java/com/gotcha/domain/user/service/UserService.java` - openStatus 사용
  - 변경: isOpenNow() → getOpenStatus() 호출
- `src/main/java/com/gotcha/domain/shop/dto/CreateShopRequest.java` - 시간 형식 예시 업데이트
  - 변경: 모든 Schema 예시의 시간 구분자 ~ → -
- `src/test/java/com/gotcha/domain/shop/controller/ShopControllerTest.java` - 테스트 데이터 업데이트
  - 변경: openTime 테스트 데이터 시간 구분자 ~ → -
- `src/main/resources/application-local.yml` - 환경변수 기본값 추가
  - 추가: oauth2.cookie-encryption-key 기본값 (local 환경)
  - 추가: shop.default-image-url 기본값 (local 환경)
- `docs/api-spec.md` - API 명세 전면 업데이트
  - 추가: GET /shops/{shopId}/reviews/images API 명세 (리뷰 이미지 전체 조회)
  - 변경: 모든 API Response의 isOpen → openStatus
  - 변경: 모든 openTime 예시의 시간 구분자 ~ → -
  - 변경: openStatus 필드 설명 및 allowableValues 추가
- `docs/api-design.md` - API 엔드포인트 목록 업데이트
  - 추가: GET /shops/{id}/reviews/images 엔드포인트
  - 변경: Response 예시의 isOpen → openStatus
- `docs/business-rules.md` - 비즈니스 규칙 업데이트
  - 변경: "영업중 판단 로직" → "영업 상태 판단 로직"
  - 변경: openTime 형식 설명 (단일 시간대 → JSON 요일별)
  - 변경: isOpen (Boolean) → openStatus (String) 로직 설명
  - 추가: 4가지 영업 상태 설명 및 판단 로직

---

## 2026-01-11

### 추가
- `src/test/java/com/gotcha/domain/review/repository/ReviewLikeRepositoryTest.java` - 배치 쿼리 메서드 테스트 (N+1 방지)

### 수정
- `src/main/java/com/gotcha/domain/review/repository/ReviewLikeRepository.java` - 배치 쿼리 메서드 추가
  - 추가: countByReviewIdInGroupByReviewId() - 여러 리뷰의 좋아요 수 일괄 조회 (N+1 방지)
  - 추가: findLikedReviewIds() - 특정 사용자가 좋아요한 리뷰 ID 목록 일괄 조회 (N+1 방지)
  - 추가: ReviewLikeCount Projection 인터페이스
- `src/main/java/com/gotcha/domain/review/service/ReviewService.java` - getReviews() N+1 문제 수정
  - 변경: reviewId마다 개별 쿼리 → IN 절로 배치 쿼리 (성능 개선: 20개 조회 시 41 쿼리 → 3 쿼리)
- `src/main/java/com/gotcha/domain/shop/service/ShopService.java` - 성능 개선 및 리팩토링
  - 추가: parseOpenTime() private 메서드 (JSON 파싱 재사용)
  - 추가: isOpenNow(Map<String, String>) 오버로드 (파싱된 Map 사용)
  - 추가: getTodayOpenTime(Map<String, String>) 오버로드 (파싱된 Map 사용)
  - 변경: getShopDetail() - openTime JSON 중복 파싱 제거 (1회만 파싱)
  - 변경: getTop5Reviews() - N+1 문제 수정 (배치 쿼리 사용, 11 쿼리 → 4 쿼리)
  - 변경: isOpenNow() - 익일 영업(overnight) 처리 주석 명확화
- `src/main/java/com/gotcha/domain/shop/controller/ShopController.java` - GET /shops/{shopId} 엔드포인트 수정
  - 추가: @ApiResponses에 400 응답 추가 (C003: 유효하지 않은 sortBy 값)
- `docs/api-spec.md` - GET /shops/{shopId} API 명세 수정 (실제 구현과 일치)
  - 추가: sortBy Query Parameter (LATEST: 최신순, LIKE_COUNT: 좋아요순, 기본값: LATEST)
  - 추가: Error Responses에 C003 추가 (유효하지 않은 sortBy 값)
  - 제거: lat, lng Query Parameters (실제 구현에 없음, 거리 계산 미지원)
  - 변경: Response 예시를 실제 ShopDetailResponse 구조에 맞게 수정
    - 제거: distance, region, district, neighborhood, favoriteCount, commentCount, reviewCount, createdAt
    - 추가: todayOpenTime, isOpen, reviews (ReviewResponse 배열), totalReviewImageCount, recentReviewImages
    - 변경: address → addressName, openTime → JSON 형식

---

## 2026-01-12

### 추가
- `src/main/java/com/gotcha/domain/favorite/repository/FavoriteRepository.java` - 전체 조회용 메서드 추가
  - 추가: findAllByUserIdWithShop(Long userId) - List 버전 (페이지네이션 없이 전체 조회)

### 수정
- `src/main/java/com/gotcha/domain/shop/dto/MapBoundsRequest.java` - 거리 계산 기준 좌표 명확화
  - 변경: centerLat, centerLng → latitude, longitude (파라미터명 변경)
  - 변경: 설명을 "사용자 현재 위치 좌표 (거리 계산 기준)"으로 명확화
  - 의미: 지도 중심이 아닌 사용자 실제 위치 기준으로 거리 계산
- `src/main/java/com/gotcha/domain/shop/controller/ShopController.java` - getShopsInMap() 수정
  - 변경: bounds.centerLat(), bounds.centerLng() → bounds.latitude(), bounds.longitude()
  - 추가: latitude/longitude null 체크 로직 (BusinessException 사용)
  - 버그 수정: null 값 입력 시 무한 로딩 문제 해결 (IllegalArgumentException → BusinessException, 400 응답 반환)
- `src/main/java/com/gotcha/domain/shop/service/ShopService.java` - getShopsInMap() 수정
  - 변경: 파라미터명 centerLat, centerLng → latitude, longitude
  - 변경: JavaDoc 주석 "중심 위도/경도" → "사용자 현재 위치 위도/경도"
  - 변경: 로그 메시지 "center" → "userLocation"
  - 변경: 거리 계산 주석 명확화 (사용자 위치 기준)
  - 변경: findAllByUserId() → findAllByUserIdWithShop(userId) (JOIN FETCH 사용, 전체 조회)
  - 성능 개선: 찜 목록 조회 시 N+1 쿼리 제거 (N개 쿼리 → 1개 쿼리)
- `src/main/java/com/gotcha/domain/favorite/service/FavoriteService.java` - getMyFavorites() N+1 문제 수정 및 페이지네이션 제거
  - 제거: Pageable import (더 이상 사용하지 않음)
  - 변경: findAllByUserId() → findAllByUserIdWithShop(userId) (JOIN FETCH 사용, 전체 조회)
  - 변경: 페이지네이션 제거 (Pageable.unpaged() → List 직접 반환)
  - 성능 개선: 즐겨찾기 목록 조회 시 N+1 쿼리 제거 (N개 쿼리 → 1개 쿼리)
- `src/main/java/com/gotcha/domain/shop/dto/MapBoundsRequest.java` - latitude, longitude 선택 사항으로 변경
  - 제거: latitude, longitude의 @NotNull 어노테이션
  - 변경: @Schema required = false로 설정
  - 변경: 설명에 "선택" 명시 추가
- `src/main/java/com/gotcha/domain/shop/controller/ShopController.java` - getShopsInMap() 파라미터 처리 개선
  - 제거: MapBoundsRequest DTO 사용 (@ModelAttribute bounds)
  - 변경: 개별 @RequestParam으로 변경 (latitude, longitude는 String으로 받음)
  - 추가: parseDoubleOrNull() 헬퍼 메서드 (null, "null", 빈 문자열 모두 null 처리)
  - 제거: latitude/longitude null 체크 예외 던지는 로직
  - 변경: @Operation description 업데이트 (latitude, longitude 선택 파라미터 명시)
- `src/main/java/com/gotcha/domain/shop/service/ShopService.java` - getShopsInMap() null 처리 개선
  - 추가: latitude, longitude null 체크 로직 (validateCoordinates 호출 전)
  - 변경: 거리 계산 시 null 처리 (null이면 distance를 null로 설정)
  - 변경: 정렬 시 null 안전 처리 (null은 Double.MAX_VALUE로 맨 뒤 정렬)
  - 변경: JavaDoc 업데이트 (latitude, longitude 선택 파라미터 명시)
- `src/main/java/com/gotcha/domain/shop/dto/ShopMapResponse.java` - distance 필드 설명 업데이트
  - 변경: @Schema description에 "사용자 위치 정보가 없으면 null" 명시
  - 추가: @Schema nullable = true 속성
  - 변경: JavaDoc distance 파라미터 설명 업데이트
- `src/test/java/com/gotcha/domain/shop/service/ShopServiceTest.java` - null 처리 테스트 추가
  - 추가: GetShopsInMap 테스트 클래스 (5개 테스트 케이스)
  - 테스트: latitude, longitude 모두 있을 때 거리 계산
  - 테스트: latitude가 null일 때 distance null 반환
  - 테스트: longitude가 null일 때 distance null 반환
  - 테스트: 둘 다 null일 때 distance null 반환
  - 테스트: 비로그인 사용자도 정상 조회 가능
- `docs/api-spec.md` - GET /shops/map API 명세 추가
  - 추가: GET /shops/map 엔드포인트 문서화
  - 추가: Query Parameters 명세 (latitude, longitude 선택 사항)
  - 추가: Response 예시 (latitude/longitude 있을 때, 없을 때)
  - 추가: 주의사항 섹션 (null 처리 방식 설명)

---

## 2026-01-11

### 추가
- `src/test/java/com/gotcha/domain/review/repository/ReviewLikeRepositoryTest.java` - 배치 쿼리 메서드 테스트 (N+1 방지)

### 수정
- `src/main/java/com/gotcha/domain/review/repository/ReviewLikeRepository.java` - 배치 쿼리 메서드 추가
  - 추가: countByReviewIdInGroupByReviewId() - 여러 리뷰의 좋아요 수 일괄 조회 (N+1 방지)
  - 추가: findLikedReviewIds() - 특정 사용자가 좋아요한 리뷰 ID 목록 일괄 조회 (N+1 방지)
  - 추가: ReviewLikeCount Projection 인터페이스
- `src/main/java/com/gotcha/domain/review/service/ReviewService.java` - getReviews() N+1 문제 수정
  - 변경: reviewId마다 개별 쿼리 → IN 절로 배치 쿼리 (성능 개선: 20개 조회 시 41 쿼리 → 3 쿼리)
- `src/main/java/com/gotcha/domain/shop/service/ShopService.java` - 성능 개선 및 리팩토링
  - 추가: parseOpenTime() private 메서드 (JSON 파싱 재사용)
  - 추가: isOpenNow(Map<String, String>) 오버로드 (파싱된 Map 사용)
  - 추가: getTodayOpenTime(Map<String, String>) 오버로드 (파싱된 Map 사용)
  - 변경: getShopDetail() - openTime JSON 중복 파싱 제거 (1회만 파싱)
  - 변경: getTop5Reviews() - N+1 문제 수정 (배치 쿼리 사용, 11 쿼리 → 4 쿼리)
  - 변경: isOpenNow() - 익일 영업(overnight) 처리 주석 명확화
- `src/main/java/com/gotcha/domain/shop/controller/ShopController.java` - GET /shops/{shopId} 엔드포인트 수정
  - 추가: @ApiResponses에 400 응답 추가 (C003: 유효하지 않은 sortBy 값)
- `docs/api-spec.md` - GET /shops/{shopId} API 명세 수정 (실제 구현과 일치)
  - 추가: sortBy Query Parameter (LATEST: 최신순, LIKE_COUNT: 좋아요순, 기본값: LATEST)
  - 추가: Error Responses에 C003 추가 (유효하지 않은 sortBy 값)
  - 제거: lat, lng Query Parameters (실제 구현에 없음, 거리 계산 미지원)
  - 변경: Response 예시를 실제 ShopDetailResponse 구조에 맞게 수정
    - 제거: distance, region, district, neighborhood, favoriteCount, commentCount, reviewCount, createdAt
    - 추가: todayOpenTime, isOpen, reviews (ReviewResponse 배열), totalReviewImageCount, recentReviewImages
    - 변경: address → addressName, openTime → JSON 형식

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
