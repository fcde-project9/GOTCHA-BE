# 프로덕션 배포 전 보안 체크리스트

## MVP 완료 후

### HTTP-only 쿠키 방식 변경
- [ ] `OAuth2AuthenticationSuccessHandler` 쿠키 설정 코드 작성
- [ ] 프론트엔드 `credentials: 'include'` 설정 협의
- [ ] 로그아웃 API 구현 (쿠키 삭제)
- [ ] 통합 테스트

**관련 파일:** `OAuth2AuthenticationSuccessHandler.java`

**현재 문제:**
- 쿼리 파라미터로 토큰 전달 → 브라우저 히스토리/서버 로그에 노출

---

## 베타 테스트 전

### 리다이렉트 URI 화이트리스트
- [ ] 프로덕션 도메인 확정
- [ ] 허용 URI 목록 작성
- [ ] 화이트리스트 검증 로직 구현
- [ ] 환경별 설정 분리 (local/dev/prod)

**관련 파일:** `OAuth2AuthenticationSuccessHandler.java`

**현재 문제:**
- 리다이렉트 URI 검증 없음 → 오픈 리다이렉트 취약점

---

## 프로덕션 배포 직전

### SecurityConfig 보안 강화
- [ ] `anyRequest().permitAll()` → `anyRequest().authenticated()` 변경
- [ ] 전체 API 엔드포인트 권한 설정 검토
- [ ] 공개/인증 필요 API 분류 완료 확인

**관련 파일:** `SecurityConfig.java`

**현재 문제:**
- 명시되지 않은 모든 엔드포인트가 인증 없이 접근 가능

---

## 완료된 항목

- [x] JWT payload에 민감정보 미포함 확인
- [x] CORS 설정 환경변수화
- [x] JwtAuthenticationEntryPoint 타입 안전성 개선

---

## 참고

### TODO 주석 위치
- `SecurityConfig.java:72-74`
- `OAuth2AuthenticationSuccessHandler.java:21-29`
