# k6 부하 테스트

GOTCHA Backend API 부하 테스트 스크립트 모음

## 설치

### Windows
```bash
# Chocolatey
choco install k6

# winget
winget install k6
```

### Mac
```bash
brew install k6
```

### Linux
```bash
sudo apt install k6
```

---

## 폴더 구조

```
k6/
├── config.js              # 공통 설정 (URL, 토큰, 좌표 등)
├── README.md              # 이 문서
├── scripts/
│   ├── shop-map.js        # 지도 조회 API 테스트
│   ├── shop-detail.js     # 가게 상세 API 테스트
│   ├── shop-search.js     # 가게 검색 API 테스트
│   ├── review-list.js     # 리뷰 목록 API 테스트
│   ├── favorite-toggle.js # 찜 추가/삭제 API 테스트 (인증 필요)
│   ├── user-me.js         # 내 정보 API 테스트 (인증 필요)
│   ├── user-flow.js       # 복합 시나리오 테스트
│   ├── all-apis.js        # 전체 API 통합 테스트
│   └── concurrency-test.js # 동시성 충돌 테스트 (인증 필요)
└── results/               # 테스트 결과 (자동 생성)
```

---

## 빠른 시작

### 1. 기본 실행 (로컬 서버)

```bash
# 지도 조회 API 테스트
k6 run k6/scripts/shop-map.js

# 가게 상세 API 테스트
k6 run k6/scripts/shop-detail.js

# 복합 시나리오 테스트
k6 run k6/scripts/user-flow.js

# 전체 API 통합 테스트
k6 run k6/scripts/all-apis.js
```

### 2. 환경 변수 설정

```bash
# 다른 서버 대상
k6 run -e BASE_URL=https://api.gotcha.com k6/scripts/shop-map.js

# 특정 가게 ID로 테스트
k6 run -e TEST_SHOP_ID=123 k6/scripts/shop-detail.js

# 인증 토큰 포함 (인증 필요 API)
k6 run -e ACCESS_TOKEN=your_jwt_token k6/scripts/user-me.js
```

### 3. 부하 강도 조절

```bash
# 가벼운 테스트 (기본값)
k6 run -e LOAD_TYPE=light k6/scripts/shop-map.js

# 중간 테스트
k6 run -e LOAD_TYPE=medium k6/scripts/shop-map.js

# 강한 테스트 (스트레스)
k6 run -e LOAD_TYPE=heavy k6/scripts/shop-map.js

# 스파이크 테스트
k6 run -e LOAD_TYPE=spike k6/scripts/shop-map.js
```

---

## 테스트 시나리오

### 단일 API 테스트

| 스크립트 | API | 설명 | 인증 |
|----------|-----|------|------|
| `shop-map.js` | GET /api/shops/map | 지도 영역 내 가게 목록 | X |
| `shop-detail.js` | GET /api/shops/{id} | 가게 상세 조회 | X |
| `shop-search.js` | GET /api/shops/search | 가게 검색 | X |
| `review-list.js` | GET /api/shops/{id}/reviews | 리뷰 목록 | X |
| `favorite-toggle.js` | POST/DELETE /api/shops/{id}/favorite | 찜 추가/삭제 | O |
| `user-me.js` | GET /api/users/me | 내 정보 조회 | O |

### 복합 시나리오 테스트

| 스크립트 | 설명 |
|----------|------|
| `user-flow.js` | 사용자 흐름: 지도 → 상세 → 리뷰 → 찜 |
| `all-apis.js` | 전체 API 혼합 (실제 트래픽 패턴 시뮬레이션) |

### 동시성 충돌 테스트

| 스크립트 | 설명 |
|----------|------|
| `concurrency-test.js` | Race Condition 및 동시성 이슈 검증 (인증 필요) |

---

## 부하 단계 설정

### light (개발 환경)
```
10초: 0 → 5명
30초: 5 → 10명
10초: 10 → 0명
```

### medium (중간)
```
30초: 0 → 20명
1분: 20 → 50명
30초: 50 → 0명
```

### heavy (스트레스 테스트)
```
1분: 0 → 50명
2분: 50 → 100명
1분: 100 → 200명
1분: 200 → 0명
```

### spike (스파이크 테스트)
```
10초: 0 → 10명
10초: 10 → 200명 (급증)
30초: 200명 유지
10초: 200 → 10명
10초: 10 → 0명
```

---

## 환경 변수 목록

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `BASE_URL` | 테스트 대상 서버 URL | `http://localhost:8080` |
| `ACCESS_TOKEN` | JWT 인증 토큰 | (없음) |
| `TEST_SHOP_ID` | 테스트용 가게 ID | `1` |
| `TEST_REVIEW_ID` | 테스트용 리뷰 ID | `1` |
| `LOAD_TYPE` | 부하 강도 (light/medium/heavy/spike) | `light` |

---

## 성능 임계값

기본 설정된 성능 기준:

| 메트릭 | 임계값 |
|--------|--------|
| 95% 응답 시간 | < 500ms |
| 99% 응답 시간 | < 1000ms |
| 에러율 | < 1% |
| 초당 요청 수 | > 10 RPS |

---

## 결과 확인

### 콘솔 출력
테스트 완료 후 자동으로 요약 결과가 출력됩니다.

### JSON 결과 파일
`k6/results/` 폴더에 JSON 형식으로 저장됩니다.

### HTML 리포트 생성 (선택)
```bash
k6 run --out json=result.json k6/scripts/shop-map.js
# 이후 k6-reporter 등으로 HTML 변환 가능
```

---

## 예시: 전체 테스트 실행

```bash
# 1. 로컬 서버 시작
./gradlew bootRun

# 2. 단일 API 테스트 (가벼운 부하)
k6 run k6/scripts/shop-map.js
k6 run k6/scripts/shop-detail.js
k6 run k6/scripts/shop-search.js

# 3. 복합 시나리오 테스트
k6 run k6/scripts/user-flow.js

# 4. 전체 API 통합 테스트 (중간 부하)
k6 run -e LOAD_TYPE=medium k6/scripts/all-apis.js

# 5. 스트레스 테스트 (주의: 서버 부하 큼)
k6 run -e LOAD_TYPE=heavy k6/scripts/all-apis.js
```

---

## 주의사항

1. **운영 서버 테스트 금지**: 운영 환경에서 부하 테스트 실행 시 서비스 장애 발생 가능
2. **인증 토큰 관리**: ACCESS_TOKEN은 환경 변수로 전달하고, 코드에 하드코딩하지 않음
3. **테스트 데이터**: TEST_SHOP_ID, TEST_REVIEW_ID가 실제 존재하는 데이터인지 확인
4. **네트워크**: 로컬 테스트와 원격 테스트의 네트워크 지연 차이 고려

---

## 동시성 충돌 테스트

### 목적
Race Condition 및 동시성 이슈를 검증합니다.
- 같은 사용자가 동시에 찜 추가 시도 시 중복 생성 여부
- 같은 사용자가 동시에 리뷰 좋아요 시도 시 중복 생성 여부
- 찜 추가/삭제 동시 실행 시 정합성

### 실행 방법

```bash
# 전체 동시성 테스트 (기본: burst 부하)
k6 run -e ACCESS_TOKEN=your_token k6/scripts/concurrency-test.js

# 찜 동시성만 테스트
k6 run -e ACCESS_TOKEN=your_token -e TEST_TYPE=favorite k6/scripts/concurrency-test.js

# 리뷰 좋아요 동시성만 테스트
k6 run -e ACCESS_TOKEN=your_token -e TEST_TYPE=review_like k6/scripts/concurrency-test.js

# 찜 토글 동시성 테스트
k6 run -e ACCESS_TOKEN=your_token -e TEST_TYPE=toggle k6/scripts/concurrency-test.js

# 부하 패턴 변경
k6 run -e ACCESS_TOKEN=your_token -e LOAD_TYPE=spike k6/scripts/concurrency-test.js
```

### 부하 패턴

| 패턴 | 설명 |
|------|------|
| `burst` (기본) | 5초 만에 50 VU 도달, 순간 집중 부하 |
| `ramp` | 점진적 증가 (20 → 50 → 100 VU) |
| `spike` | 2초 만에 10 → 100 VU 급증 |

### 결과 해석

| 메트릭 | 의미 | 정상 기준 |
|--------|------|-----------|
| `race_condition` | Race Condition 감지 횟수 | 0회 (이상적) |
| `duplicate_created` | 중복 데이터 생성 횟수 | 0회 (필수) |
| `conflict_responses` | 409 응답 횟수 | 정상 동시성 처리 |
| `unexpected_errors` | 예상치 못한 에러 | 0회 (이상적) |

### 문제 발견 시 조치

1. **Race Condition 감지됨**: 서비스 로직에 낙관적/비관적 락 적용 필요
2. **중복 생성됨**: DB 유니크 제약조건 추가 또는 트랜잭션 격리 수준 조정
3. **500 에러 다수 발생**: 동시성 예외 처리 로직 검토
