/**
 * 가게 검색 API 부하 테스트
 * GET /api/shops/search
 *
 * 실행: k6 run k6/scripts/shop-search.js
 * 환경변수: k6 run -e BASE_URL=http://localhost:8080 k6/scripts/shop-search.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { CONFIG, getHeaders, LOAD_STAGES, THRESHOLDS } from '../config.js';

// 커스텀 메트릭
const errorRate = new Rate('shop_search_errors');
const shopSearchDuration = new Trend('shop_search_duration');

// 테스트 옵션
export const options = {
    stages: LOAD_STAGES[__ENV.LOAD_TYPE || 'light'],
    thresholds: {
        ...THRESHOLDS,
        shop_search_errors: ['rate<0.01'],
        shop_search_duration: ['p(95)<500'],
    },
};

// 테스트 시나리오
export default function () {
    // 랜덤 검색 키워드 선택
    const keyword = CONFIG.SEARCH_KEYWORDS[Math.floor(Math.random() * CONFIG.SEARCH_KEYWORDS.length)];
    const url = `${CONFIG.BASE_URL}/api/shops/search?keyword=${encodeURIComponent(keyword)}`;

    const response = http.get(url, {
        headers: getHeaders(),
        tags: { name: 'ShopSearch' },
    });

    // 응답 검증
    const success = check(response, {
        'status is 200': (r) => r.status === 200,
        'response has data': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.success === true;
            } catch {
                return false;
            }
        },
        'response time < 500ms': (r) => r.timings.duration < 500,
    });

    // 메트릭 기록
    errorRate.add(!success);
    shopSearchDuration.add(response.timings.duration);

    sleep(1);
}

// 테스트 요약
export function handleSummary(data) {
    const summary = textSummary(data);
    console.log(summary);
    return {
        'k6/results/shop-search-summary.json': JSON.stringify(data),
    };
}

function textSummary(data) {
    const m = data.metrics;
    const get = (metric, key) => metric?.values?.[key] ?? 0;

    return `
========================================
  Shop Search API 부하 테스트 결과
========================================
  총 요청 수: ${get(m.http_reqs, 'count')}
  평균 응답 시간: ${get(m.http_req_duration, 'avg').toFixed(2)}ms
  95% 응답 시간: ${get(m.http_req_duration, 'p(95)').toFixed(2)}ms
  에러율: ${(get(m.http_req_failed, 'rate') * 100).toFixed(2)}%
========================================
`;
}
