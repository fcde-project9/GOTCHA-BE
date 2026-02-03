// k6 공통 설정
export const CONFIG = {
    // 테스트 대상 서버 URL
    BASE_URL: __ENV.BASE_URL || 'http://localhost:8080',

    // 인증 토큰 (인증 필요한 API 테스트 시 사용)
    ACCESS_TOKEN: __ENV.ACCESS_TOKEN || '',

    // 테스트 데이터
    TEST_SHOP_ID: __ENV.TEST_SHOP_ID || 1,
    TEST_REVIEW_ID: __ENV.TEST_REVIEW_ID || 1,

    // 지도 테스트용 좌표 (서울 강남역 기준)
    MAP_BOUNDS: {
        swLat: 37.4900,
        swLng: 127.0200,
        neLat: 37.5100,
        neLng: 127.0400,
        latitude: 37.5000,
        longitude: 127.0300,
    },

    // 검색 키워드
    SEARCH_KEYWORDS: ['가챠', '뽑기', '캡슐', '피규어'],
};

// 공통 HTTP 헤더
export function getHeaders(withAuth = false) {
    const headers = {
        'Content-Type': 'application/json',
    };

    if (withAuth && CONFIG.ACCESS_TOKEN) {
        headers['Authorization'] = `Bearer ${CONFIG.ACCESS_TOKEN}`;
    }

    return headers;
}

// 부하 테스트 단계 설정
export const LOAD_STAGES = {
    // 가벼운 테스트 (개발 환경)
    light: [
        { duration: '10s', target: 5 },
        { duration: '30s', target: 10 },
        { duration: '10s', target: 0 },
    ],

    // 중간 테스트
    medium: [
        { duration: '30s', target: 20 },
        { duration: '1m', target: 50 },
        { duration: '30s', target: 0 },
    ],

    // 강한 테스트 (스트레스 테스트)
    heavy: [
        { duration: '1m', target: 50 },
        { duration: '2m', target: 100 },
        { duration: '1m', target: 200 },
        { duration: '1m', target: 0 },
    ],

    // 스파이크 테스트
    spike: [
        { duration: '10s', target: 10 },
        { duration: '10s', target: 200 },
        { duration: '30s', target: 200 },
        { duration: '10s', target: 10 },
        { duration: '10s', target: 0 },
    ],
};

// 성능 임계값 설정
export const THRESHOLDS = {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],  // 95%가 500ms 이하, 99%가 1초 이하
    http_req_failed: ['rate<0.01'],                   // 실패율 1% 미만
    http_reqs: ['rate>10'],                           // 초당 10개 이상 요청
};
