# API 설계

## Base URL

```
/api
```

---

## 인증

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | /auth/login/{provider} | 소셜 로그인 (kakao, google, naver) |
| POST | /auth/logout | 로그아웃 |
| POST | /auth/reissue | 토큰 재발급 |
| GET | /auth/nickname/random | 랜덤 닉네임 생성 |
| GET | /auth/nickname/check | 닉네임 중복/유효성 체크 |

---

## 가게 (Shop)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /shops/map | 지도 영역 내 가게 목록 |
| GET | /shops/search | 가게 이름 검색 |
| GET | /shops/nearby | 50m 내 가게 목록 (제보 전 중복 체크) |
| GET | /shops/{id} | 가게 상세 |
| POST | /shops/save | 가게 제보 |

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
    "content": [
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
| PUT | /shops/{id}/reviews/{reviewId} | 리뷰 수정 |
| DELETE | /shops/{id}/reviews/{reviewId} | 리뷰 삭제 |
| POST | /shops/reviews/{reviewId}/like | 리뷰 좋아요 |
| DELETE | /shops/reviews/{reviewId}/like | 리뷰 좋아요 취소 |

---

## 사용자

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /users/me | 내 정보 |
| GET | /users/me/nickname | 닉네임 조회 |
| PATCH | /users/me/nickname | 닉네임 수정 |
| PATCH | /users/me/profile-image | 프로필 이미지 변경 |
| DELETE | /users/me/profile-image | 프로필 이미지 삭제 |
| GET | /users/me/shops | 내가 제보한 가게 목록 |
| DELETE | /users/me | 회원 탈퇴 (설문 포함) |

---

## 권한 (Permission)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /users/permissions/{type} | 권한 동의 여부 확인 |
| POST | /users/permissions | 권한 동의 상태 업데이트 |

---

## 파일

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | /files/upload | 이미지 업로드 (GCS) |
