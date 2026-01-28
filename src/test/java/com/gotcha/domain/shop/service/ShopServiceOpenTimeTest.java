package com.gotcha.domain.shop.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ShopServiceOpenTimeTest {

    @Test
    @DisplayName("splitTimeRange - 다양한 구분자 형식 테스트")
    void testSplitTimeRange() throws Exception {
        // given
        ShopService shopService = createShopServiceForTest();
        Method splitTimeRange = ShopService.class.getDeclaredMethod("splitTimeRange", String.class);
        splitTimeRange.setAccessible(true);

        // when & then
        System.out.println("=== splitTimeRange 테스트 ===");

        // 하이픈(-) 구분자
        String[] hyphenResult = (String[]) splitTimeRange.invoke(shopService, "09:00-21:30");
        System.out.println("09:00-21:30 → [" + hyphenResult[0] + ", " + hyphenResult[1] + "]");
        assertThat(hyphenResult).containsExactly("09:00", "21:30");

        // 틸드(~) 구분자
        String[] tildeResult = (String[]) splitTimeRange.invoke(shopService, "10:00~22:00");
        System.out.println("10:00~22:00 → [" + tildeResult[0] + ", " + tildeResult[1] + "]");
        assertThat(tildeResult).containsExactly("10:00", "22:00");

        // 공백 포함 하이픈( - ) 구분자
        String[] spacedHyphenResult = (String[]) splitTimeRange.invoke(shopService, "00:00 - 24:00");
        System.out.println("00:00 - 24:00 → [" + spacedHyphenResult[0] + ", " + spacedHyphenResult[1] + "]");
        assertThat(spacedHyphenResult).containsExactly("00:00", "24:00");

        // 공백 포함 틸드( ~ ) 구분자
        String[] spacedTildeResult = (String[]) splitTimeRange.invoke(shopService, "10:00 ~ 22:30");
        System.out.println("10:00 ~ 22:30 → [" + spacedTildeResult[0] + ", " + spacedTildeResult[1] + "]");
        assertThat(spacedTildeResult).containsExactly("10:00", "22:30");

        // null 입력
        String[] nullResult = (String[]) splitTimeRange.invoke(shopService, (String) null);
        System.out.println("null → " + nullResult);
        assertThat(nullResult).isNull();

        // 구분자 없는 경우
        String[] noSeparatorResult = (String[]) splitTimeRange.invoke(shopService, "10:00");
        System.out.println("10:00 (구분자 없음) → " + noSeparatorResult);
        assertThat(noSeparatorResult).isNull();
    }

    @Test
    @DisplayName("parseTimeString - 24:00 처리 테스트")
    void testParseTimeString() throws Exception {
        // given
        ShopService shopService = createShopServiceForTest();
        Method parseTimeString = ShopService.class.getDeclaredMethod("parseTimeString", String.class);
        parseTimeString.setAccessible(true);

        // when & then
        System.out.println("\n=== parseTimeString 테스트 ===");

        var time1 = parseTimeString.invoke(shopService, "10:00");
        System.out.println("10:00 → " + time1);
        assertThat(time1.toString()).isEqualTo("10:00");

        var time2 = parseTimeString.invoke(shopService, "24:00");
        System.out.println("24:00 → " + time2);
        assertThat(time2.toString()).isEqualTo("23:59:59");

        var time3 = parseTimeString.invoke(shopService, "00:00");
        System.out.println("00:00 → " + time3);
        assertThat(time3.toString()).isEqualTo("00:00");
    }

    @Test
    @DisplayName("통합 테스트 - 사용자 제공 데이터")
    void testUserProvidedData() throws Exception {
        System.out.println("\n=== 사용자 제공 데이터 테스트 ===");
        System.out.println("입력: {\"Fri\": \"09:00-21:30\", \"Mon\": \"10:00~22:00\", \"Sat\": \"10:00~22:30\", \"Sun\": \"10:00~22:30\", \"Thu\": \"10:00~22:00\", \"Tue\": null, \"Wed\": \"\"}");

        ShopService shopService = createShopServiceForTest();
        Method splitTimeRange = ShopService.class.getDeclaredMethod("splitTimeRange", String.class);
        splitTimeRange.setAccessible(true);

        // Fri: "09:00-21:30" - 하이픈
        String[] fri = (String[]) splitTimeRange.invoke(shopService, "09:00-21:30");
        System.out.println("\nFri: 09:00-21:30");
        System.out.println("  파싱 결과: [" + fri[0] + ", " + fri[1] + "]");
        System.out.println("  예상 상태: 영업 중 또는 영업 종료 (시간에 따라)");
        assertThat(fri).containsExactly("09:00", "21:30");

        // Mon: "10:00~22:00" - 틸드
        String[] mon = (String[]) splitTimeRange.invoke(shopService, "10:00~22:00");
        System.out.println("\nMon: 10:00~22:00");
        System.out.println("  파싱 결과: [" + mon[0] + ", " + mon[1] + "]");
        System.out.println("  예상 상태: 영업 중 또는 영업 종료 (시간에 따라)");
        assertThat(mon).containsExactly("10:00", "22:00");

        // Sat: "10:00~22:30" - 틸드
        String[] sat = (String[]) splitTimeRange.invoke(shopService, "10:00~22:30");
        System.out.println("\nSat: 10:00~22:30");
        System.out.println("  파싱 결과: [" + sat[0] + ", " + sat[1] + "]");
        assertThat(sat).containsExactly("10:00", "22:30");

        // Sun: "10:00~22:30" - 틸드
        String[] sun = (String[]) splitTimeRange.invoke(shopService, "10:00~22:30");
        System.out.println("\nSun: 10:00~22:30");
        System.out.println("  파싱 결과: [" + sun[0] + ", " + sun[1] + "]");
        assertThat(sun).containsExactly("10:00", "22:30");

        // Thu: "10:00~22:00" - 틸드
        String[] thu = (String[]) splitTimeRange.invoke(shopService, "10:00~22:00");
        System.out.println("\nThu: 10:00~22:00");
        System.out.println("  파싱 결과: [" + thu[0] + ", " + thu[1] + "]");
        assertThat(thu).containsExactly("10:00", "22:00");

        // Tue: null - 정보 없음
        System.out.println("\nTue: null");
        System.out.println("  예상 상태: \"\" (정보 없음)");

        // Wed: "" - 휴무
        System.out.println("\nWed: \"\" (빈 문자열)");
        System.out.println("  예상 상태: \"휴무\"");

        System.out.println("\n=== 모든 테스트 통과 ===");
    }

    @Test
    @DisplayName("24시간 영업 테스트 - 00:00 - 24:00")
    void test24HoursOpen() throws Exception {
        System.out.println("\n=== 24시간 영업 테스트 ===");

        ShopService shopService = createShopServiceForTest();
        Method splitTimeRange = ShopService.class.getDeclaredMethod("splitTimeRange", String.class);
        splitTimeRange.setAccessible(true);

        // 00:00 - 24:00 형식
        String[] result = (String[]) splitTimeRange.invoke(shopService, "00:00 - 24:00");
        System.out.println("00:00 - 24:00 → [" + result[0] + ", " + result[1] + "]");
        assertThat(result).containsExactly("00:00", "24:00");

        // 00:00-24:00 형식
        String[] result2 = (String[]) splitTimeRange.invoke(shopService, "00:00-24:00");
        System.out.println("00:00-24:00 → [" + result2[0] + ", " + result2[1] + "]");
        assertThat(result2).containsExactly("00:00", "24:00");

        // 00:00~24:00 형식
        String[] result3 = (String[]) splitTimeRange.invoke(shopService, "00:00~24:00");
        System.out.println("00:00~24:00 → [" + result3[0] + ", " + result3[1] + "]");
        assertThat(result3).containsExactly("00:00", "24:00");
    }

    @Test
    @DisplayName("getOpenStatus - 24시간 영업 케이스 (00:00 - 24:00)")
    void testGetOpenStatus_24Hours() throws Exception {
        System.out.println("\n=== getOpenStatus 24시간 영업 테스트 ===");

        ShopService shopService = createShopServiceForTest();
        Method getOpenStatus = ShopService.class.getDeclaredMethod("getOpenStatus", String.class);
        getOpenStatus.setAccessible(true);

        // 24시간 영업 JSON
        String json24Hours = """
            {
                "Mon": "00:00 - 24:00",
                "Tue": "00:00 - 24:00",
                "Wed": "00:00 - 24:00",
                "Thu": "00:00 - 24:00",
                "Fri": "00:00 - 24:00",
                "Sat": "00:00 - 24:00",
                "Sun": "00:00 - 24:00"
            }
            """;

        String status = (String) getOpenStatus.invoke(shopService, json24Hours);
        System.out.println("24시간 영업 상태: '" + status + "'");
        System.out.println("예상 상태: '영업 중' (24시간 영업이므로 항상 영업 중이어야 함)");

        // 24시간 영업이므로 항상 "영업 중"이어야 함
        assertThat(status).isEqualTo("영업 중");
    }

    @Test
    @DisplayName("getOpenStatus - 모든 요일 null 케이스")
    void testGetOpenStatus_AllNull() throws Exception {
        System.out.println("\n=== getOpenStatus 모든 요일 null 테스트 ===");

        ShopService shopService = createShopServiceForTest();
        Method getOpenStatus = ShopService.class.getDeclaredMethod("getOpenStatus", String.class);
        getOpenStatus.setAccessible(true);

        String jsonAllNull = """
            {
                "Mon": null,
                "Tue": null,
                "Wed": null,
                "Thu": null,
                "Fri": null,
                "Sat": null,
                "Sun": null
            }
            """;

        String status = (String) getOpenStatus.invoke(shopService, jsonAllNull);
        System.out.println("모든 요일 null 상태: '" + status + "'");
        System.out.println("예상 상태: '' (빈 문자열 - 정보 없음)");

        // null인 경우 빈 문자열 반환
        assertThat(status).isEqualTo("");
    }

    @Test
    @DisplayName("getOpenStatus - 일반 영업 시간 케이스")
    void testGetOpenStatus_NormalHours() throws Exception {
        System.out.println("\n=== getOpenStatus 일반 영업 시간 테스트 ===");

        ShopService shopService = createShopServiceForTest();
        Method getOpenStatus = ShopService.class.getDeclaredMethod("getOpenStatus", String.class);
        getOpenStatus.setAccessible(true);

        String jsonNormalHours = """
            {
                "Mon": "10:00 - 21:30",
                "Tue": "10:00 - 21:30",
                "Wed": "10:00 - 21:30",
                "Thu": "10:00 - 21:30",
                "Fri": "10:00 - 21:30",
                "Sat": "11:00 - 20:30",
                "Sun": "11:00 - 20:30"
            }
            """;

        String status = (String) getOpenStatus.invoke(shopService, jsonNormalHours);
        System.out.println("일반 영업 시간 상태: '" + status + "'");
        System.out.println("예상 상태: '영업 중' 또는 '영업 종료' (현재 시간에 따라 다름)");

        // 빈 문자열이 아니어야 함
        assertThat(status).isIn("영업 중", "영업 종료");
    }

    @Test
    @DisplayName("getOpenStatus - 휴무일 포함 케이스")
    void testGetOpenStatus_WithHoliday() throws Exception {
        System.out.println("\n=== getOpenStatus 휴무일 포함 테스트 ===");

        ShopService shopService = createShopServiceForTest();
        Method getOpenStatus = ShopService.class.getDeclaredMethod("getOpenStatus", String.class);
        getOpenStatus.setAccessible(true);

        String jsonWithHoliday = """
            {
                "Mon": "휴무",
                "Tue": "11:30 - 20:00",
                "Wed": "11:30 - 20:00",
                "Thu": "11:30 - 20:00",
                "Fri": "11:30 - 20:00",
                "Sat": "11:30 - 20:00",
                "Sun": "11:30 - 20:00"
            }
            """;

        String status = (String) getOpenStatus.invoke(shopService, jsonWithHoliday);
        System.out.println("휴무일 포함 상태: '" + status + "'");

        // 오늘이 월요일이면 "휴무", 그 외에는 "영업 중" 또는 "영업 종료"
        assertThat(status).isIn("휴무", "영업 중", "영업 종료");
    }

    @Test
    @DisplayName("isOpenNow - 24시간 영업 로직 상세 분석")
    void testIsOpenNow_24Hours_DetailAnalysis() throws Exception {
        System.out.println("\n=== isOpenNow 24시간 영업 로직 상세 분석 ===");

        ShopService shopService = createShopServiceForTest();
        Method parseTimeString = ShopService.class.getDeclaredMethod("parseTimeString", String.class);
        parseTimeString.setAccessible(true);

        java.time.LocalTime openTime = (java.time.LocalTime) parseTimeString.invoke(shopService, "00:00");
        java.time.LocalTime closeTime = (java.time.LocalTime) parseTimeString.invoke(shopService, "24:00");

        System.out.println("openTime (00:00): " + openTime);
        System.out.println("closeTime (24:00 → 변환): " + closeTime);
        System.out.println("closeTime.isBefore(openTime): " + closeTime.isBefore(openTime));

        // 다양한 시간에서 영업 중 여부 확인
        java.time.LocalTime[] testTimes = {
            java.time.LocalTime.of(0, 0),
            java.time.LocalTime.of(6, 0),
            java.time.LocalTime.of(12, 0),
            java.time.LocalTime.of(18, 0),
            java.time.LocalTime.of(23, 59, 59)
        };

        System.out.println("\n각 시간별 영업 중 여부:");
        for (java.time.LocalTime testTime : testTimes) {
            boolean isOpen;
            if (closeTime.isBefore(openTime)) {
                // overnight 케이스
                isOpen = !testTime.isBefore(openTime) || !testTime.isAfter(closeTime);
            } else {
                // 일반 케이스
                isOpen = !testTime.isBefore(openTime) && !testTime.isAfter(closeTime);
            }
            System.out.println("  " + testTime + " → " + (isOpen ? "영업 중" : "영업 종료"));
        }
    }

    private ShopService createShopServiceForTest() {
        // ShopService 인스턴스 생성 (ObjectMapper는 3번째 파라미터, 나머지 의존성은 null로 설정)
        // 순서: ShopRepository, KakaoMapClient, ObjectMapper, FavoriteRepository, ReviewRepository, ReviewImageRepository, ReviewLikeRepository
        return new ShopService(null, null, new com.fasterxml.jackson.databind.ObjectMapper(), null, null, null, null);
    }

    @Test
    @DisplayName("openTime Map → JSON 변환 테스트 (저장 시)")
    void testOpenTimeMapToJsonConversion() throws Exception {
        System.out.println("\n=== openTime Map → JSON 변환 테스트 ===");

        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

        // 24시간 영업 Map
        Map<String, String> openTime24Hours = new java.util.LinkedHashMap<>();
        openTime24Hours.put("Mon", "00:00 - 24:00");
        openTime24Hours.put("Tue", "00:00 - 24:00");
        openTime24Hours.put("Wed", "00:00 - 24:00");
        openTime24Hours.put("Thu", "00:00 - 24:00");
        openTime24Hours.put("Fri", "00:00 - 24:00");
        openTime24Hours.put("Sat", "00:00 - 24:00");
        openTime24Hours.put("Sun", "00:00 - 24:00");

        // Map → JSON 문자열 변환 (저장 시)
        String jsonString = objectMapper.writeValueAsString(openTime24Hours);
        System.out.println("Map → JSON 변환 결과:");
        System.out.println("  " + jsonString);

        // JSON 문자열 → Map 역변환 (조회 시)
        Map<String, String> parsedMap = objectMapper.readValue(jsonString,
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
        System.out.println("\nJSON → Map 역변환 결과:");
        parsedMap.forEach((key, value) -> System.out.println("  " + key + ": '" + value + "'"));

        // 검증
        assertThat(parsedMap.get("Mon")).isEqualTo("00:00 - 24:00");
        assertThat(parsedMap.get("Tue")).isEqualTo("00:00 - 24:00");

        // 이 Map으로 getOpenStatus 호출
        ShopService shopService = createShopServiceForTest();
        Method getOpenStatus = ShopService.class.getDeclaredMethod("getOpenStatus", String.class);
        getOpenStatus.setAccessible(true);

        String status = (String) getOpenStatus.invoke(shopService, jsonString);
        System.out.println("\ngetOpenStatus 결과: '" + status + "'");
        assertThat(status).isEqualTo("영업 중");
    }

    @Test
    @DisplayName("convertOpenTimeMapToString 동작 확인")
    void testConvertOpenTimeMapToString() throws Exception {
        System.out.println("\n=== convertOpenTimeMapToString 동작 확인 ===");

        ShopService shopService = createShopServiceForTest();
        Method convertMethod = ShopService.class.getDeclaredMethod("convertOpenTimeMapToString", Map.class);
        convertMethod.setAccessible(true);

        // 케이스 1: 모든 값이 null인 Map
        Map<String, String> allNullMap = new java.util.LinkedHashMap<>();
        allNullMap.put("Mon", null);
        allNullMap.put("Tue", null);
        allNullMap.put("Wed", null);
        allNullMap.put("Thu", null);
        allNullMap.put("Fri", null);
        allNullMap.put("Sat", null);
        allNullMap.put("Sun", null);
        String result1 = (String) convertMethod.invoke(shopService, allNullMap);
        System.out.println("케이스 1 (모든 값 null Map):");
        System.out.println("  입력 Map: " + allNullMap);
        System.out.println("  변환 결과: '" + result1 + "'");
        System.out.println("  결과 길이: " + (result1 != null ? result1.length() : "null"));

        // 케이스 2: null Map
        String result2 = (String) convertMethod.invoke(shopService, (Map<String, String>) null);
        System.out.println("\n케이스 2 (null Map):");
        System.out.println("  변환 결과: '" + result2 + "'");

        // 케이스 3: 빈 Map
        Map<String, String> emptyMap = new java.util.LinkedHashMap<>();
        String result3 = (String) convertMethod.invoke(shopService, emptyMap);
        System.out.println("\n케이스 3 (빈 Map):");
        System.out.println("  입력 Map: " + emptyMap);
        System.out.println("  변환 결과: '" + result3 + "'");
        System.out.println("  결과 길이: " + (result3 != null ? result3.length() : "null"));

        // 케이스 4: 24시간 영업 Map
        Map<String, String> fullMap = new java.util.LinkedHashMap<>();
        fullMap.put("Mon", "00:00 - 24:00");
        fullMap.put("Tue", "00:00 - 24:00");
        fullMap.put("Wed", "00:00 - 24:00");
        fullMap.put("Thu", "00:00 - 24:00");
        fullMap.put("Fri", "00:00 - 24:00");
        fullMap.put("Sat", "00:00 - 24:00");
        fullMap.put("Sun", "00:00 - 24:00");
        String result4 = (String) convertMethod.invoke(shopService, fullMap);
        System.out.println("\n케이스 4 (24시간 영업 Map):");
        System.out.println("  변환 결과: '" + result4 + "'");
    }

    @Test
    @DisplayName("다양한 openTime 저장 케이스 테스트")
    void testVariousOpenTimeCases() throws Exception {
        System.out.println("\n=== 다양한 openTime 저장 케이스 테스트 ===");

        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        ShopService shopService = createShopServiceForTest();
        Method getOpenStatus = ShopService.class.getDeclaredMethod("getOpenStatus", String.class);
        getOpenStatus.setAccessible(true);

        // 케이스 1: 모든 요일 null
        Map<String, String> case1 = new java.util.LinkedHashMap<>();
        case1.put("Mon", null);
        case1.put("Tue", null);
        case1.put("Wed", null);
        case1.put("Thu", null);
        case1.put("Fri", null);
        case1.put("Sat", null);
        case1.put("Sun", null);
        String json1 = objectMapper.writeValueAsString(case1);
        System.out.println("케이스 1 (모든 요일 null):");
        System.out.println("  저장되는 JSON: " + json1);
        String status1 = (String) getOpenStatus.invoke(shopService, json1);
        System.out.println("  getOpenStatus 결과: '" + status1 + "'");

        // 케이스 2: 24시간 영업
        Map<String, String> case2 = new java.util.LinkedHashMap<>();
        case2.put("Mon", "00:00 - 24:00");
        case2.put("Tue", "00:00 - 24:00");
        case2.put("Wed", "00:00 - 24:00");
        case2.put("Thu", "00:00 - 24:00");
        case2.put("Fri", "00:00 - 24:00");
        case2.put("Sat", "00:00 - 24:00");
        case2.put("Sun", "00:00 - 24:00");
        String json2 = objectMapper.writeValueAsString(case2);
        System.out.println("\n케이스 2 (24시간 영업):");
        System.out.println("  저장되는 JSON: " + json2);
        String status2 = (String) getOpenStatus.invoke(shopService, json2);
        System.out.println("  getOpenStatus 결과: '" + status2 + "'");

        // 케이스 3: 일반 영업 시간
        Map<String, String> case3 = new java.util.LinkedHashMap<>();
        case3.put("Mon", "10:00 - 21:30");
        case3.put("Tue", "10:00 - 21:30");
        case3.put("Wed", "10:00 - 21:30");
        case3.put("Thu", "10:00 - 21:30");
        case3.put("Fri", "10:00 - 21:30");
        case3.put("Sat", "11:00 - 20:30");
        case3.put("Sun", "11:00 - 20:30");
        String json3 = objectMapper.writeValueAsString(case3);
        System.out.println("\n케이스 3 (일반 영업 시간):");
        System.out.println("  저장되는 JSON: " + json3);
        String status3 = (String) getOpenStatus.invoke(shopService, json3);
        System.out.println("  getOpenStatus 결과: '" + status3 + "'");

        // 케이스 4: 월요일 휴무
        Map<String, String> case4 = new java.util.LinkedHashMap<>();
        case4.put("Mon", "");
        case4.put("Tue", "11:30 - 20:00");
        case4.put("Wed", "11:30 - 20:00");
        case4.put("Thu", "11:30 - 20:00");
        case4.put("Fri", "11:30 - 20:00");
        case4.put("Sat", "11:30 - 20:00");
        case4.put("Sun", "11:30 - 20:00");
        String json4 = objectMapper.writeValueAsString(case4);
        System.out.println("\n케이스 4 (월요일 휴무):");
        System.out.println("  저장되는 JSON: " + json4);
        String status4 = (String) getOpenStatus.invoke(shopService, json4);
        System.out.println("  getOpenStatus 결과: '" + status4 + "'");
    }
}
