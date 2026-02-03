/**
 * 내 정보 조회 API 부하 테스트 (인증 필요)
 * GET /api/users/me
 *
 * 실행: k6 run -e ACCESS_TOKEN=your_token k6/scripts/user-me.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { CONFIG, getHeaders, LOAD_STAGES, THRESHOLDS } from '../config.js';

// 커스텀 메트릭
const errorRate = new Rate('user_me_errors');
const userMeDuration = new Trend('user_me_duration');

// 테스트 옵션
export const options = {
    stages: LOAD_STAGES[__ENV.LOAD_TYPE || 'light'],
    thresholds: {
        ...THRESHOLDS,
        user_me_errors: ['rate<0.01'],
        user_me_duration: ['p(95)<300'],
    },
};

// 테스트 시나리오
export default function () {
    if (!CONFIG.ACCESS_TOKEN) {
        console.warn('ACCESS_TOKEN이 설정되지 않았습니다. -e ACCESS_TOKEN=your_token 옵션을 사용하세요.');
        return;
    }

    const url = `${CONFIG.BASE_URL}/api/users/me`;

    const response = http.get(url, {
        headers: getHeaders(true),
        tags: { name: 'UserMe' },
    });

    // 응답 검증
    const success = check(response, {
        'status is 200': (r) => r.status === 200,
        'response has user data': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.success === true && body.data && body.data.nickname !== undefined;
            } catch {
                return false;
            }
        },
        'response time < 300ms': (r) => r.timings.duration < 300,
    });

    // 메트릭 기록
    errorRate.add(!success);
    userMeDuration.add(response.timings.duration);

    sleep(1);
}

// 테스트 요약
export function handleSummary(data) {
    const summary = textSummary(data);
    console.log(summary);
    return {
        'k6/results/user-me-summary.json': JSON.stringify(data),
    };
}

function textSummary(data) {
    const m = data.metrics;
    const get = (metric, key) => metric?.values?.[key] ?? 0;

    return `
========================================
  User Me API 부하 테스트 결과
========================================
  총 요청 수: ${get(m.http_reqs, 'count')}
  평균 응답 시간: ${get(m.http_req_duration, 'avg').toFixed(2)}ms
  95% 응답 시간: ${get(m.http_req_duration, 'p(95)').toFixed(2)}ms
  에러율: ${(get(m.http_req_failed, 'rate') * 100).toFixed(2)}%
========================================
`;
}
