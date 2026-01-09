# Entity 설계

## 개요

- **MVP (V1)**: users, refresh_tokens, shops, shop_reports, favorites, comments, reviews, withdrawal_surveys
- **V2**: post_types, posts, post_comments, chat_rooms, chats, inquiries

---

## users

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | |
| social_type | Enum | KAKAO, GOOGLE, NAVER (탈퇴 시 null) |
| social_id | String | 소셜 제공자 ID (탈퇴 시 null) |
| nickname | String | ex: 빨간캡슐#21 (탈퇴 시 "탈퇴한 사용자_{id}") |
| email | String | 소셜 이메일 (탈퇴 시 null) |
| profile_image_url | String | 프로필 이미지 URL (탈퇴 시 null) |
| is_anonymous | Boolean | 게스트 여부 |
| is_deleted | Boolean | 탈퇴 여부 (soft delete) |
| last_login_at | LocalDateTime | |
| created_at, updated_at | LocalDateTime | BaseTimeEntity |

**탈퇴 처리**
- soft delete: is_deleted = true
- 개인정보 마스킹: email, profile_image_url = null
- 소셜 연동 해제: social_type, social_id = null (재가입 허용)
- 닉네임 변경: "탈퇴한 사용자_{id}"

---

## refresh_tokens

리프레시 토큰 저장 (로그아웃 시 무효화용)

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | |
| user_id | Long (FK → users) | |
| token | String (unique) | 리프레시 토큰 값 |
| expires_at | LocalDateTime | 만료 시간 |
| created_at, updated_at | LocalDateTime | BaseTimeEntity |

---

## shops

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | |
| name | String | 가게명 |
| address_name | String | 전체 주소 (카카오 API 자동 변환) |
| latitude | Double | 위도 |
| longitude | Double | 경도 |
| main_image_url | String | 대표 이미지 (필수) |
| location_hint | String | 찾아가는 힌트 |
| open_time | String | 영업시간 `"HH:mm-HH:mm"` (예: "10:00-22:00") |
| region_1depth_name | String | 시/도 (예: 서울) |
| region_2depth_name | String | 구/군 (예: 강남구) |
| region_3depth_name | String | 동 (예: 신사동) |
| main_address_no | String | 지번 본번 |
| sub_address_no | String | 지번 부번 |
| created_by | Long (FK → users) | 제보자 |
| created_at, updated_at | LocalDateTime | |

---

## shop_reports

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | |
| shop_id | Long (FK) | |
| reporter_id | Long (FK) | |
| report_title | String | 제보 유형 (new, update, duplicate) |
| report_content | String | |
| is_anonymous | Boolean | |
| created_at, updated_at | LocalDateTime | |

---

## favorites

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | |
| user_id | Long (FK) | |
| shop_id | Long (FK) | |
| created_at, updated_at | LocalDateTime | |

- UNIQUE(user_id, shop_id)

---

## comments

댓글 (가벼운 코멘트)

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | |
| shop_id | Long (FK) | |
| user_id | Long (FK) | |
| content | String | |
| is_anonymous | Boolean | |
| created_at, updated_at | LocalDateTime | |

---

## reviews

리뷰/후기/인증

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | |
| shop_id | Long (FK) | |
| user_id | Long (FK) | |
| content | String | |
| created_at, updated_at | LocalDateTime | |

---

## review_images

리뷰 이미지 (리뷰당 최대 10개)

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | |
| review_id | Long (FK → reviews) | |
| image_url | String | GCS 이미지 URL |
| display_order | Integer | 표시 순서 (0부터 시작) |
| created_at, updated_at | LocalDateTime | |

---

## withdrawal_surveys

회원 탈퇴 설문

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | |
| user_id | Long (FK) | |
| reasons | JSON (text) | 탈퇴 사유 목록 (복수 선택 가능, JSON 배열로 저장) |
| detail | String | 기타 사유 상세 |
| created_at, updated_at | LocalDateTime | BaseTimeEntity |

### WithdrawalReason (Enum)

| 값 | 설명 |
|----|------|
| LOW_USAGE | 사용을 잘 안하게 돼요 |
| INSUFFICIENT_INFO | 가챠샵 정보가 부족해요 |
| INACCURATE_INFO | 가챠샵 정보가 기재된 내용과 달라요 |
| PRIVACY_CONCERN | 개인정보 보호를 위해 삭제할래요 |
| HAS_OTHER_ACCOUNT | 다른 계정이 있어요 |
| OTHER | 기타 |

---

## user_permissions

사용자 권한 동의 상태 (최신 상태만 저장)

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | AUTO_INCREMENT |
| user_id | Long (FK) | 사용자 ID |
| permission_type | Enum | LOCATION, CAMERA, ALBUM |
| is_agreed | Boolean | 동의 여부 |
| agreed_at | LocalDateTime | 동의한 시간 (nullable) |
| created_at, updated_at | LocalDateTime | BaseTimeEntity |

- UNIQUE(user_id, permission_type): 중복 방지

---

## user_permission_histories

사용자 권한 변경 이력 (법적 증빙용)

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | AUTO_INCREMENT |
| user_id | Long | FK (직접 저장) |
| permission_type | Enum | LOCATION, CAMERA, ALBUM |
| is_agreed | Boolean | 동의 여부 |
| changed_at | LocalDateTime | 변경 시간 |
| device_info | String (200) | User-Agent (nullable) |

---

# V2 Entity (Phase 2)

---

## post_types

게시글 타입 (자유게시판, 공지사항 등)

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | |
| type_name | String | 타입명 |
| description | String | 설명 |

---

## posts

커뮤니티 게시글

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | |
| user_id | Long (FK) | 작성자 |
| type_id | Long (FK → post_types) | 게시글 타입 |
| title | String | 제목 |
| content | String | 내용 |
| post_image_url | String | 이미지 |
| created_at, updated_at | LocalDateTime | |

---

## post_comments

게시글 댓글 (대댓글 지원)

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | |
| post_id | Long (FK) | |
| user_id | Long (FK) | |
| parent_id | Long (FK → self) | 대댓글인 경우 부모 댓글 |
| content | String | |
| is_anonymous | Boolean | |
| created_at, updated_at | LocalDateTime | |

---

## chat_rooms

1:1 채팅방

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | |
| user1_id | Long (FK) | 참여자1 |
| user2_id | Long (FK) | 참여자2 |
| last_message_at | LocalDateTime | 마지막 메시지 시간 |
| created_at, updated_at | LocalDateTime | |

---

## chats

채팅 메시지

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | |
| chat_room_id | Long (FK) | |
| sender_id | Long (FK) | 발신자 |
| content | String | |
| created_at, updated_at | LocalDateTime | |

---

## inquiries

1:1 문의

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | |
| user_id | Long (FK) | |
| content | String | |
| status | Enum | OPEN, PENDING, CLOSED |
| created_at, updated_at | LocalDateTime | |
