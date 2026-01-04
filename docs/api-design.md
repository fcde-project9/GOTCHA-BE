# API 설계

## Base URL

```
/api
```

---

## 인증

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | /auth/login/{provider} | 소셜 로그인 (kakao, google, naver, apple) |
| GET | /auth/nickname/random | 랜덤 닉네임 생성 |

---

## 가게 (Shop)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /shops | 주변 가챠샵 목록 |
| GET | /shops/{id} | 가게 상세 |
| POST | /shops/report | 가게 제보 |

### GET /shops 파라미터

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| lat | O | 위도 |
| lng | O | 경도 |
| radius | X | 반경 (m), 기본값 1000 |
| sort | X | 정렬 (distance 기본) |

### Response 예시

```json
{
  "success": true,
  "data": {
    "totalCount": 328,
    "shops": [
      {
        "id": 1,
        "name": "가챠샵",
        "address": "서울시 강남구...",
        "distance": 300,
        "isOpen": true,
        "isFavorite": false
      }
    ]
  }
}
```

---

## 찜 (Favorite)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /users/me/favorites | 내 찜 목록 |
| POST | /shops/{id}/favorite | 찜 추가 |
| DELETE | /shops/{id}/favorite | 찜 삭제 |

---

## 댓글/리뷰

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /shops/{id}/comments | 댓글 목록 |
| POST | /shops/{id}/comments | 댓글 작성 |
| GET | /shops/{id}/reviews | 리뷰 목록 |
| POST | /shops/{id}/reviews | 리뷰 작성 |

---

## 사용자

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /users/me | 내 정보 |
| PATCH | /users/me/nickname | 닉네임 수정 |
