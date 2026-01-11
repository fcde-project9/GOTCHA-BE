# API 상세 명세

## 공통 사항

### Base URL
```text
/api
```

### 공통 Response 형식

#### 성공
```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```

#### 실패
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "S001",
    "message": "가게를 찾을 수 없습니다"
  }
}
```

### 페이징 Response
```json
{
  "success": true,
  "data": {
    "content": [...],
    "totalCount": 100,
    "page": 0,
    "size": 20,
    "hasNext": true
  }
}
```

---

## 인증 API

### POST /auth/login/{provider}

소셜 로그인 처리

**Path Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| provider | String | O | kakao, google, naver |

**Request Body**
```json
{
  "accessToken": "소셜 액세스 토큰"
}
```

**Response (200)**
```json
{
  "success": true,
  "data": {
    "accessToken": "JWT 토큰",
    "refreshToken": "리프레시 토큰",
    "user": {
      "id": 1,
      "nickname": "빨간캡슐#21",
      "profileImageUrl": "https://...",
      "isNewUser": true
    }
  }
}
```

---

### POST /auth/logout

로그아웃 (리프레시 토큰 무효화)

**Headers**
```
Authorization: Bearer {accessToken}
```

**Response (200)**
```json
{
  "success": true,
  "data": null
}
```

---

### POST /auth/reissue

토큰 재발급

**Request Body**
```json
{
  "refreshToken": "리프레시 토큰"
}
```

**Response (200)**
```json
{
  "success": true,
  "data": {
    "accessToken": "새 JWT 토큰",
    "refreshToken": "새 리프레시 토큰",
    "user": {
      "id": 1,
      "nickname": "빨간캡슐#21",
      "email": "user@example.com",
      "socialType": "KAKAO",
      "isNewUser": false
    }
  }
}
```

**Error Responses**
| 코드 | 상황 |
|------|------|
| A010 | 리프레시 토큰을 찾을 수 없습니다 |
| A011 | 리프레시 토큰이 만료되었습니다 |

---

### GET /auth/nickname/random

랜덤 닉네임 생성

**Response (200)**
```json
{
  "success": true,
  "data": {
    "nickname": "빨간캡슐#21"
  }
}
```

---

### GET /auth/nickname/check

닉네임 중복 및 유효성 체크

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| nickname | String | O | 체크할 닉네임 |

**Response (200)**
```json
{
  "success": true,
  "data": {
    "nickname": "파란캡슐#99",
    "available": true,
    "reason": null
  }
}
```

**Response (200) - 사용 불가**
```json
{
  "success": true,
  "data": {
    "nickname": "파란캡슐#99",
    "available": false,
    "reason": "DUPLICATE"
  }
}
```

**reason 값**
| 값 | 설명 |
|---|------|
| null | 사용 가능 |
| DUPLICATE | 이미 사용 중인 닉네임 |
| INVALID_FORMAT | 형식 오류 (2-20자, 허용 문자 위반) |
| FORBIDDEN_WORD | 금지어 포함 |

---

## 가게 API

### GET /shops

주변 가챠샵 목록 조회

**Query Parameters**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| lat | Double | O | - | 위도 |
| lng | Double | O | - | 경도 |
| radius | Integer | X | 1000 | 반경 (m), 최대 5000 |
| sort | String | X | distance | distance, newest |
| page | Integer | X | 0 | 페이지 번호 |
| size | Integer | X | 20 | 페이지 크기, 최대 50 |

**Response (200)**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "가챠샵 신사점",
        "address": "서울시 강남구 신사동 123-45",
        "latitude": 37.5172,
        "longitude": 127.0473,
        "mainImageUrl": "https://...",
        "distance": 300,
        "isOpen": true,
        "isFavorite": false
      }
    ],
    "totalCount": 328,
    "page": 0,
    "size": 20,
    "hasNext": true
  }
}
```

---

### GET /shops/search

가게 이름 검색

**Query Parameters**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| keyword | String | O | - | 검색어 (2자 이상) |
| lat | Double | X | - | 거리 계산용 위도 |
| lng | Double | X | - | 거리 계산용 경도 |
| page | Integer | X | 0 | |
| size | Integer | X | 20 | |

**Response (200)**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "가챠샵 신사점",
        "address": "서울시 강남구 신사동 123-45",
        "mainImageUrl": "https://...",
        "distance": 300
      }
    ],
    "totalCount": 5,
    "page": 0,
    "size": 20,
    "hasNext": false
  }
}
```

---

### GET /shops/nearby

50m 내 가게 목록 조회 (제보 전 중복 체크용)

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| lat | Double | O | 위도 |
| lng | Double | O | 경도 |

**Response (200)**
```json
{
  "success": true,
  "data": {
    "count": 4,
    "shops": [
      {
        "id": 1,
        "name": "뽀바뽀빠",
        "address": "서울시 강남구 강남대로 364",
        "distance": 30
      }
    ]
  }
}
```

---

### GET /shops/{shopId}

가게 상세 조회

**Path Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| shopId | Long | O | 가게 ID |

**Query Parameters**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| lat | Double | X | - | 거리 계산용 위도 |
| lng | Double | X | - | 거리 계산용 경도 |
| sortBy | String | X | LATEST | 리뷰 정렬 방식 (LATEST: 최신순, LIKE_COUNT: 좋아요순) |

**Response (200)**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "가챠샵 신사점",
    "address": "서울시 강남구 신사동 123-45",
    "latitude": 37.5172,
    "longitude": 127.0473,
    "mainImageUrl": "https://...",
    "locationHint": "신사역 4번 출구에서 도보 3분, 스타벅스 옆 건물 1층",
    "openTime": "10:00-22:00",
    "region": "서울",
    "district": "강남구",
    "neighborhood": "신사동",
    "distance": 300,
    "isOpen": true,
    "isFavorite": false,
    "favoriteCount": 42,
    "commentCount": 15,
    "reviewCount": 8,
    "createdAt": "2025-01-01T10:00:00"
  }
}
```

**Error Responses**

| 코드 | 상황 |
|------|------|
| C003 | 유효하지 않은 sortBy 값 |
| S001 | 가게를 찾을 수 없음 |

---

### POST /shops/report

가게 제보 (새 가게 등록)

**Headers**
```
Authorization: Bearer {accessToken}
```

**Request Body**
```json
{
  "name": "가챠샵 홍대점",
  "latitude": 37.5563,
  "longitude": 126.9236,
  "mainImageUrl": "https://...",
  "locationHint": "홍대입구역 9번 출구 앞",
  "openTime": "10:00-22:00"
}
```

**Validation**
| 필드 | 규칙 |
|------|------|
| name | 필수, 2-100자 |
| latitude | 필수, -90 ~ 90 |
| longitude | 필수, -180 ~ 180 |
| mainImageUrl | 필수 |
| locationHint | 선택, 최대 500자 |
| openTime | 선택, HH:mm-HH:mm 형식 |

**Response (201)**
```json
{
  "success": true,
  "data": {
    "id": 100,
    "name": "가챠샵 홍대점"
  }
}
```

**Error Responses**
| 코드 | 상황 |
|------|------|
| S002 | 중복된 가게 (반경 50m 내 동일 이름) |
| A001 | 인증 필요 |

---

## 찜 API

### GET /users/me/favorites

내 찜 목록 조회

**Headers**
```
Authorization: Bearer {accessToken}
```

**Query Parameters**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| lat | Double | X | - | 거리 계산용 |
| lng | Double | X | - | 거리 계산용 |
| page | Integer | X | 0 | |
| size | Integer | X | 20 | |

**Response (200)**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "가챠샵 신사점",
        "address": "서울시 강남구...",
        "mainImageUrl": "https://...",
        "distance": 300,
        "isOpen": true,
        "favoritedAt": "2025-01-01T10:00:00"
      }
    ],
    "totalCount": 5,
    "page": 0,
    "size": 20,
    "hasNext": false
  }
}
```

---

### POST /shops/{shopId}/favorite

찜 추가

**Headers**
```
Authorization: Bearer {accessToken}
```

**Response (201)**
```json
{
  "success": true,
  "data": {
    "shopId": 1,
    "isFavorite": true
  }
}
```

**Error Responses**
| 코드 | 상황 |
|------|------|
| F001 | 이미 찜한 가게 |
| S001 | 가게를 찾을 수 없음 |

---

### DELETE /shops/{shopId}/favorite

찜 삭제

**Headers**
```
Authorization: Bearer {accessToken}
```

**Response (200)**
```json
{
  "success": true,
  "data": {
    "shopId": 1,
    "isFavorite": false
  }
}
```

---

## 댓글 API

### GET /shops/{shopId}/comments

댓글 목록 조회

**Query Parameters**
| 파라미터 | 타입 | 필수 | 기본값 |
|---------|------|------|--------|
| page | Integer | X | 0 |
| size | Integer | X | 20 |

**Response (200)**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "content": "뽑기 종류가 다양해요!",
        "author": {
          "id": 1,
          "nickname": "빨간캡슐#21",
          "profileImageUrl": "https://..."
        },
        "isAnonymous": false,
        "isOwner": false,
        "createdAt": "2025-01-01T10:00:00"
      }
    ],
    "totalCount": 15,
    "page": 0,
    "size": 20,
    "hasNext": false
  }
}
```

---

### POST /shops/{shopId}/comments

댓글 작성

**Headers**
```
Authorization: Bearer {accessToken}
```

**Request Body**
```json
{
  "content": "뽑기 종류가 다양해요!",
  "isAnonymous": false
}
```

**Validation**
| 필드 | 규칙 |
|------|------|
| content | 필수, 1-500자 |

**Response (201)**
```json
{
  "success": true,
  "data": {
    "id": 100,
    "content": "뽑기 종류가 다양해요!"
  }
}
```

---

## 리뷰 API

### GET /shops/{shopId}/reviews

리뷰 목록 조회

**Query Parameters**
| 파라미터 | 타입 | 필수 | 기본값 |
|---------|------|------|--------|
| page | Integer | X | 0 |
| size | Integer | X | 20 |

**Response (200)**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "content": "원하는 캐릭터 뽑았어요!",
        "imageUrls": [
          "https://storage.googleapis.com/.../image1.jpg",
          "https://storage.googleapis.com/.../image2.jpg"
        ],
        "author": {
          "id": 1,
          "nickname": "빨간캡슐#21",
          "profileImageUrl": "https://..."
        },
        "isOwner": false,
        "createdAt": "2025-01-01T10:00:00"
      }
    ],
    "totalCount": 8,
    "page": 0,
    "size": 20,
    "hasNext": false
  }
}
```

---

### POST /shops/{shopId}/reviews

리뷰 작성

**Headers**
```
Authorization: Bearer {accessToken}
```

**Request Body**
```json
{
  "content": "원하는 캐릭터 뽑았어요!",
  "imageUrls": [
    "https://storage.googleapis.com/.../image1.jpg",
    "https://storage.googleapis.com/.../image2.jpg"
  ]
}
```

**Validation**
| 필드 | 규칙 |
|------|------|
| content | 필수, 10-1000자 |
| imageUrls | 선택, 최대 10개 |

**Response (201)**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "content": "원하는 캐릭터 뽑았어요!",
    "imageUrls": [
      "https://storage.googleapis.com/.../image1.jpg",
      "https://storage.googleapis.com/.../image2.jpg"
    ],
    "author": {
      "id": 1,
      "nickname": "빨간캡슐#21",
      "profileImageUrl": "https://..."
    },
    "isOwner": true,
    "createdAt": "2025-01-08T12:00:00"
  }
}
```

---

### PUT /shops/{shopId}/reviews/{reviewId}

리뷰 수정

**Headers**
```
Authorization: Bearer {accessToken}
```

**Request Body**
```json
{
  "content": "수정된 리뷰 내용입니다!",
  "imageUrls": [
    "https://storage.googleapis.com/.../new-image1.jpg",
    "https://storage.googleapis.com/.../new-image2.jpg"
  ]
}
```

**Validation**
| 필드 | 규칙 |
|------|------|
| content | 필수, 10-1000자 |
| imageUrls | 선택, 최대 10개 |

**Response (200)**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "content": "수정된 리뷰 내용입니다!",
    "imageUrls": [
      "https://storage.googleapis.com/.../new-image1.jpg",
      "https://storage.googleapis.com/.../new-image2.jpg"
    ],
    "author": {
      "id": 1,
      "nickname": "빨간캡슐#21",
      "profileImageUrl": "https://..."
    },
    "isOwner": true,
    "createdAt": "2025-01-08T12:00:00"
  }
}
```

**Error Responses**
| 코드 | 상황 |
|------|------|
| R001 | 리뷰를 찾을 수 없음 |
| R003 | 본인의 리뷰만 수정 가능 |
| R005 | 이미지 개수 초과 (최대 10개) |

**참고**
- 기존 이미지는 모두 삭제되고 새로운 이미지로 대체됩니다
- 이미지를 모두 삭제하려면 imageUrls를 빈 배열 [] 또는 null로 전송

---

### DELETE /shops/{shopId}/reviews/{reviewId}

리뷰 삭제

**Headers**
```
Authorization: Bearer {accessToken}
```

**Response (200)**
```json
{
  "success": true,
  "data": null
}
```

**Error Responses**
| 코드 | 상황 |
|------|------|
| R001 | 리뷰를 찾을 수 없음 |
| R003 | 본인의 리뷰만 삭제 가능 |

---

## 사용자 API

### GET /users/me

내 정보 조회

**Headers**
```
Authorization: Bearer {accessToken}
```

**Response (200)**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "nickname": "빨간캡슐#21",
    "email": "user@example.com",
    "profileImageUrl": null,
    "socialType": "KAKAO"
  }
}
```

---

### PATCH /users/me/nickname

닉네임 수정

**Headers**
```
Authorization: Bearer {accessToken}
```

**Request Body**
```json
{
  "nickname": "파란캡슐#99"
}
```

**Validation**
| 필드 | 규칙 |
|------|------|
| nickname | 필수, 2-20자, 허용문자: 한글, 영문, 숫자, #, _ |

**Response (200)**
```json
{
  "success": true,
  "data": {
    "nickname": "파란캡슐#99"
  }
}
```

**Error Responses**
| 코드 | 상황 |
|------|------|
| U001 | 이미 사용 중인 닉네임 |
| U002 | 닉네임 형식 오류 |

---

### PATCH /users/me/profile-image

프로필 이미지 변경

**Headers**
```
Authorization: Bearer {accessToken}
```

**Request Body**
```json
{
  "profileImageUrl": "https://storage.googleapis.com/gotcha-dev-files/profiles/abc-123.webp"
}
```

**Validation**
| 필드 | 규칙 |
|------|------|
| profileImageUrl | 필수, GCS URL 형식 (https://storage.googleapis.com/...) |

**Response (200)**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "nickname": "빨간캡슐#21",
    "email": "user@example.com",
    "profileImageUrl": "https://storage.googleapis.com/gotcha-dev-files/profiles/abc-123.webp",
    "socialType": "KAKAO"
  }
}
```

**Error Responses**
| 코드 | 상황 |
|------|------|
| C001 | URL 형식 오류 |
| A001 | 인증 필요 |

**참고**
- 먼저 `/api/files/upload`로 이미지를 업로드한 후 반환된 URL 사용
- 기존 커스텀 이미지는 GCS에서 자동 삭제됨
- 기본 프로필 이미지는 삭제되지 않음 (공유 리소스)

---

### DELETE /users/me/profile-image

프로필 이미지 삭제 (기본 이미지로 복구)

**Headers**
```
Authorization: Bearer {accessToken}
```

**Request Body**
- 없음 (Body 없이 DELETE 요청)

**Response (200)**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "nickname": "빨간캡슐#21",
    "email": "user@example.com",
    "profileImageUrl": "https://storage.googleapis.com/gotcha-dev-files/defaults/profile-default-join.png",
    "socialType": "KAKAO"
  }
}
```

**Error Responses**
| 코드 | 상황 |
|------|------|
| A001 | 인증 필요 |

**참고**
- 프로필 이미지를 삭제하고 자동으로 기본 프로필 이미지로 복구
- 기존 커스텀 이미지는 GCS에서 자동 삭제됨
- 기본 프로필 이미지를 사용 중인 경우 변화 없음

---

### GET /users/me/shops

내가 제보한 가게 목록 조회

**Headers**
```
Authorization: Bearer {accessToken}
```

**Query Parameters**
| 파라미터 | 타입 | 필수 | 기본값 |
|---------|------|------|--------|
| lat | Double | X | - |
| lng | Double | X | - |
| page | Integer | X | 0 |
| size | Integer | X | 20 |

**Response (200)**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "가챠샵 신사점",
        "address": "서울시 강남구...",
        "mainImageUrl": "https://...",
        "distance": 300,
        "createdAt": "2025-01-01T10:00:00"
      }
    ],
    "totalCount": 2,
    "page": 0,
    "size": 20,
    "hasNext": false
  }
}
```

---

### DELETE /users/me

회원 탈퇴 (탈퇴 설문 포함)

**Headers**
```
Authorization: Bearer {accessToken}
```

**Request Body**
```json
{
  "reasons": ["LOW_USAGE", "INSUFFICIENT_INFO"],
  "detail": "사용자가 입력한 상세 사유"
}
```

**reasons 값 (복수 선택 가능)**
| 값 | 설명 |
|---|------|
| LOW_USAGE | 사용을 잘 안하게 돼요 |
| INSUFFICIENT_INFO | 가챠샵 정보가 부족해요 |
| INACCURATE_INFO | 가챠샵 정보가 기재된 내용과 달라요 |
| PRIVACY_CONCERN | 개인정보 보호를 위해 삭제할래요 |
| HAS_OTHER_ACCOUNT | 다른 계정이 있어요 |
| OTHER | 기타 |

**Validation**
| 필드 | 규칙 |
|------|------|
| reasons | 필수, 최소 1개 이상 |
| detail | 선택, 최대 500자 |

**Response (200)**
```json
{
  "success": true,
  "data": null
}
```

**Error Responses**
| 코드 | 상황 |
|------|------|
| U005 | 이미 탈퇴한 사용자 |
| A001 | 인증 필요 |

**참고**
- 설문 저장과 탈퇴가 하나의 트랜잭션으로 처리됨
- 탈퇴 시 삭제되는 데이터: 찜 목록, 리뷰(+이미지), 댓글, Refresh Token
- 사용자는 soft delete 처리됨 (isDeleted = true, 개인정보 마스킹)
- 소셜 연동 해제됨 (동일 계정으로 재가입 가능)

---

## 파일 업로드 API

### POST /files/upload

이미지 파일을 Google Cloud Storage에 업로드

**Headers**
```
Authorization: Bearer {accessToken}
Content-Type: multipart/form-data
```

**Request Parameters**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| file | MultipartFile | O | 업로드할 이미지 파일 |
| folder | String | O | 저장할 폴더 (reviews, shops, profiles) |

**Validation**
- 허용 형식: jpg, jpeg, png, webp, heic, heif
- 최대 크기: 20MB
- 허용 폴더: reviews, shops, profiles

**Response (201)**
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

**Error Responses**
| 코드 | 상황 |
|------|------|
| I001 | 지원하지 않는 파일 형식 |
| I002 | 파일 크기 초과 (20MB) |
| I003 | 파일 업로드 실패 |
| I004 | 잘못된 폴더명 |

**사용 예시**
```bash
curl -X POST /api/files/upload \
  -H "Authorization: Bearer {accessToken}" \
  -F "file=@photo.jpg" \
  -F "folder=reviews"
```

**참고**
- 상세한 사용법은 `docs/file-upload-guide.md` 참고
- 업로드된 URL을 리뷰/가게 API에 전달하여 사용
