# 인증/권한 정책

## 인증 방식

### JWT 토큰

| 토큰 | 만료 시간 | 용도 |
|------|----------|------|
| Access Token | 1시간 | API 인증 |
| Refresh Token | 14일 | Access Token 재발급 |

### 토큰 구조

```json
// Access Token Payload
{
  "sub": "1",              // userId
  "nickname": "빨간캡슐#21",
  "socialType": "KAKAO",
  "iat": 1704067200,
  "exp": 1704070800
}
```

---

## 소셜 로그인 플로우

```
1. 프론트: 소셜 로그인 → Access Token 획득
2. 프론트: POST /api/auth/login/{provider} + Access Token
3. 백엔드: 소셜 API로 사용자 정보 조회
4. 백엔드:
   - 기존 회원 → 로그인 처리
   - 신규 회원 → 자동 회원가입 + 랜덤 닉네임 생성
5. 백엔드: JWT 토큰 발급
6. 프론트: 토큰 저장 후 API 요청에 사용
```

---

## API 권한 매트릭스

### Public (인증 불필요)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | /auth/login/{provider} | 소셜 로그인 |
| GET | /auth/nickname/random | 랜덤 닉네임 |
| GET | /shops | 가게 목록 |
| GET | /shops/{id} | 가게 상세 |
| GET | /shops/{id}/comments | 댓글 목록 |
| GET | /shops/{id}/reviews | 리뷰 목록 |

### Authenticated (로그인 필요)

| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| POST | /shops/report | 가게 제보 | 로그인 사용자 |
| GET | /users/me | 내 정보 | 본인 |
| PATCH | /users/me/nickname | 닉네임 수정 | 본인 |
| GET | /users/me/favorites | 내 찜 목록 | 본인 |
| POST | /shops/{id}/favorite | 찜 추가 | 로그인 사용자 |
| DELETE | /shops/{id}/favorite | 찜 삭제 | 본인 찜만 |
| POST | /shops/{id}/comments | 댓글 작성 | 로그인 사용자 |
| PUT | /shops/{id}/comments/{cid} | 댓글 수정 | 작성자 본인 |
| DELETE | /shops/{id}/comments/{cid} | 댓글 삭제 | 작성자 본인 |
| POST | /shops/{id}/reviews | 리뷰 작성 | 로그인 사용자 |
| PUT | /shops/{id}/reviews/{rid} | 리뷰 수정 | 작성자 본인 |
| DELETE | /shops/{id}/reviews/{rid} | 리뷰 삭제 | 작성자 본인 |

### Admin (관리자 전용) - 추후 구현

| Method | Endpoint | 설명 |
|--------|----------|------|
| DELETE | /admin/shops/{id} | 가게 삭제 |
| DELETE | /admin/comments/{id} | 댓글 강제 삭제 |
| DELETE | /admin/reviews/{id} | 리뷰 강제 삭제 |
| GET | /admin/reports | 신고 목록 |
| PATCH | /admin/reports/{id} | 신고 처리 |

---

## 권한 체크 로직

### 본인 확인

```java
// 댓글 수정/삭제 시
if (!comment.getUserId().equals(currentUserId)) {
    throw CommentException.unauthorized();
}

// 찜 삭제 시
if (!favorite.getUserId().equals(currentUserId)) {
    throw FavoriteException.unauthorized();
}
```

### 리소스 소유자 확인

```java
// isOwner 필드 계산
response.setIsOwner(
    currentUserId != null &&
    resource.getUserId().equals(currentUserId)
);
```

---

## Security 설정

### Spring Security 설정

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public
                .requestMatchers(GET, "/api/shops/**").permitAll()
                .requestMatchers(POST, "/api/auth/**").permitAll()
                .requestMatchers(GET, "/api/auth/**").permitAll()
                // Authenticated
                .requestMatchers("/api/users/**").authenticated()
                .requestMatchers(POST, "/api/shops/report").authenticated()
                .requestMatchers(POST, "/api/shops/*/favorite").authenticated()
                .requestMatchers(DELETE, "/api/shops/*/favorite").authenticated()
                .requestMatchers(POST, "/api/shops/*/comments").authenticated()
                .requestMatchers(POST, "/api/shops/*/reviews").authenticated()
                // Admin
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

---

## 에러 응답

### 인증 실패 (401 Unauthorized)

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A001",
    "message": "로그인이 필요합니다"
  }
}
```

### 권한 부족 (403 Forbidden)

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A002",
    "message": "권한이 없습니다"
  }
}
```

### 토큰 만료 (401 Unauthorized)

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A003",
    "message": "토큰이 만료되었습니다"
  }
}
```

---

## 토큰 재발급

### POST /auth/refresh

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
    "accessToken": "새 액세스 토큰",
    "refreshToken": "새 리프레시 토큰"
  }
}
```
