/**
 * GOTCHA 부하테스트 스크립트
 *
 * [사전 준비]
 *   - gotcha_dev DB에 더미 데이터 삽입 완료
 *   - 테스트 유저 ID 범위: 1032 ~ 2031 (1,000명)
 *   - dev 서버 실행 중 확인
 *
 * [실행 방법]
 *
 *   # 1단계 - 코드 레벨 점검 (20 VU, 10분) — N+1 / Slow Query 탐지
 *   k6 run --env BASE_URL=https://dev.gotcha.com --env TEST_TYPE=check k6/load-test.js
 *
 *   # 2단계 - 정상 부하 (60 VU, 10분) — SLA 기준 충족 확인
 *   k6 run --env BASE_URL=https://dev.gotcha.com --env TEST_TYPE=load k6/load-test.js
 *
 *   # 3단계 - 스트레스 (60→200 VU) — 한계 지점 탐색
 *   k6 run --env BASE_URL=https://dev.gotcha.com --env TEST_TYPE=stress k6/load-test.js
 *
 * [SLA 기준]
 *   정상 부하: P95 < 500ms, 에러율 < 1%
 *   스트레스:  P95 < 1s,    에러율 < 5%
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import exec from 'k6/execution';

// ================================================================
// 설정
// ================================================================
const BASE_URL       = __ENV.BASE_URL   || 'http://localhost:8080';
const TEST_TYPE      = __ENV.TEST_TYPE  || 'check'; // check | load | stress

const USER_ID_START  = 1032;
const USER_ID_END    = 2031;
const TOTAL_USERS    = USER_ID_END - USER_ID_START + 1; // 1,000명

// 서울 지도 뷰포트 — 지역별 소규모 영역 (실제 사용자처럼 특정 구역만 조회)
const SEOUL_AREAS = [
    { name: '강남/서초',   ne: { lat: 37.530, lng: 127.050 }, sw: { lat: 37.490, lng: 126.990 } },
    { name: '마포/홍대',   ne: { lat: 37.570, lng: 126.940 }, sw: { lat: 37.530, lng: 126.880 } },
    { name: '송파/잠실',   ne: { lat: 37.520, lng: 127.110 }, sw: { lat: 37.480, lng: 127.050 } },
    { name: '성동/성수',   ne: { lat: 37.560, lng: 127.070 }, sw: { lat: 37.520, lng: 127.010 } },
    { name: '강서/여의도', ne: { lat: 37.550, lng: 126.870 }, sw: { lat: 37.510, lng: 126.810 } },
    { name: '노원/도봉',   ne: { lat: 37.650, lng: 127.070 }, sw: { lat: 37.610, lng: 127.010 } },
    { name: '종로/용산',   ne: { lat: 37.580, lng: 127.000 }, sw: { lat: 37.540, lng: 126.940 } },
];

// ================================================================
// 커스텀 메트릭
// ================================================================
const shopMapDuration    = new Trend('shop_map_duration',    true);
const shopDetailDuration = new Trend('shop_detail_duration', true);
const reviewsDuration    = new Trend('reviews_duration',     true);
const apiErrors          = new Rate('api_errors');
const successCount       = new Counter('success_count');

// ================================================================
// 옵션 (TEST_TYPE별 분기)
// ================================================================
function buildOptions() {
    if (TEST_TYPE === 'check') {
        // 1단계: 코드 레벨 점검 — 낮은 VU로 Slow Query / N+1 탐지
        return {
            scenarios: {
                code_check: {
                    executor: 'constant-vus',
                    vus:      20,
                    duration: '10m',
                },
            },
            thresholds: {
                http_req_duration: ['p(95)<500'],
                http_req_failed:   ['rate<0.01'],
            },
        };
    }

    if (TEST_TYPE === 'load') {
        // 2단계: 정상 부하 — 시나리오별 가중치 (총 60 VU)
        //   시나리오 A - 지도 탐색:  65% → 39 VU
        //   시나리오 B - 마이페이지: 15% →  9 VU
        //   시나리오 C - 콘텐츠 소비: 12% →  7 VU
        //   시나리오 D - 쓰기:         8% →  5 VU
        return {
            scenarios: {
                map_browsing: {
                    executor: 'constant-vus', vus: 39, duration: '10m', exec: 'scenarioA',
                },
                my_page: {
                    executor: 'constant-vus', vus: 9,  duration: '10m', exec: 'scenarioB',
                },
                content: {
                    executor: 'constant-vus', vus: 7,  duration: '10m', exec: 'scenarioC',
                },
                write: {
                    executor: 'constant-vus', vus: 5,  duration: '10m', exec: 'scenarioD',
                },
            },
            thresholds: {
                'http_req_duration{scenario:map_browsing}': ['p(95)<500'],
                'http_req_duration{scenario:my_page}':      ['p(95)<500'],
                'http_req_duration{scenario:content}':      ['p(95)<500'],
                'http_req_duration{scenario:write}':        ['p(95)<1000'],
                http_req_failed: ['rate<0.01'],
            },
        };
    }

    if (TEST_TYPE === 'stress') {
        // 3단계: 스트레스 — VU 점진 증가로 한계 지점 탐색
        return {
            scenarios: {
                stress: {
                    executor: 'ramping-vus',
                    stages: [
                        { duration: '2m', target: 30  }, // 워밍업
                        { duration: '3m', target: 60  }, // 피크 부하 (목표)
                        { duration: '2m', target: 100 }, // 여유분 검증
                        { duration: '3m', target: 150 }, // 스트레스
                        { duration: '2m', target: 200 }, // 한계 탐색
                        { duration: '1m', target: 0   }, // 쿨다운
                    ],
                },
            },
            thresholds: {
                http_req_duration: ['p(95)<1000'],
                http_req_failed:   ['rate<0.05'],
            },
        };
    }

    throw new Error(`알 수 없는 TEST_TYPE: ${TEST_TYPE}. check | load | stress 중 하나를 사용하세요.`);
}

export const options = buildOptions();

// ================================================================
// Setup — 테스트 데이터 수집 (전체 1회 실행)
// ================================================================
export function setup() {
    console.log(`\n[Setup] TEST_TYPE=${TEST_TYPE} / BASE_URL=${BASE_URL}\n`);

    // 1. 지도 API로 shop ID 목록 수집 (인증 불필요 — 비회원도 조회 가능)
    const mapRes = http.get(
        `${BASE_URL}/api/shops/map` +
        `?northEastLat=37.65&northEastLng=127.20` +
        `&southWestLat=37.44&southWestLng=126.80`
    );

    let shopIds = [];
    if (mapRes.status === 200) {
        shopIds = (mapRes.json('data') || []).map(s => s.id);
        console.log(`[Setup] shop ${shopIds.length}개 수집 완료`);
    } else {
        console.warn(`[Setup] 가게 목록 조회 실패 (status=${mapRes.status}) — 더미 데이터 삽입 확인 필요`);
    }

    // 2. 첫 번째 가게의 리뷰 ID 수집 (좋아요 시나리오 사용)
    let reviewIds = [];
    if (shopIds.length > 0) {
        const reviewRes = http.get(
            `${BASE_URL}/api/shops/${shopIds[0]}/reviews?size=50`
        );
        if (reviewRes.status === 200) {
            reviewIds = (reviewRes.json('data.content') || []).map(r => r.id);
            console.log(`[Setup] review ${reviewIds.length}개 수집 완료`);
        }
    }

    if (shopIds.length === 0) {
        console.error('[Setup] shop ID 없음 — 더미 데이터가 없거나 서버 연결 실패');
    }

    return { shopIds, reviewIds };
}

// ================================================================
// VU별 토큰 관리 — 첫 요청 시 발급 후 캐시, 401 시 재발급
// ================================================================
let _accessToken = null;

function ensureToken() {
    if (_accessToken) return;

    const userId = USER_ID_START + ((exec.vu.idInTest - 1) % TOTAL_USERS);
    const res    = http.get(`${BASE_URL}/api/dev/token?userId=${userId}`);

    if (res.status === 200) {
        _accessToken = res.json('data.accessToken');
    } else {
        console.error(`[Token] 발급 실패 userId=${userId}, status=${res.status}`);
    }
}

function authHeaders() {
    ensureToken();
    return {
        headers: {
            'Authorization': `Bearer ${_accessToken}`,
            'Content-Type':  'application/json',
        },
    };
}

// 401 응답 시 토큰 무효화 → 다음 호출에서 재발급
function handleUnauthorized(res) {
    if (res.status === 401) {
        _accessToken = null;
    }
}

// ================================================================
// 유틸
// ================================================================
function randomItem(arr) {
    return arr[Math.floor(Math.random() * arr.length)];
}

function randomArea() {
    return SEOUL_AREAS[Math.floor(Math.random() * SEOUL_AREAS.length)];
}

// ================================================================
// 시나리오 A — 지도 탐색 (65%)
//   지도 이동 2~4회 반복 → 관심 가게 상세 조회
// ================================================================
export function scenarioA(data) {
    const headers = authHeaders();
    const area    = randomArea();
    const centerLat = (area.ne.lat + area.sw.lat) / 2;
    const centerLng = (area.ne.lng + area.sw.lng) / 2;

    group('A. 지도 탐색', () => {
        // 지도 이동 2~4회
        const moveCount = Math.floor(Math.random() * 3) + 2;
        for (let i = 0; i < moveCount; i++) {
            const dLat = (Math.random() - 0.5) * 0.01;
            const dLng = (Math.random() - 0.5) * 0.015;

            const res = http.get(
                `${BASE_URL}/api/shops/map` +
                `?northEastLat=${(area.ne.lat + dLat).toFixed(6)}` +
                `&northEastLng=${(area.ne.lng + dLng).toFixed(6)}` +
                `&southWestLat=${(area.sw.lat + dLat).toFixed(6)}` +
                `&southWestLng=${(area.sw.lng + dLng).toFixed(6)}` +
                `&latitude=${centerLat.toFixed(6)}` +
                `&longitude=${centerLng.toFixed(6)}`,
                headers
            );

            handleUnauthorized(res);
            shopMapDuration.add(res.timings.duration);

            const ok = check(res, {
                '[A] 지도 조회 200':    (r) => r.status === 200,
                '[A] 지도 500ms 이내': (r) => r.timings.duration < 500,
            });
            apiErrors.add(!ok);

            sleep(0.5 + Math.random() * 0.5); // 지도 이동 간 0.5~1초
        }

        // 관심 가게 상세 조회
        if (data.shopIds.length > 0) {
            const shopId = randomItem(data.shopIds);
            const res    = http.get(`${BASE_URL}/api/shops/${shopId}`, headers);

            handleUnauthorized(res);
            shopDetailDuration.add(res.timings.duration);

            const ok = check(res, {
                '[A] 가게 상세 200':    (r) => r.status === 200,
                '[A] 가게 상세 500ms': (r) => r.timings.duration < 500,
            });
            apiErrors.add(!ok);
            if (ok) successCount.add(1);

            sleep(1 + Math.random()); // 상세 페이지 체류 1~2초
        }
    });
}

// ================================================================
// 시나리오 B — 마이페이지 (15%)
//   내 정보 조회 → 찜 목록 조회
// ================================================================
export function scenarioB(data) {
    const headers = authHeaders();

    group('B. 마이페이지', () => {
        // 내 정보
        const meRes = http.get(`${BASE_URL}/api/users/me`, headers);
        handleUnauthorized(meRes);
        const meOk = check(meRes, {
            '[B] 내 정보 200':   (r) => r.status === 200,
            '[B] 내 정보 500ms': (r) => r.timings.duration < 500,
        });
        apiErrors.add(!meOk);

        sleep(0.5);

        // 찜 목록
        const favRes = http.get(`${BASE_URL}/api/users/me/favorites`, headers);
        handleUnauthorized(favRes);
        const favOk = check(favRes, {
            '[B] 찜 목록 200':   (r) => r.status === 200,
            '[B] 찜 목록 500ms': (r) => r.timings.duration < 500,
        });
        apiErrors.add(!favOk);
        if (meOk && favOk) successCount.add(1);

        sleep(1 + Math.random());
    });
}

// ================================================================
// 시나리오 C — 콘텐츠 소비 (12%)
//   리뷰 목록 조회 → 리뷰 좋아요 → 찜 추가
// ================================================================
export function scenarioC(data) {
    if (data.shopIds.length === 0) return;

    const headers = authHeaders();
    const shopId  = randomItem(data.shopIds);

    group('C. 콘텐츠 소비', () => {
        // 리뷰 목록 조회
        const sortBy    = Math.random() < 0.7 ? 'LATEST' : 'LIKE_COUNT';
        const reviewRes = http.get(
            `${BASE_URL}/api/shops/${shopId}/reviews?sortBy=${sortBy}&size=10`,
            headers
        );
        handleUnauthorized(reviewRes);
        reviewsDuration.add(reviewRes.timings.duration);
        const reviewOk = check(reviewRes, {
            '[C] 리뷰 목록 200':   (r) => r.status === 200,
            '[C] 리뷰 목록 500ms': (r) => r.timings.duration < 500,
        });
        apiErrors.add(!reviewOk);

        sleep(0.5);

        // 리뷰 좋아요 (중복 충돌 → 400 허용)
        if (data.reviewIds.length > 0) {
            const reviewId = randomItem(data.reviewIds);
            const likeRes  = http.post(
                `${BASE_URL}/api/shops/reviews/${reviewId}/like`,
                null,
                headers
            );
            handleUnauthorized(likeRes);
            check(likeRes, {
                '[C] 좋아요 201 또는 중복 400': (r) => r.status === 201 || r.status === 400,
            });
        }

        sleep(0.5);

        // 찜 추가 (중복 충돌 → 400 허용)
        const favRes = http.post(
            `${BASE_URL}/api/shops/${shopId}/favorite`,
            null,
            headers
        );
        handleUnauthorized(favRes);
        check(favRes, {
            '[C] 찜 201 또는 중복 400': (r) => r.status === 201 || r.status === 400,
        });
        if (reviewOk) successCount.add(1);

        sleep(1 + Math.random());
    });
}

// ================================================================
// 시나리오 D — 쓰기 (8%)
//   리뷰 작성 (content 10자 이상 필수)
// ================================================================
export function scenarioD(data) {
    if (data.shopIds.length === 0) return;

    const headers = authHeaders();
    const shopId  = randomItem(data.shopIds);

    group('D. 쓰기', () => {
        const body = JSON.stringify({
            content:   '부하테스트 자동 생성 리뷰입니다. 실제 방문 후 작성한 후기가 아닙니다.',
            imageUrls: [],
        });

        const res = http.post(
            `${BASE_URL}/api/shops/${shopId}/reviews`,
            body,
            headers
        );
        handleUnauthorized(res);

        const ok = check(res, {
            '[D] 리뷰 작성 201':  (r) => r.status === 201,
            '[D] 리뷰 작성 1s':   (r) => r.timings.duration < 1000,
        });
        apiErrors.add(!ok);
        if (ok) successCount.add(1);

        sleep(2 + Math.random() * 2); // 쓰기 후 2~4초 대기
    });
}

// ================================================================
// Default 함수 — check / stress 모드에서 가중치 기반 시나리오 분기
// ================================================================
export default function (data) {
    const rand = Math.random();

    if      (rand < 0.65) scenarioA(data); // 65% - 지도 탐색
    else if (rand < 0.80) scenarioB(data); // 15% - 마이페이지
    else if (rand < 0.92) scenarioC(data); // 12% - 콘텐츠 소비
    else                  scenarioD(data); //  8% - 쓰기
}

// ================================================================
// 결과 요약 출력
// ================================================================
export function handleSummary(data) {
    const m   = data.metrics;
    const get = (metric, key) => m[metric]?.values?.[key] ?? 0;

    const summary = `
================================================================================
                        GOTCHA 부하테스트 결과
================================================================================
  설정
  ─────────────────────────────────────────
  서버:          ${BASE_URL}
  테스트 유형:   ${TEST_TYPE}
  테스트 유저:   ${USER_ID_START} ~ ${USER_ID_END} (${TOTAL_USERS}명)

  전체 요청
  ─────────────────────────────────────────
  총 요청 수:    ${get('http_reqs', 'count')}
  초당 요청:     ${get('http_reqs', 'rate').toFixed(2)} req/s
  에러율:        ${(get('http_req_failed', 'rate') * 100).toFixed(2)}%
  성공 건수:     ${get('success_count', 'count')}

  응답 시간 (전체)
  ─────────────────────────────────────────
  평균:   ${get('http_req_duration', 'avg').toFixed(1)}ms
  P90:    ${get('http_req_duration', 'p(90)').toFixed(1)}ms
  P95:    ${get('http_req_duration', 'p(95)').toFixed(1)}ms
  P99:    ${get('http_req_duration', 'p(99)').toFixed(1)}ms
  최대:   ${get('http_req_duration', 'max').toFixed(1)}ms

  API별 응답 시간 P95
  ─────────────────────────────────────────
  GET /shops/map:      ${get('shop_map_duration', 'p(95)').toFixed(1)}ms
  GET /shops/{id}:     ${get('shop_detail_duration', 'p(95)').toFixed(1)}ms
  GET /reviews:        ${get('reviews_duration', 'p(95)').toFixed(1)}ms

  처리량
  ─────────────────────────────────────────
  수신 데이터:   ${(get('data_received', 'rate') / 1024).toFixed(1)} KB/s
  송신 데이터:   ${(get('data_sent', 'rate') / 1024).toFixed(1)} KB/s

================================================================================
`;

    console.log(summary);

    return {
        [`k6/results/${TEST_TYPE}-summary.json`]: JSON.stringify(data, null, 2),
    };
}
