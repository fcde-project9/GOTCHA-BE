# 이미지 업로드 가이드

## 개요

GOTCHA 프로젝트의 모든 이미지 업로드는 **Google Cloud Storage (GCS)**를 사용합니다.
`FileUploadService`가 공통 서비스로 제공되며, 리뷰/가게/프로필 이미지를 통합 관리합니다.

---

## 프론트엔드 개발자용

### 1. 이미지 업로드 API

**엔드포인트**: `POST /api/files/upload`

**Request**:
```http
POST /api/files/upload
Content-Type: multipart/form-data

file: [이미지 파일]
folder: "reviews"  // "shops", "profiles" 중 선택
```

**Response (201)**:
```json
{
  "success": true,
  "data": {
    "url": "https://storage.googleapis.com/gotcha-bucket/reviews/abc123-def456.jpg",
    "originalFilename": "my-photo.jpg",
    "size": 1024000,
    "contentType": "image/jpeg"
  }
}
```

### 2. 지원 폴더

| 폴더명 | 용도 | 예시 |
|--------|------|------|
| `reviews` | 리뷰 이미지 | 리뷰 작성/수정 시 |
| `shops` | 가게 이미지 | 가게 제보 시 |
| `profiles` | 프로필 이미지 | 프로필 사진 변경 시 |

### 3. 제약사항

| 항목 | 제한 |
|------|------|
| 파일 크기 | 최대 50MB |
| 파일 형식 | jpg, jpeg, png, webp, heic, heif |
| 폴더명 | reviews, shops, profiles만 허용 |

### 4. 사용 예시

#### 리뷰 작성 플로우

```javascript
// Step 1: 이미지 업로드 (각각)
const uploadedUrls = [];

for (const file of selectedFiles) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('folder', 'reviews');

  const response = await fetch('/api/files/upload', {
    method: 'POST',
    body: formData,
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  });

  const result = await response.json();
  uploadedUrls.push(result.data.url);
}

// Step 2: 리뷰 작성 API 호출 (URL만 전달)
await fetch('/api/shops/1/reviews', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${accessToken}`
  },
  body: JSON.stringify({
    content: '좋아요!',
    imageUrls: uploadedUrls  // ← GCS URL 배열
  })
});
```

#### 리뷰 수정 플로우

```javascript
// 기존 리뷰: ["url1", "url2", "url3", "url4", "url5"]

// 사용자 동작: 3번 이미지 삭제 + 새 이미지 2개 추가

// Step 1: 새 이미지만 업로드
const newUrls = [];
for (const newFile of newSelectedFiles) {
  const formData = new FormData();
  formData.append('file', newFile);
  formData.append('folder', 'reviews');

  const response = await fetch('/api/files/upload', { ... });
  const result = await response.json();
  newUrls.push(result.data.url);
}

// Step 2: 최종 URL 배열 구성 (삭제된 url3 제외 + 새 URL 추가)
const finalUrls = [
  "url1",      // 유지
  "url2",      // 유지
  // "url3" 제외 (삭제)
  "url4",      // 유지
  "url5",      // 유지
  ...newUrls   // 새로 추가
];

// Step 3: 리뷰 수정 API 호출
await fetch('/api/shops/1/reviews/10', {
  method: 'PUT',
  headers: { ... },
  body: JSON.stringify({
    content: '수정된 내용',
    imageUrls: finalUrls  // ← 최종 URL 배열 (순서 유지)
  })
});

// 백엔드가 자동으로:
// - GCS에서 url3만 삭제
// - DB에 finalUrls를 displayOrder 0,1,2,... 로 저장
```

---

## 백엔드 개발자용

### 1. FileUploadService 사용법

**위치**: `com.gotcha.domain.file.service.FileUploadService`

**의존성 주입**:
```java
@Service
@RequiredArgsConstructor
public class YourService {

    private final FileUploadService fileUploadService;

    // ...
}
```

### 2. 주요 메서드

#### 2.1 파일 삭제

```java
/**
 * GCS에서 파일 삭제
 *
 * @param fileUrl 삭제할 파일의 공개 URL
 */
public void deleteFile(String fileUrl)
```

**사용 예시** (ReviewService 참고):
```java
@Transactional
public void deleteReview(Long reviewId) {
    Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> ReviewException.notFound(reviewId));

    // 1. DB에서 이미지 URL 조회
    List<ReviewImage> images = reviewImageRepository
            .findAllByReviewIdOrderByDisplayOrder(reviewId);

    // 2. GCS에서 파일 삭제
    for (ReviewImage image : images) {
        try {
            fileUploadService.deleteFile(image.getImageUrl());
            log.info("Deleted image: {}", image.getImageUrl());
        } catch (Exception e) {
            log.error("Failed to delete image: {}", image.getImageUrl(), e);
            // 파일 삭제 실패해도 DB는 삭제 진행
        }
    }

    // 3. DB 레코드 삭제
    reviewImageRepository.deleteAllByReviewId(reviewId);
    reviewRepository.delete(review);
}
```

#### 2.2 파일 업로드 (일반적으로 사용 안 함)

**중요**: 백엔드에서 직접 업로드하는 경우는 드뭅니다.
- **프론트엔드**가 `/api/files/upload` API로 직접 업로드
- **백엔드**는 업로드된 URL을 받아서 DB에 저장

하지만 서버에서 직접 업로드해야 하는 경우:

```java
public FileUploadResponse uploadImage(MultipartFile file, String folder)
```

**사용 예시**:
```java
// 예: 관리자가 기본 프로필 이미지 업로드
FileUploadResponse response = fileUploadService.uploadImage(file, "profiles");
String imageUrl = response.url();
```

### 3. 개발 패턴

#### 패턴 1: 이미지 URL만 DB에 저장 (일반적)

```java
@Transactional
public ReviewResponse createReview(Long shopId, Long userId, CreateReviewRequest request) {
    // 1. request.imageUrls()는 프론트가 이미 업로드한 URL 배열
    //    → GCS에 파일은 이미 존재함

    // 2. Review 엔티티 생성
    Review review = Review.builder()
            .shop(shop)
            .user(user)
            .content(request.content())
            .build();
    reviewRepository.save(review);

    // 3. 이미지 URL을 DB에 저장 (파일은 이미 GCS에 있음)
    for (int i = 0; i < request.imageUrls().size(); i++) {
        ReviewImage image = ReviewImage.builder()
                .review(review)
                .imageUrl(request.imageUrls().get(i))  // ← URL만 저장
                .displayOrder(i)
                .build();
        reviewImageRepository.save(image);
    }

    return ReviewResponse.from(review, user, images);
}
```

#### 패턴 2: 이미지 수정 시 삭제된 것만 GCS에서 제거

```java
@Transactional
public ReviewResponse updateReview(Long reviewId, UpdateReviewRequest request) {
    Review review = findReviewById(reviewId);

    // 1. 기존 이미지 조회
    List<ReviewImage> existingImages = reviewImageRepository
            .findAllByReviewIdOrderByDisplayOrder(reviewId);

    // 2. 새 요청에 포함되지 않은 이미지만 GCS에서 삭제
    List<String> newImageUrls = request.imageUrls() != null
            ? request.imageUrls()
            : List.of();

    for (ReviewImage existingImage : existingImages) {
        if (!newImageUrls.contains(existingImage.getImageUrl())) {
            // 삭제된 이미지만 GCS에서 제거
            try {
                fileUploadService.deleteFile(existingImage.getImageUrl());
            } catch (Exception e) {
                log.error("Failed to delete image: {}", existingImage.getImageUrl(), e);
            }
        }
    }

    // 3. DB 전체 재생성 (displayOrder 재할당)
    reviewImageRepository.deleteAllByReviewId(reviewId);

    for (int i = 0; i < newImageUrls.size(); i++) {
        ReviewImage image = ReviewImage.builder()
                .review(review)
                .imageUrl(newImageUrls.get(i))
                .displayOrder(i)
                .build();
        reviewImageRepository.save(image);
    }

    return ReviewResponse.from(review, user, updatedImages);
}
```

#### 패턴 3: 엔티티 삭제 시 관련 파일도 삭제

```java
@Transactional
public void deleteReview(Long reviewId) {
    Review review = findReviewById(reviewId);

    // 1. 관련 이미지 조회
    List<ReviewImage> images = reviewImageRepository
            .findAllByReviewIdOrderByDisplayOrder(reviewId);

    // 2. GCS에서 파일 삭제
    for (ReviewImage image : images) {
        try {
            fileUploadService.deleteFile(image.getImageUrl());
        } catch (Exception e) {
            log.error("Failed to delete image: {}", image.getImageUrl(), e);
            // 파일 삭제 실패해도 계속 진행 (이미 삭제된 파일일 수 있음)
        }
    }

    // 3. DB 삭제 (CASCADE 설정되어 있다면 ReviewImage도 자동 삭제)
    reviewRepository.delete(review);
}
```

### 4. 참고 예제

**추천 참고 코드**: `ReviewService.java`

| 메서드 | 참고할 내용 |
|--------|------------|
| `createReview()` | 이미지 URL 저장 패턴 |
| `updateReview()` | 이미지 수정 시 안전한 GCS 삭제 |
| `deleteReview()` | 이미지 전체 삭제 패턴 |

---

## GCS 저장 구조

```
gotcha-bucket/
├── reviews/
│   ├── abc123-def456.jpg
│   ├── ghi789-jkl012.png
│   └── mno345-pqr678.webp
├── shops/
│   ├── stu901-vwx234.jpg
│   └── yza567-bcd890.png
└── profiles/
    ├── efg123-hij456.jpg
    └── klm789-nop012.heic
```

**파일명 규칙**: `UUID.확장자` (예: `abc123-def456-789.jpg`)

---

## 보안 주의사항

### 1. 폴더명 검증
- ✅ 화이트리스트: `reviews`, `shops`, `profiles`만 허용
- ❌ Path Traversal 공격 차단: `..`, `/`, `\` 포함 시 거부

### 2. 파일 타입 검증
- ✅ MIME Type 검증: `image/jpeg`, `image/png`, `image/webp`, `image/heic`, `image/heif`
- ❌ 확장자만 검증하면 안 됨 (우회 가능)

### 3. 파일 크기 제한
- ✅ 최대 50MB
- 프론트엔드에서도 사전 체크 권장

### 4. GCS 삭제 시 주의
- 삭제 실패해도 예외 던지지 않음 (이미 삭제된 파일일 수 있음)
- 트랜잭션 밖에서 동작 (GCS는 외부 시스템)

---

## 자주 묻는 질문 (FAQ)

### Q1. 백엔드에서 파일 업로드를 직접 해야 하나요?

**A**: 아니요. 일반적으로:
1. 프론트엔드가 `/api/files/upload` API로 파일 업로드
2. GCS URL을 받음
3. 해당 URL을 백엔드 API에 전달 (예: `/api/shops/1/reviews`)
4. 백엔드는 URL만 DB에 저장

### Q2. 이미지 수정 시 어떻게 동작하나요?

**A**:
1. 프론트엔드가 최종 URL 배열을 전송 (삭제된 URL 제외)
2. 백엔드가 기존 URL과 비교하여 삭제된 것만 GCS에서 제거
3. DB는 전체 삭제/재생성하여 displayOrder 재할당

### Q3. displayOrder가 불연속이 될 수 있나요?

**A**: 아니요. 항상 0부터 시작하는 연속된 숫자입니다.
- 예: `[0, 1, 2, 3, 4]` (5개 이미지)
- 3번 삭제 후: `[0, 1, 2, 3]` (4개 이미지, 순서 재할당)

### Q4. GCS 파일 삭제가 실패하면 어떻게 되나요?

**A**:
- 로그만 남기고 계속 진행 (이미 삭제된 파일일 수 있음)
- DB 삭제는 정상적으로 진행
- 트랜잭션 롤백 안 함 (GCS는 외부 시스템)

### Q5. 프로필 사진은 단일 파일인데 배열로 저장해야 하나요?

**A**: 아니요. 프로필 사진은 User 엔티티에 `profileImageUrl` 필드로 단일 문자열 저장.
- 리뷰/가게는 여러 장 → 별도 테이블 (ReviewImage, ShopImage)
- 프로필은 1장 → User 엔티티 필드

---

## 관련 문서

- API 스펙: `docs/api-spec.md` - 파일 업로드 API 섹션
- 에러 코드: `docs/error-codes.md` - 파일 관련 에러 (I001, I002, I003)
- Review 구현: `src/main/java/com/gotcha/domain/review/service/ReviewService.java`
