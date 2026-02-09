# 인증/권한 정책

## 인증 방식

### JWT 토큰

| 토큰 | 만료 시간 | 용도 | 저장 위치 |
|------|----------|------|----------|
| Access Token | 15분 | API 인증 | 클라이언트 |
| Refresh Token | 7일 | Access Token 재발급 | DB + 클라이언트 |

### Refresh Token 관리

- **DB 저장**: 로그인 성공 시 `refresh_tokens` 테이블에 저장
- **로그아웃**: DB에서 해당 사용자의 Refresh Token 삭제
- **재발급**: 기존 토큰 삭제 후 새 토큰 저장 (Rotation 방식)

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

### OAuth2 Login (Spring Security OAuth2 Client)

```text
1. 프론트: /oauth2/authorize/{provider} 호출
2. 백엔드: 소셜 로그인 페이지로 리다이렉트
3. 사용자: 소셜 로그인 진행
4. 백엔드: /api/auth/callback/{provider}로 콜백 수신
5. 백엔드: CustomOAuth2UserService에서 사용자 정보 처리
   - 기존 회원 → 프로필 업데이트 + 마지막 로그인 시간 갱신
   - 신규 회원 → 자동 회원가입 + 랜덤 닉네임 생성
6. 백엔드: OAuth2AuthenticationSuccessHandler에서 JWT 발급
7. 백엔드: 프론트엔드 콜백 URL로 리다이렉트 (토큰 쿼리 파라미터)
8. 프론트: 토큰 저장 후 API 요청에 사용
```

### 지원 소셜 로그인

| Provider | 엔드포인트 | scope |
|----------|-----------|-------|
| 카카오 | /oauth2/authorize/kakao | profile_nickname, account_email |
| 구글 | /oauth2/authorize/google | profile, email |
| 네이버 | /oauth2/authorize/naver | name, email, profile_image |

### OAuth2 관련 클래스

| 클래스 | 설명 |
|--------|------|
| CustomOAuth2UserService | OAuth2 로그인 처리, 사용자 생성/조회 |
| CustomOAuth2User | OAuth2User 구현체, 인증된 사용자 정보 |
| OAuth2UserInfo | 소셜별 사용자 정보 추상화 |
| OAuth2UserInfoFactory | 소셜별 OAuth2UserInfo 생성 |
| OAuth2AuthenticationSuccessHandler | 로그인 성공 시 JWT 발급 및 리다이렉트 |
| OAuth2AuthenticationFailureHandler | 로그인 실패 시 에러 리다이렉트 |

### 환경변수

```bash
# 카카오
KAKAO_CLIENT_ID=카카오 앱 REST API 키
KAKAO_CLIENT_SECRET=카카오 앱 시크릿 키

# 구글
GOOGLE_CLIENT_ID=구글 OAuth 2.0 클라이언트 ID
GOOGLE_CLIENT_SECRET=구글 OAuth 2.0 클라이언트 시크릿

# 네이버
NAVER_CLIENT_ID=네이버 앱 Client ID
NAVER_CLIENT_SECRET=네이버 앱 Client Secret

# 리다이렉트 URI (로그인 성공 후 프론트엔드 콜백)
OAUTH2_REDIRECT_URI=http://localhost:3000/oauth/callback
```

---

## API 권한 매트릭스

### Public (인증 불필요)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /oauth2/authorize/{provider} | OAuth2 소셜 로그인 시작 |
| GET | /api/auth/callback/{provider} | OAuth2 콜백 |
| POST | /auth/login/{provider} | 소셜 로그인 (레거시) |
| GET | /auth/nickname/random | 랜덤 닉네임 |
| GET | /shops | 가게 목록 |
| GET | /shops/{id} | 가게 상세 |
| GET | /shops/{id}/comments | 댓글 목록 |
| GET | /shops/{id}/reviews | 리뷰 목록 |

### Authenticated (로그인 필요)

| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| POST | /shops/report | 가게 제보 | 로그인 사용자 |
| PUT | /shops/{id} | 가게 수정 | ADMIN |
| PATCH | /shops/{id}/main-image | 가게 대표 이미지 수정 | ADMIN |
| DELETE | /shops/{id} | 가게 삭제 | ADMIN |
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
| DELETE | /shops/{id}/reviews/{rid} | 리뷰 삭제 | 작성자 본인 또는 ADMIN |
| POST | /shops/reviews/{rid}/like | 리뷰 좋아요 | 로그인 사용자 |
| DELETE | /shops/reviews/{rid}/like | 리뷰 좋아요 취소 | 본인 좋아요만 |

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

### POST /auth/reissue

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

## 로그아웃

### POST /auth/logout

로그아웃 시 서버에서 Refresh Token을 삭제합니다.
Access Token은 Stateless이므로 즉시 무효화되지 않으며, 만료될 때까지 유효합니다.

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
