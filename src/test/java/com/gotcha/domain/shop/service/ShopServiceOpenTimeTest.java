package com.gotcha.domain.shop.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

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

    private ShopService createShopServiceForTest() {
        // ShopService 인스턴스 생성 (의존성은 null로 설정 - private 메서드 테스트용)
        return new ShopService(null, null, null, null, null, null, null);
    }
}
