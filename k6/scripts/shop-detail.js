/**
 * 가게 상세 조회 API 부하 테스트
 * GET /api/shops/{shopId}
 *
 * 실행: k6 run k6/scripts/shop-detail.js
 * 환경변수: k6 run -e BASE_URL=http://localhost:8080 -e TEST_SHOP_ID=1 k6/scripts/shop-detail.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { CONFIG, getHeaders, LOAD_STAGES, THRESHOLDS } from '../config.js';

// 커스텀 메트릭
const errorRate = new Rate('shop_detail_errors');
const shopDetailDuration = new Trend('shop_detail_duration');

// 테스트 옵션
export const options = {
    stages: LOAD_STAGES[__ENV.LOAD_TYPE || 'light'],
    thresholds: {
        ...THRESHOLDS,
        shop_detail_errors: ['rate<0.01'],
        shop_detail_duration: ['p(95)<500'],
    },
};

// 테스트 시나리오
export default function () {
    const shopId = CONFIG.TEST_SHOP_ID;
    const url = `${CONFIG.BASE_URL}/api/shops/${shopId}`;

    const response = http.get(url, {
        headers: getHeaders(),
        tags: { name: 'ShopDetail' },
    });

    // 응답 검증
    const success = check(response, {
        'status is 200': (r) => r.status === 200,
        'response has shop data': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.success === true && body.data && body.data.id !== undefined;
            } catch {
                return false;
            }
        },
        'response time < 500ms': (r) => r.timings.duration < 500,
    });

    // 메트릭 기록
    errorRate.add(!success);
    shopDetailDuration.add(response.timings.duration);

    sleep(1);
}

// 테스트 요약
export function handleSummary(data) {
    const summary = textSummary(data);
    console.log(summary);
    return {
        'k6/results/shop-detail-summary.json': JSON.stringify(data),
    };
}

function textSummary(data) {
    const m = data.metrics;
    const get = (metric, key) => metric?.values?.[key] ?? 0;

    return `
========================================
  Shop Detail API 부하 테스트 결과
========================================
  총 요청 수: ${get(m.http_reqs, 'count')}
  평균 응답 시간: ${get(m.http_req_duration, 'avg').toFixed(2)}ms
  95% 응답 시간: ${get(m.http_req_duration, 'p(95)').toFixed(2)}ms
  에러율: ${(get(m.http_req_failed, 'rate') * 100).toFixed(2)}%
========================================
`;
}
