/**
 * GOTCHA Backend 통합 부하 테스트
 *
 * 모든 API를 한 번에 테스트합니다.
 *
 * 실행:
 *   k6 run k6/load-test.js
 *   k6 run -e BASE_URL=http://13.209.53.103:8080 k6/load-test.js
 *   k6 run -e BASE_URL=http://13.209.53.103:8080 -e LOAD_TYPE=medium k6/load-test.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// ===== 설정 =====
const CONFIG = {
    BASE_URL: __ENV.BASE_URL || 'http://localhost:8080',
    TEST_SHOP_ID: __ENV.TEST_SHOP_ID || 1,
    ACCESS_TOKEN: __ENV.ACCESS_TOKEN || '',

    // 지도 테스트용 좌표 (서울 강남역 기준)
    MAP_BOUNDS: {
        southWestLat: 37.4900,
        southWestLng: 127.0200,
        northEastLat: 37.5100,
        northEastLng: 127.0400,
        latitude: 37.5000,
        longitude: 127.0300,
    },

    // 검색 키워드
    SEARCH_KEYWORDS: ['가챠', '뽑기', '캡슐', '피규어'],
};

// 부하 단계 설정
const LOAD_STAGES = {
    light: [
        { duration: '10s', target: 5 },
        { duration: '30s', target: 10 },
        { duration: '10s', target: 0 },
    ],
    medium: [
        { duration: '30s', target: 20 },
        { duration: '1m', target: 50 },
        { duration: '30s', target: 0 },
    ],
    heavy: [
        { duration: '1m', target: 50 },
        { duration: '2m', target: 100 },
        { duration: '1m', target: 200 },
        { duration: '1m', target: 0 },
    ],
};

// ===== 메트릭 =====
const apiErrors = new Rate('api_errors');
const apiDuration = new Trend('api_duration');
const successfulRequests = new Counter('successful_requests');

// API별 메트릭
const shopMapDuration = new Trend('shop_map_duration');
const shopDetailDuration = new Trend('shop_detail_duration');
const reviewListDuration = new Trend('review_list_duration');

// ===== 테스트 옵션 =====
export const options = {
    stages: LOAD_STAGES[__ENV.LOAD_TYPE || 'light'],
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1000'],
        http_req_failed: ['rate<0.1'],
        api_errors: ['rate<0.1'],
    },
};

// ===== 헬퍼 함수 =====
function getHeaders(withAuth = false) {
    const headers = { 'Content-Type': 'application/json' };
    if (withAuth && CONFIG.ACCESS_TOKEN) {
        headers['Authorization'] = `Bearer ${CONFIG.ACCESS_TOKEN}`;
    }
    return headers;
}

// ===== API 테스트 함수들 =====

function testShopMap() {
    const { southWestLat, southWestLng, northEastLat, northEastLng, latitude, longitude } = CONFIG.MAP_BOUNDS;
    const url = `${CONFIG.BASE_URL}/api/shops/map?southWestLat=${southWestLat}&southWestLng=${southWestLng}&northEastLat=${northEastLat}&northEastLng=${northEastLng}&latitude=${latitude}&longitude=${longitude}`;

    const res = http.get(url, { headers: getHeaders(), tags: { name: 'ShopMap' } });
    shopMapDuration.add(res.timings.duration);

    return check(res, {
        '[지도] status 200': (r) => r.status === 200,
        '[지도] has data': (r) => {
            try { return JSON.parse(r.body).success === true; }
            catch { return false; }
        },
    });
}

function testShopDetail() {
    const url = `${CONFIG.BASE_URL}/api/shops/${CONFIG.TEST_SHOP_ID}`;

    const res = http.get(url, { headers: getHeaders(), tags: { name: 'ShopDetail' } });
    shopDetailDuration.add(res.timings.duration);

    return check(res, {
        '[상세] status 200': (r) => r.status === 200,
        '[상세] has shop': (r) => {
            try { return JSON.parse(r.body).data?.id !== undefined; }
            catch { return false; }
        },
    });
}

function testShopSearch() {
    const keyword = CONFIG.SEARCH_KEYWORDS[Math.floor(Math.random() * CONFIG.SEARCH_KEYWORDS.length)];
    const url = `${CONFIG.BASE_URL}/api/shops/search?keyword=${encodeURIComponent(keyword)}`;

    const res = http.get(url, { headers: getHeaders(), tags: { name: 'ShopSearch' } });

    return check(res, {
        '[검색] status 200 or 400': (r) => r.status === 200 || r.status === 400,
    });
}

function testReviewList() {
    const sortTypes = ['LATEST', 'LIKE_COUNT'];
    const sortBy = sortTypes[Math.floor(Math.random() * sortTypes.length)];
    const url = `${CONFIG.BASE_URL}/api/shops/${CONFIG.TEST_SHOP_ID}/reviews?page=0&size=20&sortBy=${sortBy}`;

    const res = http.get(url, { headers: getHeaders(), tags: { name: 'ReviewList' } });
    reviewListDuration.add(res.timings.duration);

    return check(res, {
        '[리뷰] status 200': (r) => r.status === 200,
    });
}

function testUserMe() {
    if (!CONFIG.ACCESS_TOKEN) return true;

    const url = `${CONFIG.BASE_URL}/api/users/me`;
    const res = http.get(url, { headers: getHeaders(true), tags: { name: 'UserMe' } });

    return check(res, {
        '[내정보] status 200': (r) => r.status === 200,
    });
}

function testFavorite() {
    if (!CONFIG.ACCESS_TOKEN) return true;

    const url = `${CONFIG.BASE_URL}/api/shops/${CONFIG.TEST_SHOP_ID}/favorite`;

    // 찜 추가
    const addRes = http.post(url, null, { headers: getHeaders(true), tags: { name: 'FavoriteAdd' } });
    const addOk = check(addRes, {
        '[찜추가] status 200/409': (r) => r.status === 200 || r.status === 409,
    });

    sleep(0.3);

    // 찜 삭제
    const delRes = http.del(url, null, { headers: getHeaders(true), tags: { name: 'FavoriteDel' } });
    const delOk = check(delRes, {
        '[찜삭제] status 200/404': (r) => r.status === 200 || r.status === 404,
    });

    return addOk && delOk;
}

// ===== 메인 시나리오 =====
export default function () {
    let allSuccess = true;

    // 1. 지도 조회
    group('1. Shop Map', () => {
        allSuccess = testShopMap() && allSuccess;
    });
    sleep(0.3);

    // 2. 가게 상세 (25%)
    group('2. Shop Detail', () => {
        allSuccess = testShopDetail() && allSuccess;
    });
    sleep(0.3);

    // 3. 리뷰 목록 (15%)
    group('3. Review List', () => {
        allSuccess = testReviewList() && allSuccess;
    });
    sleep(0.3);

    // 4. 검색 (10%)
    if (Math.random() < 0.5) {
        group('4. Shop Search', () => {
            testShopSearch();
        });
        sleep(0.3);
    }

    // 5. 인증 API (ACCESS_TOKEN 있을 때만)
    if (CONFIG.ACCESS_TOKEN) {
        group('5. User Me', () => {
            allSuccess = testUserMe() && allSuccess;
        });
        sleep(0.3);

        if (Math.random() < 0.3) {
            group('6. Favorite Toggle', () => {
                allSuccess = testFavorite() && allSuccess;
            });
        }
    }

    // 메트릭 기록
    apiErrors.add(!allSuccess);
    if (allSuccess) successfulRequests.add(1);

    sleep(0.5);
}

// ===== 테스트 요약 =====
export function handleSummary(data) {
    const m = data.metrics;
    const get = (metric, key) => metric?.values?.[key] ?? 0;

    const summary = `
================================================================================
                      GOTCHA Backend 통합 부하 테스트 결과
================================================================================

  테스트 설정
  -----------
  서버: ${CONFIG.BASE_URL}
  부하 타입: ${__ENV.LOAD_TYPE || 'light'}
  테스트 가게 ID: ${CONFIG.TEST_SHOP_ID}
  인증 토큰: ${CONFIG.ACCESS_TOKEN ? '있음' : '없음'}

  전체 결과
  -----------
  총 요청 수: ${get(m.http_reqs, 'count')}
  성공한 요청: ${get(m.successful_requests, 'count')}
  에러율: ${(get(m.api_errors, 'rate') * 100).toFixed(2)}%

  응답 시간
  -----------
  평균: ${get(m.http_req_duration, 'avg').toFixed(2)}ms
  최소: ${get(m.http_req_duration, 'min').toFixed(2)}ms
  최대: ${get(m.http_req_duration, 'max').toFixed(2)}ms
  p(90): ${get(m.http_req_duration, 'p(90)').toFixed(2)}ms
  p(95): ${get(m.http_req_duration, 'p(95)').toFixed(2)}ms

  API별 응답 시간 (p95)
  -----------
  지도 조회: ${get(m.shop_map_duration, 'p(95)').toFixed(2)}ms
  가게 상세: ${get(m.shop_detail_duration, 'p(95)').toFixed(2)}ms
  리뷰 목록: ${get(m.review_list_duration, 'p(95)').toFixed(2)}ms

  처리량
  -----------
  초당 요청 수: ${get(m.http_reqs, 'rate').toFixed(2)} req/s
  초당 데이터 수신: ${(get(m.data_received, 'rate') / 1024).toFixed(2)} KB/s

================================================================================
`;

    console.log(summary);

    return {
        'k6/results/load-test-summary.json': JSON.stringify(data, null, 2),
    };
}
