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

### GET /shops/{shopId}

가게 상세 조회

**Path Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| shopId | Long | O | 가게 ID |

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| lat | Double | X | 거리 계산용 위도 |
| lng | Double | X | 거리 계산용 경도 |

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
    "openTime": {
      "mon": "10:00-22:00",
      "tue": "10:00-22:00",
      "wed": "10:00-22:00",
      "thu": "10:00-22:00",
      "fri": "10:00-23:00",
      "sat": "10:00-23:00",
      "sun": "12:00-20:00"
    },
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
  "address": "서울시 마포구 홍대동 123-45",
  "latitude": 37.5563,
  "longitude": 126.9236,
  "locationHint": "홍대입구역 9번 출구 앞",
  "openTime": {
    "mon": "10:00-22:00",
    "tue": "10:00-22:00"
  },
  "isAnonymous": false
}
```

**Validation**
| 필드 | 규칙 |
|------|------|
| name | 필수, 2-100자 |
| address | 필수 |
| latitude | 필수, -90 ~ 90 |
| longitude | 필수, -180 ~ 180 |
| locationHint | 선택, 최대 500자 |

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

**Response (200)**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "content": "원하는 캐릭터 뽑았어요!",
        "imageUrl": "https://...",
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
  "imageUrl": "https://..."
}
```

**Validation**
| 필드 | 규칙 |
|------|------|
| content | 필수, 10-1000자 |
| imageUrl | 선택 |

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
    "profileImageUrl": "https://...",
    "socialType": "KAKAO",
    "favoriteCount": 5,
    "commentCount": 10,
    "reviewCount": 3,
    "createdAt": "2025-01-01T10:00:00"
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
| nickname | 필수, 2-20자, 특수문자 제한 |

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
