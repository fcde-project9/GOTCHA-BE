# GOTCHA Backend

가챠샵 지도 탐색 서비스 백엔드

## 기술 스택

- Java 21 / Spring Boot 3.5.9
- Spring Data JPA / PostgreSQL
- Gradle 8.x
- Lombok

## 빌드 & 실행

- 빌드: `./gradlew build`
- 실행: `./gradlew bootRun`
- 테스트: `./gradlew test`

## 패키지 구조 (도메인형)

```
com.gotcha
├── _global/                # 정렬 시 최상단
│   ├── common/
│   │   └── enums/
│   ├── config/
│   ├── controller/
│   ├── exception/
│   └── util/
├── domain/{feature}/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   │   ├── request/
│   │   └── response/
│   └── exception/
└── GotchaServerApplication.java
```

## 코딩 컨벤션

- 포맷팅: IntelliJ 기본 포맷터 (Option+Command+L)
- 들여쓰기: 4 spaces
- 줄 길이: 120자 권장
- 네이밍
  - 클래스: PascalCase
  - 메서드/변수: camelCase
  - 상수: UPPER_SNAKE_CASE
- DTO: `*Request`, `*Response` 접미사
- Entity: 접미사 없음

## API 응답 형식

```json
{
  "success": true,
  "data": { },
  "error": null
}
```

- 성공: `success: true`, `data`에 결과
- 실패: `success: false`, `error`에 코드/메시지

## Git 컨벤션

- 브랜치 네이밍: `feature/`, `fix/`, `refactor/`, `chore/`
- PR 대상 브랜치: `dev`

## PR 전 체크리스트

- [ ] 브랜치 이름 규칙 확인 (feature/, fix/ 등)
- [ ] PR 대상 브랜치 확인 (dev)
- [ ] 최신 dev 반영 완료 (merge or rebase)
- [ ] 코드 자동 정렬 실행 (spotlessApply) & 불필요한 import 제거
