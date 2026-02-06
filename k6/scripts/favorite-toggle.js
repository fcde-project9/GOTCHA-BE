/**
 * 찜 추가/삭제 API 부하 테스트 (인증 필요)
 * POST /api/shops/{shopId}/favorite
 * DELETE /api/shops/{shopId}/favorite
 *
 * 실행: k6 run -e ACCESS_TOKEN=your_token k6/scripts/favorite-toggle.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { CONFIG, getHeaders, LOAD_STAGES, THRESHOLDS } from '../config.js';

// 커스텀 메트릭
const errorRate = new Rate('favorite_toggle_errors');
const favoriteToggleDuration = new Trend('favorite_toggle_duration');

// 테스트 옵션
export const options = {
    stages: LOAD_STAGES[__ENV.LOAD_TYPE || 'light'],
    thresholds: {
        ...THRESHOLDS,
        favorite_toggle_errors: ['rate<0.05'],
        favorite_toggle_duration: ['p(95)<500'],
    },
};

// 테스트 시나리오
export default function () {
    if (!CONFIG.ACCESS_TOKEN) {
        console.warn('ACCESS_TOKEN이 설정되지 않았습니다. -e ACCESS_TOKEN=your_token 옵션을 사용하세요.');
        return;
    }

    const shopId = CONFIG.TEST_SHOP_ID;
    const url = `${CONFIG.BASE_URL}/api/shops/${shopId}/favorite`;

    // 찜 추가
    const addResponse = http.post(url, null, {
        headers: getHeaders(true),
        tags: { name: 'FavoriteAdd' },
    });

    const addSuccess = check(addResponse, {
        'add status is 200 or 409': (r) => r.status === 200 || r.status === 409,
        'add response time < 500ms': (r) => r.timings.duration < 500,
    });

    sleep(0.5);

    // 찜 삭제
    const deleteResponse = http.del(url, null, {
        headers: getHeaders(true),
        tags: { name: 'FavoriteDelete' },
    });

    const deleteSuccess = check(deleteResponse, {
        'delete status is 200 or 404': (r) => r.status === 200 || r.status === 404,
        'delete response time < 500ms': (r) => r.timings.duration < 500,
    });

    // 메트릭 기록
    errorRate.add(!addSuccess || !deleteSuccess);
    favoriteToggleDuration.add((addResponse.timings.duration + deleteResponse.timings.duration) / 2);

    sleep(1);
}

// 테스트 요약
export function handleSummary(data) {
    const summary = textSummary(data);
    console.log(summary);
    return {
        'k6/results/favorite-toggle-summary.json': JSON.stringify(data),
    };
}

function textSummary(data) {
    const m = data.metrics;
    const get = (metric, key) => metric?.values?.[key] ?? 0;

    return `
========================================
  Favorite Toggle API 부하 테스트 결과
========================================
  총 요청 수: ${get(m.http_reqs, 'count')}
  평균 응답 시간: ${get(m.http_req_duration, 'avg').toFixed(2)}ms
  95% 응답 시간: ${get(m.http_req_duration, 'p(95)').toFixed(2)}ms
  에러율: ${(get(m.http_req_failed, 'rate') * 100).toFixed(2)}%
========================================
`;
}
