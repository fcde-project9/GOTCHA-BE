# Repository Edge Cases

Repository 메서드별 엣지 케이스 정리

---

## UserRepository

| 메서드 | 엣지 케이스 | 예상 동작 | 테스트 |
|--------|------------|----------|--------|
| `findBySocialTypeAndSocialId` | 존재하지 않는 socialId | Empty Optional | O |
| `findBySocialTypeAndSocialId` | 동일 socialId, 다른 socialType | Empty Optional | O |
| `findByNickname` | 존재하지 않는 닉네임 | Empty Optional | O |
| `findByNickname` | 빈 문자열 닉네임 | Empty Optional | O |
| `existsByNickname` | 존재하지 않는 닉네임 | false | O |

### 고려사항
- socialType + socialId 조합이 유니크 제약조건으로 설정됨
- 동일 socialId라도 socialType이 다르면 다른 사용자로 취급

---

## ShopRepository

| 메서드 | 엣지 케이스 | 예상 동작 | 테스트 |
|--------|------------|----------|--------|
| `findByIdWithCreator` | 존재하지 않는 ID | Empty Optional | O |
| `findNearbyShops` | 반경 0 | 빈 리스트 | O |
| `findNearbyShops` | 범위 밖 샵만 존재 | 빈 리스트 | O |
| `findNearbyShops` | 샵 데이터 없음 | 빈 리스트 | O |
| `findNearbyShops` | 여러 샵이 반경 내 | 모든 샵 반환 | O |

### 고려사항
- Haversine 공식 사용하여 거리 계산
- 반경(radius)은 km 단위
- 경계값(정확히 반경 위치)은 `<` 조건으로 제외됨

### 주의사항
- 음수 반경은 현재 검증하지 않음 (Service 레이어에서 처리 필요)
- 위도/경도 범위 검증 없음 (클라이언트/Service에서 처리 필요)

---

## FavoriteRepository

| 메서드 | 엣지 케이스 | 예상 동작 | 테스트 |
|--------|------------|----------|--------|
| `findByUserIdAndShopId` | 즐겨찾기 없음 | Empty Optional | O |
| `findAllByUserId` | 즐겨찾기 없는 사용자 | 빈 리스트 | O |
| `findAllByUserId` | 존재하지 않는 사용자 ID | 빈 리스트 | O |
| `countByShopId` | 즐겨찾기 없는 샵 | 0 | O |
| `deleteByUserIdAndShopId` | 존재하지 않는 즐겨찾기 | 에러 없이 진행 | O |

### 고려사항
- user_id + shop_id 조합이 유니크 제약조건
- 동일 사용자가 같은 샵을 중복 즐겨찾기 불가

### 주의사항
- `deleteByUserIdAndShopId`는 `@Transactional` 필요 (Service 레이어에서)

---

## CommentRepository

| 메서드 | 엣지 케이스 | 예상 동작 | 테스트 |
|--------|------------|----------|--------|
| `findAllByShopIdOrderByCreatedAtDesc` | 댓글 없는 샵 | 빈 페이지 | O |
| `findAllByShopIdOrderByCreatedAtDesc` | 존재하지 않는 샵 ID | 빈 페이지 | O |
| `findAllByShopIdOrderByCreatedAtDesc` | 페이지 범위 초과 | 빈 content, totalElements 유지 | O |
| `findAllByShopIdOrderByCreatedAtDesc` | 큰 페이지 크기 | 모든 데이터 반환 | O |

### 고려사항
- 페이지네이션 시 createdAt 기준 내림차순 정렬
- 페이지 범위 초과 시에도 totalElements는 정확히 반환

---

## ReviewRepository

| 메서드 | 엣지 케이스 | 예상 동작 | 테스트 |
|--------|------------|----------|--------|
| `findAllByShopIdOrderByCreatedAtDesc` | 리뷰 없는 샵 | 빈 페이지 | O |
| `findAllByShopIdOrderByCreatedAtDesc` | 존재하지 않는 샵 ID | 빈 페이지 | O |
| `findAllByShopIdOrderByCreatedAtDesc` | 페이지 범위 초과 | 빈 content, totalElements 유지 | O |
| `existsByUserIdAndShopId` | 존재하지 않는 사용자/샵 | false | O |
| `existsByUserIdAndShopId` | 같은 사용자, 다른 샵 | 각각 확인 필요 | O |

### 고려사항
- 한 사용자는 한 샵에 하나의 리뷰만 작성 가능 (비즈니스 규칙)
- 같은 사용자가 여러 샵에 리뷰 작성은 가능

### 참고
- 현재 Review Entity에 rating 필드 없음
- `getAverageRatingByShopId`는 rating 필드 추가 후 구현 필요

---

## ShopReportRepository

| 메서드 | 엣지 케이스 | 예상 동작 | 테스트 |
|--------|------------|----------|--------|
| `findById` | 존재하지 않는 ID | Empty Optional | O |
| `save` | isAnonymous = null | false로 저장 | O |
| `save` | reportContent = null | null로 저장 | O |
| `save` | reportContent = "" | 빈 문자열로 저장 | O |

### 고려사항
- 기본 CRUD만 사용
- isAnonymous 기본값은 Entity Builder에서 처리 (null -> false)
- reportContent는 nullable

---

## 공통 주의사항

### Service 레이어에서 처리해야 할 사항
1. **입력값 검증**: null, 음수, 범위 초과 등
2. **비즈니스 규칙 검증**: 중복 등록, 권한 확인 등
3. **트랜잭션 관리**: delete 메서드에 `@Transactional` 필요

### 테스트 환경
- Testcontainers 사용 (PostgreSQL)
- `@DataJpaTest`로 Repository 레이어만 테스트

---

*최종 업데이트: 2026-01-05*
