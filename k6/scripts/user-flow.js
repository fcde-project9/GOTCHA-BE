/**
 * 사용자 흐름 복합 시나리오 부하 테스트
 *
 * 시나리오: 지도 조회 → 가게 상세 → 리뷰 확인 → 찜 추가
 *
 * 실행 (비인증): k6 run k6/scripts/user-flow.js
 * 실행 (인증): k6 run -e ACCESS_TOKEN=your_token k6/scripts/user-flow.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { CONFIG, getHeaders, LOAD_STAGES, THRESHOLDS } from '../config.js';

// 커스텀 메트릭
const errorRate = new Rate('user_flow_errors');
const flowDuration = new Trend('user_flow_duration');
const flowComplete = Counter('user_flow_complete');

// 테스트 옵션
export const options = {
    stages: LOAD_STAGES[__ENV.LOAD_TYPE || 'light'],
    thresholds: {
        ...THRESHOLDS,
        user_flow_errors: ['rate<0.05'],
        user_flow_duration: ['p(95)<2000'],
    },
};

// 테스트 시나리오
export default function () {
    const flowStart = Date.now();
    let success = true;

    // Step 1: 지도에서 가게 목록 조회
    group('Step 1: 지도 조회', function () {
        const { southWestLat, southWestLng, northEastLat, northEastLng, latitude, longitude } = CONFIG.MAP_BOUNDS;
        const url = `${CONFIG.BASE_URL}/api/shops/map?southWestLat=${southWestLat}&southWestLng=${southWestLng}&northEastLat=${northEastLat}&northEastLng=${northEastLng}&latitude=${latitude}&longitude=${longitude}`;

        const response = http.get(url, {
            headers: getHeaders(),
            tags: { name: 'Flow_ShopMap' },
        });

        success = success && check(response, {
            '[지도] status is 200': (r) => r.status === 200,
            '[지도] has shop data': (r) => {
                try {
                    const body = JSON.parse(r.body);
                    return body.success === true;
                } catch {
                    return false;
                }
            },
        });
    });

    sleep(0.5); // 사용자가 지도를 보는 시간

    // Step 2: 가게 상세 조회
    group('Step 2: 가게 상세', function () {
        const shopId = CONFIG.TEST_SHOP_ID;
        const url = `${CONFIG.BASE_URL}/api/shops/${shopId}`;

        const response = http.get(url, {
            headers: getHeaders(),
            tags: { name: 'Flow_ShopDetail' },
        });

        success = success && check(response, {
            '[상세] status is 200': (r) => r.status === 200,
            '[상세] has shop detail': (r) => {
                try {
                    const body = JSON.parse(r.body);
                    return body.success === true && body.data && body.data.id !== undefined;
                } catch {
                    return false;
                }
            },
        });
    });

    sleep(1); // 사용자가 상세 정보를 보는 시간

    // Step 3: 리뷰 목록 조회
    group('Step 3: 리뷰 목록', function () {
        const shopId = CONFIG.TEST_SHOP_ID;
        const url = `${CONFIG.BASE_URL}/api/shops/${shopId}/reviews?page=0&size=20&sortBy=LATEST`;

        const response = http.get(url, {
            headers: getHeaders(),
            tags: { name: 'Flow_ReviewList' },
        });

        success = success && check(response, {
            '[리뷰] status is 200': (r) => r.status === 200,
            '[리뷰] has review data': (r) => {
                try {
                    const body = JSON.parse(r.body);
                    return body.success === true;
                } catch {
                    return false;
                }
            },
        });
    });

    sleep(0.5); // 사용자가 리뷰를 보는 시간

    // Step 4: 찜 추가 (인증된 경우에만)
    if (CONFIG.ACCESS_TOKEN) {
        group('Step 4: 찜 추가', function () {
            const shopId = CONFIG.TEST_SHOP_ID;
            const url = `${CONFIG.BASE_URL}/api/shops/${shopId}/favorite`;

            const response = http.post(url, null, {
                headers: getHeaders(true),
                tags: { name: 'Flow_FavoriteAdd' },
            });

            success = success && check(response, {
                '[찜] status is 200 or 409': (r) => r.status === 200 || r.status === 409,
            });
        });
    }

    // 메트릭 기록
    const flowEnd = Date.now();
    errorRate.add(!success);
    flowDuration.add(flowEnd - flowStart);

    if (success) {
        flowComplete.add(1);
    }

    sleep(1);
}

// 테스트 요약
export function handleSummary(data) {
    const summary = textSummary(data);
    console.log(summary);
    return {
        'k6/results/user-flow-summary.json': JSON.stringify(data),
    };
}

function textSummary(data) {
    const m = data.metrics;
    const get = (metric, key) => metric?.values?.[key] ?? 0;

    return `
========================================
  User Flow 복합 시나리오 테스트 결과
========================================
  시나리오: 지도 → 상세 → 리뷰 → 찜
========================================
  총 요청 수: ${get(m.http_reqs, 'count')}
  평균 응답 시간: ${get(m.http_req_duration, 'avg').toFixed(2)}ms
  95% 응답 시간: ${get(m.http_req_duration, 'p(95)').toFixed(2)}ms
  에러율: ${(get(m.http_req_failed, 'rate') * 100).toFixed(2)}%
  완료된 플로우: ${get(m.user_flow_complete, 'count')}
========================================
`;
}
