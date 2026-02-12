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
        "openStatus": "영업 중",
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

### GET /shops/map

지도 영역 내 가게 목록 조회

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| northEastLat | Double | O | 북동쪽 위도 |
| northEastLng | Double | O | 북동쪽 경도 |
| southWestLat | Double | O | 남서쪽 위도 |
| southWestLng | Double | O | 남서쪽 경도 |
| latitude | Double | X | 사용자 위치 위도 (거리 계산용, null이면 distance도 null 반환) |
| longitude | Double | X | 사용자 위치 경도 (거리 계산용, null이면 distance도 null 반환) |

**Response (200)**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "가챠샵 신사점",
      "mainImageUrl": "https://...",
      "latitude": 37.5172,
      "longitude": 127.0473,
      "openTime": "{\"Mon\":\"10:00-22:00\",\"Tue\":\"\",\"Wed\":\"10:00-22:00\"}",
      "openStatus": "영업 중",
      "distance": "50m",
      "isFavorite": false
    }
  ]
}
```

**Response (200) - latitude/longitude가 null인 경우**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "가챠샵 신사점",
      "mainImageUrl": "https://...",
      "latitude": 37.5172,
      "longitude": 127.0473,
      "openTime": "{\"Mon\":\"10:00-22:00\",\"Tue\":\"\",\"Wed\":\"10:00-22:00\"}",
      "openStatus": "영업 중",
      "distance": null,
      "isFavorite": false
    }
  ]
}
```

**주의사항**
- latitude, longitude는 선택 파라미터입니다
- 사용자 위치 정보가 없는 경우(null) distance는 null로 반환됩니다
- 거리는 사용자 위치 기준 10m 단위로 계산됩니다 (1000m 미만: "50m", 1000m 이상: "1.5km")
- 로그인한 사용자는 찜 여부(isFavorite)를 확인할 수 있습니다

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
| sortBy | String | X | LATEST | 리뷰 정렬 방식 (LATEST: 최신순, LIKE_COUNT: 좋아요순) |

**Response (200)**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "가챠샵 신사점",
    "addressName": "서울시 강남구 신사동 123-45",
    "locationHint": "신사역 4번 출구에서 도보 3분, 스타벅스 옆 건물 1층",
    "openTime": "{\"Mon\":\"10:00-22:00\",\"Tue\":\"\",\"Wed\":\"10:00-22:00\",\"Thu\":\"10:00-22:00\",\"Fri\":\"10:00-22:00\",\"Sat\":\"10:00-22:00\",\"Sun\":\"10:00-22:00\"}",
    "todayOpenTime": "10:00-22:00",
    "openStatus": "영업 중",
    "latitude": 37.5172,
    "longitude": 127.0473,
    "mainImageUrl": "https://...",
    "isFavorite": false,
    "reviews": [
      {
        "id": 1,
        "content": "원하는 캐릭터 뽑았어요!",
        "imageUrls": ["https://..."],
        "author": {
          "id": 1,
          "nickname": "빨간캡슐#21",
          "profileImageUrl": "https://..."
        },
        "isOwner": false,
        "likeCount": 5,
        "isLiked": false,
        "createdAt": "2025-01-01T10:00:00"
      }
    ],
    "reviewCount": 42,
    "totalReviewImageCount": 25,
    "recentReviewImages": [
      "https://s3.ap-northeast-2.amazonaws.com/.../image1.jpg",
      "https://s3.ap-northeast-2.amazonaws.com/.../image2.jpg",
      "https://s3.ap-northeast-2.amazonaws.com/.../image3.jpg",
      "https://s3.ap-northeast-2.amazonaws.com/.../image4.jpg"
    ]
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
| latitude | 필수, -90 - 90 |
| longitude | 필수, -180 - 180 |
| mainImageUrl | 필수 |
| locationHint | 선택, 최대 500자 |
| openTime | 선택, HH:mm-HH:mm 또는 HH:mm~HH:mm 형식 |

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
        "openStatus": "영업 중",
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
          "https://s3.ap-northeast-2.amazonaws.com/.../image1.jpg",
          "https://s3.ap-northeast-2.amazonaws.com/.../image2.jpg"
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
    "https://s3.ap-northeast-2.amazonaws.com/.../image1.jpg",
    "https://s3.ap-northeast-2.amazonaws.com/.../image2.jpg"
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
      "https://s3.ap-northeast-2.amazonaws.com/.../image1.jpg",
      "https://s3.ap-northeast-2.amazonaws.com/.../image2.jpg"
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
    "https://s3.ap-northeast-2.amazonaws.com/.../new-image1.jpg",
    "https://s3.ap-northeast-2.amazonaws.com/.../new-image2.jpg"
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
      "https://s3.ap-northeast-2.amazonaws.com/.../new-image1.jpg",
      "https://s3.ap-northeast-2.amazonaws.com/.../new-image2.jpg"
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

### GET /shops/{shopId}/reviews/images

가게 리뷰 이미지 전체 조회 (최신순)

**Path Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| shopId | Long | O | 가게 ID |

**Response (200)**
```json
{
  "success": true,
  "data": {
    "totalCount": 25,
    "imageUrls": [
      "https://s3.ap-northeast-2.amazonaws.com/.../image1.jpg",
      "https://s3.ap-northeast-2.amazonaws.com/.../image2.jpg",
      "https://s3.ap-northeast-2.amazonaws.com/.../image3.jpg"
    ]
  }
}
```

**Error Responses**
| 코드 | 상황 |
|------|------|
| S001 | 가게를 찾을 수 없음 |

**참고**
- 리뷰 생성일시 기준 최신순으로 정렬됨
- 모든 리뷰 이미지가 반환됨 (페이징 없음)
- 이미지가 없는 경우 빈 배열 반환

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
    "socialType": "KAKAO",
    "userType": "NORMAL"
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
  "profileImageUrl": "https://s3.ap-northeast-2.amazonaws.com/gotcha-dev-files/profiles/abc-123.webp"
}
```

**Validation**
| 필드 | 규칙 |
|------|------|
| profileImageUrl | 필수, S3 URL 형식 (https://{bucket}.s3.{region}.amazonaws.com/...) |

**Response (200)**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "nickname": "빨간캡슐#21",
    "email": "user@example.com",
    "profileImageUrl": "https://s3.ap-northeast-2.amazonaws.com/gotcha-dev-files/profiles/abc-123.webp",
    "socialType": "KAKAO",
    "userType": "NORMAL"
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
- 기존 커스텀 이미지는 S3에서 자동 삭제됨
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
    "profileImageUrl": "https://s3.ap-northeast-2.amazonaws.com/gotcha-dev-files/defaults/profile-default-join.png",
    "socialType": "KAKAO",
    "userType": "NORMAL"
  }
}
```

**Error Responses**
| 코드 | 상황 |
|------|------|
| A001 | 인증 필요 |

**참고**
- 프로필 이미지를 삭제하고 자동으로 기본 프로필 이미지로 복구
- 기존 커스텀 이미지는 S3에서 자동 삭제됨
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

이미지 파일을 AWS S3에 업로드

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
- 최대 크기: 50MB
- 허용 폴더: reviews, shops, profiles

**Response (201)**
```json
{
  "success": true,
  "data": {
    "url": "https://s3.ap-northeast-2.amazonaws.com/gotcha-bucket/reviews/abc123-def456.jpg",
    "originalFilename": "my-photo.jpg",
    "size": 1024000,
    "contentType": "image/jpeg"
  }
}
```

**Error Responses**
| 코드 | 상황 |
|------|------|
| FL001 | 파일이 비어있습니다 |
| FL002 | 파일 크기 초과 (50MB) |
| FL003 | 지원하지 않는 파일 형식 |
| FL004 | 파일 업로드 실패 |
| FL005 | 잘못된 폴더명 |

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

---

## 신고 API

### POST /reports

리뷰 또는 유저 신고

**Headers**
```
Authorization: Bearer {accessToken}
```

**Request Body**
```json
{
  "targetType": "REVIEW",
  "targetId": 1,
  "reason": "REVIEW_ABUSE",
  "detail": "욕설이 포함되어 있습니다"
}
```

**targetType 값**
| 값 | 설명 |
|---|------|
| REVIEW | 리뷰 신고 |
| SHOP | 가게 신고 |
| USER | 유저 신고 |

**reason 값** (targetType별로 해당 prefix 사유만 사용 가능)

*리뷰 신고 (REVIEW_*)*
| 값 | 설명 |
|---|------|
| REVIEW_SPAM | 도배/광고성 글이에요 |
| REVIEW_COPYRIGHT | 저작권을 침해해요 |
| REVIEW_DEFAMATION | 명예를 훼손하는 내용이에요 |
| REVIEW_ABUSE | 욕설이나 비방이 심해요 |
| REVIEW_VIOLENCE | 폭력적이거나 위협적인 내용이에요 |
| REVIEW_OBSCENE | 외설적인 내용이 포함돼있어요 |
| REVIEW_PRIVACY | 개인정보가 노출되어 있어요 |
| REVIEW_HATE_SPEECH | 혐오 표현이 포함돼있어요 |
| REVIEW_FALSE_INFO | 허위/거짓 정보예요 |
| REVIEW_OTHER | 기타 (detail 필수) |

*가게 신고 (SHOP_*)*
| 값 | 설명 |
|---|------|
| SHOP_WRONG_ADDRESS | 잘못된 주소예요 |
| SHOP_CLOSED | 영업 종료/폐업된 업체예요 |
| SHOP_INAPPROPRIATE | 부적절한 업체(불법/유해 업소)예요 |
| SHOP_DUPLICATE | 중복 제보된 업체예요 |
| SHOP_FALSE_INFO | 허위/거짓 정보예요 |
| SHOP_OTHER | 기타 (detail 필수) |

*사용자 신고 (USER_*)*
| 값 | 설명 |
|---|------|
| USER_INAPPROPRIATE_NICKNAME | 부적절한 닉네임이에요 |
| USER_INAPPROPRIATE_PROFILE | 부적절한 프로필 사진이에요 |
| USER_PRIVACY | 개인정보가 노출되어 있어요 |
| USER_IMPERSONATION | 다른 사람을 사칭하고 있어요 |
| USER_HATE_SPEECH | 혐오 표현이 포함돼있어요 |
| USER_OTHER | 기타 (detail 필수) |

**Validation**
| 필드 | 규칙 |
|------|------|
| targetType | 필수 |
| targetId | 필수 |
| reason | 필수, targetType과 prefix 일치 필요 |
| detail | reason이 *_OTHER일 때 필수 |

**Response (201)**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "targetType": "REVIEW",
    "targetId": 1,
    "reason": "REVIEW_ABUSE",
    "detail": "욕설이 포함되어 있습니다",
    "status": "PENDING",
    "createdAt": "2025-01-08T12:00:00"
  }
}
```

**Error Responses**
| 코드 | 상황 |
|------|------|
| RP002 | 이미 신고한 대상 |
| RP003 | 신고 대상을 찾을 수 없음 |
| RP004 | 본인을 신고할 수 없음 |
| RP009 | targetType과 reason prefix 불일치 |
| RP005 | 기타 사유 선택 시 상세 내용 필수 |

---

### GET /users/me/reports

본인 신고 목록 조회

**Headers**
```
Authorization: Bearer {accessToken}
```

**Response (200)**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "targetType": "REVIEW",
      "targetId": 1,
      "reason": "REVIEW_ABUSE",
      "detail": "욕설이 포함되어 있습니다",
      "status": "PENDING",
      "createdAt": "2025-01-08T12:00:00"
    }
  ]
}
```

---

### DELETE /reports/{reportId}

신고 취소 (PENDING 상태에서만 가능)

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
| RP001 | 신고를 찾을 수 없음 |
| RP006 | 본인의 신고만 취소 가능 |
| RP007 | 이미 처리된 신고는 취소 불가 |

---

## 관리자 API

### GET /admin/reports

신고 목록 조회 (관리자)

**Headers**
```
Authorization: Bearer {accessToken}
```

**Query Parameters**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| targetType | String | X | - | REVIEW, SHOP, USER |
| status | String | X | - | PENDING, ACCEPTED, REJECTED, CANCELLED |
| page | Integer | X | 0 | |
| size | Integer | X | 20 | |

**Response (200)**
```json
{
  "success": true,
  "data": {
    "reports": [
      {
        "id": 1,
        "reporterId": 1,
        "reporterNickname": "신고자#21",
        "targetType": "REVIEW",
        "targetId": 1,
        "reason": "REVIEW_ABUSE",
        "reasonDescription": "욕설이나 비방이 심해요",
        "detail": "욕설이 포함되어 있습니다",
        "status": "PENDING",
        "statusDescription": "처리 대기",
        "createdAt": "2025-01-08T12:00:00",
        "updatedAt": "2025-01-08T12:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "last": false
  }
}
```

**Error Responses**
| 코드 | 상황 |
|------|------|
| A002 | 관리자 권한 필요 |

---

### GET /admin/reports/{reportId}

신고 상세 조회 (관리자)

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
    "reporterId": 1,
    "reporterNickname": "신고자#21",
    "targetType": "REVIEW",
    "targetId": 1,
    "reason": "REVIEW_ABUSE",
    "reasonDescription": "욕설이나 비방이 심해요",
    "detail": "욕설이 포함되어 있습니다",
    "status": "PENDING",
    "statusDescription": "처리 대기",
    "createdAt": "2025-01-08T12:00:00",
    "updatedAt": "2025-01-08T12:00:00"
  }
}
```

**Error Responses**
| 코드 | 상황 |
|------|------|
| RP001 | 신고를 찾을 수 없음 |
| A002 | 관리자 권한 필요 |

---

### PATCH /admin/reports/{reportId}/status

신고 상태 변경 (관리자)

**Headers**
```
Authorization: Bearer {accessToken}
```

**Request Body**
```json
{
  "status": "ACCEPTED"
}
```

**status 값**
| 값 | 설명 |
|---|------|
| ACCEPTED | 승인 |
| REJECTED | 반려 |

**Response (200)**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "reporterId": 1,
    "reporterNickname": "신고자#21",
    "targetType": "REVIEW",
    "targetId": 1,
    "reason": "REVIEW_ABUSE",
    "reasonDescription": "욕설이나 비방이 심해요",
    "detail": "욕설이 포함되어 있습니다",
    "status": "ACCEPTED",
    "statusDescription": "승인",
    "createdAt": "2025-01-08T12:00:00",
    "updatedAt": "2025-01-08T14:00:00"
  }
}
```

**Error Responses**
| 코드 | 상황 |
|------|------|
| RP001 | 신고를 찾을 수 없음 |
| A002 | 관리자 권한 필요 |

---

### GET /admin/users

사용자 목록 조회 (관리자)

**Headers**
```
Authorization: Bearer {accessToken}
```

**Query Parameters**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| status | String | X | - | ACTIVE, SUSPENDED, BANNED |
| page | Integer | X | 0 | |
| size | Integer | X | 20 | |

**Response (200)**
```json
{
  "success": true,
  "data": {
    "users": [
      {
        "id": 1,
        "nickname": "빨간캡슐#21",
        "email": "user@example.com",
        "profileImageUrl": "https://...",
        "socialType": "KAKAO",
        "userType": "NORMAL",
        "status": "ACTIVE",
        "suspendedUntil": null,
        "lastLoginAt": "2026-01-08T12:00:00",
        "createdAt": "2025-12-01T10:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "last": false
  }
}
```

**Error Responses**
| 코드 | 상황 |
|------|------|
| A002 | 관리자 권한 필요 |

---

### GET /admin/users/{userId}

사용자 상세 조회 (관리자)

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
    "profileImageUrl": "https://...",
    "socialType": "KAKAO",
    "userType": "NORMAL",
    "status": "SUSPENDED",
    "suspendedUntil": "2026-02-15T12:00:00",
    "lastLoginAt": "2026-01-08T12:00:00",
    "createdAt": "2025-12-01T10:00:00"
  }
}
```

**Error Responses**
| 코드 | 상황 |
|------|------|
| U004 | 사용자를 찾을 수 없음 |
| A002 | 관리자 권한 필요 |

---

### PATCH /admin/users/{userId}/status

사용자 상태 변경 — 제재/해제 (관리자)

**Headers**
```
Authorization: Bearer {accessToken}
```

**Request Body**
```json
{
  "status": "SUSPENDED",
  "suspensionHours": 24
}
```

**status 값**
| 값 | 설명 | suspensionHours |
|---|------|-----------------|
| SUSPENDED | 기간 정지 | 필수 (1, 12, 24, 72, 120, 168, 336, 720) |
| BANNED | 영구 차단 | 불필요 |
| ACTIVE | 제재 해제 | 불필요 |

**Response (200)**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "nickname": "빨간캡슐#21",
    "email": "user@example.com",
    "profileImageUrl": "https://...",
    "socialType": "KAKAO",
    "userType": "NORMAL",
    "status": "SUSPENDED",
    "suspendedUntil": "2026-02-13T12:00:00",
    "lastLoginAt": "2026-01-08T12:00:00",
    "createdAt": "2025-12-01T10:00:00"
  }
}
```

**Error Responses**
| 코드 | 상황 |
|------|------|
| U004 | 사용자를 찾을 수 없음 |
| U006 | 허용되지 않는 정지 기간 |
| A002 | 관리자 권한 필요 |
