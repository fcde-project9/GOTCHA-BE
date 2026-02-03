/**
 * 전체 API 통합 부하 테스트
 *
 * 여러 API를 혼합하여 실제 트래픽 패턴을 시뮬레이션
 *
 * 실행: k6 run k6/scripts/all-apis.js
 * 실행 (인증 포함): k6 run -e ACCESS_TOKEN=your_token k6/scripts/all-apis.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { CONFIG, getHeaders, LOAD_STAGES, THRESHOLDS } from '../config.js';

// 커스텀 메트릭
const errorRate = new Rate('all_apis_errors');
const apiDuration = new Trend('all_apis_duration');

// API별 호출 비율 (실제 사용 패턴 기반)
const API_WEIGHTS = {
    shopMap: 40,      // 40% - 메인 화면
    shopDetail: 25,   // 25% - 상세 페이지
    shopSearch: 15,   // 15% - 검색
    reviewList: 10,   // 10% - 리뷰 조회
    userMe: 5,        // 5% - 내 정보
    favorite: 5,      // 5% - 찜
};

// 테스트 옵션
export const options = {
    stages: LOAD_STAGES[__ENV.LOAD_TYPE || 'light'],
    thresholds: {
        ...THRESHOLDS,
        all_apis_errors: ['rate<0.05'],
    },
};

// 가중치 기반 API 선택
function selectApi() {
    const total = Object.values(API_WEIGHTS).reduce((a, b) => a + b, 0);
    let random = Math.random() * total;

    for (const [api, weight] of Object.entries(API_WEIGHTS)) {
        random -= weight;
        if (random <= 0) {
            return api;
        }
    }
    return 'shopMap';
}

// 테스트 시나리오
export default function () {
    const selectedApi = selectApi();
    let response;
    let success = false;

    switch (selectedApi) {
        case 'shopMap':
            response = callShopMap();
            success = check(response, {
                '[지도] status is 200': (r) => r.status === 200,
            });
            break;

        case 'shopDetail':
            response = callShopDetail();
            success = check(response, {
                '[상세] status is 200': (r) => r.status === 200,
            });
            break;

        case 'shopSearch':
            response = callShopSearch();
            success = check(response, {
                '[검색] status is 200': (r) => r.status === 200,
            });
            break;

        case 'reviewList':
            response = callReviewList();
            success = check(response, {
                '[리뷰] status is 200': (r) => r.status === 200,
            });
            break;

        case 'userMe':
            if (CONFIG.ACCESS_TOKEN) {
                response = callUserMe();
                success = check(response, {
                    '[내정보] status is 200': (r) => r.status === 200,
                });
            } else {
                success = true; // 스킵
            }
            break;

        case 'favorite':
            if (CONFIG.ACCESS_TOKEN) {
                response = callFavorite();
                success = check(response, {
                    '[찜] status is 200 or 409': (r) => r.status === 200 || r.status === 409,
                });
            } else {
                success = true; // 스킵
            }
            break;
    }

    // 메트릭 기록
    errorRate.add(!success);
    if (response) {
        apiDuration.add(response.timings.duration);
    }

    sleep(Math.random() * 2 + 0.5); // 0.5~2.5초 랜덤 대기
}

// API 호출 함수들
function callShopMap() {
    const { swLat, swLng, neLat, neLng, latitude, longitude } = CONFIG.MAP_BOUNDS;
    return http.get(
        `${CONFIG.BASE_URL}/api/shops/map?swLat=${swLat}&swLng=${swLng}&neLat=${neLat}&neLng=${neLng}&latitude=${latitude}&longitude=${longitude}`,
        { headers: getHeaders(), tags: { name: 'AllApis_ShopMap' } }
    );
}

function callShopDetail() {
    return http.get(
        `${CONFIG.BASE_URL}/api/shops/${CONFIG.TEST_SHOP_ID}`,
        { headers: getHeaders(), tags: { name: 'AllApis_ShopDetail' } }
    );
}

function callShopSearch() {
    const keyword = CONFIG.SEARCH_KEYWORDS[Math.floor(Math.random() * CONFIG.SEARCH_KEYWORDS.length)];
    return http.get(
        `${CONFIG.BASE_URL}/api/shops/search?keyword=${encodeURIComponent(keyword)}`,
        { headers: getHeaders(), tags: { name: 'AllApis_ShopSearch' } }
    );
}

function callReviewList() {
    const sortTypes = ['LATEST', 'LIKE_COUNT'];
    const sortBy = sortTypes[Math.floor(Math.random() * sortTypes.length)];
    return http.get(
        `${CONFIG.BASE_URL}/api/shops/${CONFIG.TEST_SHOP_ID}/reviews?page=0&size=20&sortBy=${sortBy}`,
        { headers: getHeaders(), tags: { name: 'AllApis_ReviewList' } }
    );
}

function callUserMe() {
    return http.get(
        `${CONFIG.BASE_URL}/api/users/me`,
        { headers: getHeaders(true), tags: { name: 'AllApis_UserMe' } }
    );
}

function callFavorite() {
    return http.post(
        `${CONFIG.BASE_URL}/api/shops/${CONFIG.TEST_SHOP_ID}/favorite`,
        null,
        { headers: getHeaders(true), tags: { name: 'AllApis_Favorite' } }
    );
}

// 테스트 요약
export function handleSummary(data) {
    const summary = textSummary(data);
    console.log(summary);
    return {
        'k6/results/all-apis-summary.json': JSON.stringify(data),
    };
}

function textSummary(data) {
    const m = data.metrics;
    const get = (metric, key) => metric?.values?.[key] ?? 0;

    return `
========================================
  전체 API 통합 부하 테스트 결과
========================================
  API 비율: 지도(40%) | 상세(25%) | 검색(15%) | 리뷰(10%) | 내정보(5%) | 찜(5%)
========================================
  총 요청 수: ${get(m.http_reqs, 'count')}
  평균 응답 시간: ${get(m.http_req_duration, 'avg').toFixed(2)}ms
  95% 응답 시간: ${get(m.http_req_duration, 'p(95)').toFixed(2)}ms
  에러율: ${(get(m.http_req_failed, 'rate') * 100).toFixed(2)}%
========================================
`;
}
