/**
 * 동시성 충돌 테스트
 *
 * 목적: Race Condition 및 동시성 이슈 검증
 * - 같은 사용자가 동시에 찜 추가 시도 (중복 생성 여부)
 * - 같은 사용자가 동시에 리뷰 좋아요 시도
 * - 찜 추가/삭제 동시 실행
 *
 * 실행:
 *   k6 run -e ACCESS_TOKEN=your_token k6/scripts/concurrency-test.js
 *   k6 run -e ACCESS_TOKEN=your_token -e TEST_TYPE=favorite k6/scripts/concurrency-test.js
 *   k6 run -e ACCESS_TOKEN=your_token -e TEST_TYPE=review_like k6/scripts/concurrency-test.js
 *
 * 주의: 이 테스트는 동일한 ACCESS_TOKEN으로 여러 VU가 동시 접근하여
 *      Race Condition을 유발합니다. 테스트 환경에서만 실행하세요.
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';
import { CONFIG, getHeaders } from '../config.js';

// ===== 메트릭 =====
const duplicateCreated = new Counter('duplicate_created');       // 중복 생성 횟수
const raceConditionDetected = new Counter('race_condition');     // Race Condition 감지
const conflictResponses = new Counter('conflict_responses');     // 409 응답 횟수
const unexpectedErrors = new Counter('unexpected_errors');       // 예상치 못한 에러
const successRate = new Rate('success_rate');
const responseDuration = new Trend('response_duration');

// ===== 테스트 설정 =====
const TEST_TYPE = __ENV.TEST_TYPE || 'all';  // favorite, review_like, toggle, all

// 동시성 테스트용 부하 설정 (짧고 강하게)
const CONCURRENCY_STAGES = {
    // 순간 집중 부하: 동시에 많은 요청 발생
    burst: [
        { duration: '5s', target: 50 },   // 5초 만에 50 VU
        { duration: '10s', target: 50 },  // 50 VU 유지
        { duration: '5s', target: 0 },    // 종료
    ],
    // 점진적 증가
    ramp: [
        { duration: '10s', target: 20 },
        { duration: '20s', target: 50 },
        { duration: '10s', target: 100 },
        { duration: '10s', target: 0 },
    ],
    // 스파이크
    spike: [
        { duration: '5s', target: 10 },
        { duration: '2s', target: 100 },  // 급격한 증가
        { duration: '10s', target: 100 },
        { duration: '5s', target: 0 },
    ],
};

export const options = {
    stages: CONCURRENCY_STAGES[__ENV.LOAD_TYPE || 'burst'],
    thresholds: {
        // 동시성 테스트에서는 일부 실패 허용
        http_req_failed: ['rate<0.3'],           // 30% 미만 실패
        success_rate: ['rate>0.5'],              // 50% 이상 성공
        race_condition: ['count<10'],            // Race Condition 10회 미만
        duplicate_created: ['count==0'],         // 중복 생성 0회 (이상적)
    },
};

// ===== 헬퍼 함수 =====
function getAuthHeaders() {
    if (!CONFIG.ACCESS_TOKEN) {
        throw new Error('ACCESS_TOKEN is required for concurrency tests');
    }
    return getHeaders(true);
}

// ===== 테스트 시나리오 =====

/**
 * 찜 동시 추가 테스트
 * 같은 사용자가 같은 가게에 동시에 찜 추가 시도
 */
function testFavoriteConcurrency() {
    const shopId = CONFIG.TEST_SHOP_ID;
    const url = `${CONFIG.BASE_URL}/api/shops/${shopId}/favorite`;

    // 찜 추가 시도
    const addRes = http.post(url, null, {
        headers: getAuthHeaders(),
        tags: { name: 'Concurrency_FavoriteAdd' },
    });

    responseDuration.add(addRes.timings.duration);

    const status = addRes.status;

    if (status === 200) {
        // 성공: 정상적으로 찜 추가됨
        successRate.add(1);

        // 즉시 삭제하여 다음 테스트 준비
        sleep(0.1);
        http.del(url, null, {
            headers: getAuthHeaders(),
            tags: { name: 'Concurrency_FavoriteCleanup' },
        });
    } else if (status === 409) {
        // 이미 찜됨: 다른 VU가 먼저 추가함 (정상적인 동시성 처리)
        conflictResponses.add(1);
        successRate.add(1);  // 정상 처리로 간주
    } else if (status === 500) {
        // 서버 에러: Race Condition으로 인한 중복 키 에러 가능성
        raceConditionDetected.add(1);
        successRate.add(0);

        // 에러 내용 확인
        try {
            const body = JSON.parse(addRes.body);
            if (body.message && body.message.includes('duplicate')) {
                duplicateCreated.add(1);
            }
        } catch (e) {
            // ignore
        }
    } else {
        unexpectedErrors.add(1);
        successRate.add(0);
    }

    return status;
}

/**
 * 리뷰 좋아요 동시 추가 테스트
 * 같은 사용자가 같은 리뷰에 동시에 좋아요 시도
 */
function testReviewLikeConcurrency() {
    const reviewId = CONFIG.TEST_REVIEW_ID;
    const url = `${CONFIG.BASE_URL}/api/reviews/${reviewId}/like`;

    // 좋아요 추가 시도
    const addRes = http.post(url, null, {
        headers: getAuthHeaders(),
        tags: { name: 'Concurrency_ReviewLikeAdd' },
    });

    responseDuration.add(addRes.timings.duration);

    const status = addRes.status;

    if (status === 200) {
        successRate.add(1);

        // 즉시 삭제
        sleep(0.1);
        http.del(url, null, {
            headers: getAuthHeaders(),
            tags: { name: 'Concurrency_ReviewLikeCleanup' },
        });
    } else if (status === 409) {
        conflictResponses.add(1);
        successRate.add(1);
    } else if (status === 500) {
        raceConditionDetected.add(1);
        successRate.add(0);

        try {
            const body = JSON.parse(addRes.body);
            if (body.message && body.message.includes('duplicate')) {
                duplicateCreated.add(1);
            }
        } catch (e) {
            // ignore
        }
    } else {
        unexpectedErrors.add(1);
        successRate.add(0);
    }

    return status;
}

/**
 * 찜 토글 동시성 테스트
 * 추가와 삭제가 동시에 발생하는 상황 시뮬레이션
 */
function testFavoriteToggleConcurrency() {
    const shopId = CONFIG.TEST_SHOP_ID;
    const url = `${CONFIG.BASE_URL}/api/shops/${shopId}/favorite`;

    // 랜덤하게 추가 또는 삭제 실행
    const action = Math.random() < 0.5 ? 'add' : 'delete';

    let res;
    if (action === 'add') {
        res = http.post(url, null, {
            headers: getAuthHeaders(),
            tags: { name: 'Concurrency_ToggleAdd' },
        });
    } else {
        res = http.del(url, null, {
            headers: getAuthHeaders(),
            tags: { name: 'Concurrency_ToggleDelete' },
        });
    }

    responseDuration.add(res.timings.duration);

    // 허용되는 응답: 200, 404, 409
    const validStatus = check(res, {
        'valid response': (r) => [200, 404, 409].includes(r.status),
    });

    if (validStatus) {
        successRate.add(1);
        if (res.status === 409) {
            conflictResponses.add(1);
        }
    } else if (res.status === 500) {
        raceConditionDetected.add(1);
        successRate.add(0);
    } else {
        unexpectedErrors.add(1);
        successRate.add(0);
    }

    return res.status;
}

// ===== 메인 시나리오 =====
export default function () {
    if (!CONFIG.ACCESS_TOKEN) {
        console.error('ACCESS_TOKEN is required. Use: k6 run -e ACCESS_TOKEN=your_token ...');
        return;
    }

    switch (TEST_TYPE) {
        case 'favorite':
            testFavoriteConcurrency();
            break;
        case 'review_like':
            testReviewLikeConcurrency();
            break;
        case 'toggle':
            testFavoriteToggleConcurrency();
            break;
        case 'all':
        default:
            // 랜덤하게 테스트 선택
            const tests = [
                testFavoriteConcurrency,
                testReviewLikeConcurrency,
                testFavoriteToggleConcurrency,
            ];
            const selectedTest = tests[Math.floor(Math.random() * tests.length)];
            selectedTest();
            break;
    }

    // 짧은 대기 (동시성 테스트이므로 최소화)
    sleep(0.1);
}

// ===== 테스트 요약 =====
export function handleSummary(data) {
    const m = data.metrics;
    const get = (metric, key) => metric?.values?.[key] ?? 0;

    const summary = `
================================================================================
                       동시성 충돌 테스트 결과
================================================================================

  테스트 설정
  -----------
  서버: ${CONFIG.BASE_URL}
  테스트 유형: ${TEST_TYPE}
  부하 패턴: ${__ENV.LOAD_TYPE || 'burst'}
  테스트 가게 ID: ${CONFIG.TEST_SHOP_ID}
  테스트 리뷰 ID: ${CONFIG.TEST_REVIEW_ID}

  동시성 결과
  -----------
  총 요청 수: ${get(m.http_reqs, 'count')}
  성공률: ${(get(m.success_rate, 'rate') * 100).toFixed(2)}%

  ⚠️  Race Condition 감지: ${get(m.race_condition, 'count')} 회
  ⚠️  중복 생성 감지: ${get(m.duplicate_created, 'count')} 회
  ✅ 정상 충돌 처리 (409): ${get(m.conflict_responses, 'count')} 회
  ❌ 예상치 못한 에러: ${get(m.unexpected_errors, 'count')} 회

  응답 시간
  -----------
  평균: ${get(m.response_duration, 'avg').toFixed(2)}ms
  p(95): ${get(m.response_duration, 'p(95)').toFixed(2)}ms
  최대: ${get(m.response_duration, 'max').toFixed(2)}ms

  분석
  -----------
  ${get(m.race_condition, 'count') > 0 ? '⚠️  Race Condition이 감지되었습니다. 동시성 처리 로직 검토가 필요합니다.' : '✅ Race Condition이 감지되지 않았습니다.'}
  ${get(m.duplicate_created, 'count') > 0 ? '❌ 중복 데이터가 생성되었습니다! DB 유니크 제약조건 또는 락 적용이 필요합니다.' : '✅ 중복 생성이 발생하지 않았습니다.'}
  ${get(m.conflict_responses, 'count') > 0 ? `ℹ️  ${get(m.conflict_responses, 'count')}회의 409 응답은 정상적인 동시성 충돌 처리입니다.` : ''}

================================================================================
`;

    console.log(summary);

    return {
        'k6/results/concurrency-test-summary.json': JSON.stringify(data, null, 2),
    };
}
