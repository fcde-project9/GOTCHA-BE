# GOTCHA! – 가챠샵 지도 서비스

가챠샵(Gacha Shop)을 지도 기반으로 탐색하고,

리스트 및 상세 페이지를 통해 매장 정보를 확인할 수 있는 **모바일 웹 서비스**입니다.

---

## Tech Stack

### Frontend

- Next.js 14 (App Router)
- TypeScript
- Tailwind CSS
- Zustand
- TanStack Query
- Kakao Map SDK
- Vercel

### Backend

- Spring Boot 3.x
- Java 21
- Spring Data JPA
- PostgreSQL
- Swagger
- AWS EC2 / S3

---

## User Flow (v1)

1. 소셜 로그인
2. 메인 페이지 → 가챠샵 지도
3. 지도 핀 클릭 → 매장 미리보기
4. 하단 리스트 슬라이드
5. 매장 상세 페이지 진입