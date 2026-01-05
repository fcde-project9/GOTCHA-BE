# Entity 설계

## 개요

- **MVP (V1)**: users, shops, shop_reports, favorites, comments, reviews
- **V2**: post_types, posts, post_comments, chat_rooms, chats, inquiries

---

## users

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | |
| social_type | Enum | KAKAO, GOOGLE, NAVER |
| social_id | String | 소셜 제공자 ID |
| nickname | String | ex: 빨간캡슐#21 |
| profile_image_url | String | |
| is_anonymous | Boolean | 게스트 여부 |
| last_login_at | LocalDateTime | |
| created_at, updated_at | LocalDateTime | BaseTimeEntity |

---

## shops

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | |
| name | String | 가게명 |
| address | String | 주소 |
| latitude | Double | 위도 |
| longitude | Double | 경도 |
| main_image_url | String | 대표 이미지 |
| location_hint | String | 찾아가는 힌트 |
| open_time | JSON (jsonb) | `{"mon": "10:00-22:00", ...}` |
| region | String | 시/도 |
| district | String | 구/군 |
| neighborhood | String | 동 |
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

리뷰/후기/인증 (이미지 포함 가능)

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | |
| shop_id | Long (FK) | |
| user_id | Long (FK) | |
| content | String | |
| image_url | String | 인증 이미지 |
| created_at, updated_at | LocalDateTime | |

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
