package com.gotcha.domain.report.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.gotcha.domain.report.dto.ReportReasonResponse;
import com.gotcha.domain.report.dto.ReportReasonResponse.ReasonItem;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReportControllerTest {

    @Test
    @DisplayName("ReportReasonResponse.getAllReasons() - 신고 사유 목록 조회")
    void getAllReasons_Success() {
        // when
        List<ReportReasonResponse> result = ReportReasonResponse.getAllReasons();

        // then
        assertThat(result).hasSize(4);

        // REVIEW
        ReportReasonResponse review = result.get(0);
        assertThat(review.targetType()).isEqualTo("REVIEW");
        assertThat(review.targetTypeDescription()).isEqualTo("리뷰");
        assertThat(review.reasons()).hasSize(10);

        // SHOP_REPORT
        ReportReasonResponse shopReport = result.get(1);
        assertThat(shopReport.targetType()).isEqualTo("SHOP_REPORT");
        assertThat(shopReport.targetTypeDescription()).isEqualTo("매장 문제 신고");
        assertThat(shopReport.reasons()).hasSize(5);

        // SHOP_SUGGESTION
        ReportReasonResponse shopSuggestion = result.get(2);
        assertThat(shopSuggestion.targetType()).isEqualTo("SHOP_SUGGESTION");
        assertThat(shopSuggestion.targetTypeDescription()).isEqualTo("매장 정보 수정 제안");
        assertThat(shopSuggestion.reasons()).hasSize(6);

        // USER
        ReportReasonResponse user = result.get(3);
        assertThat(user.targetType()).isEqualTo("USER");
        assertThat(user.targetTypeDescription()).isEqualTo("유저");
        assertThat(user.reasons()).hasSize(6);
    }

    @Test
    @DisplayName("SHOP_REPORT 사유 상세 확인")
    void shopReportReasons_Details() {
        // when
        List<ReportReasonResponse> result = ReportReasonResponse.getAllReasons();
        ReportReasonResponse shopReport = result.stream()
                .filter(r -> r.targetType().equals("SHOP_REPORT"))
                .findFirst()
                .orElseThrow();

        // then
        List<ReasonItem> reasons = shopReport.reasons();
        assertThat(reasons.get(0).code()).isEqualTo("SHOP_REPORT_INAPPROPRIATE");
        assertThat(reasons.get(0).description()).isEqualTo("부적절한 업체(불법/유해 업소)예요");

        assertThat(reasons.get(1).code()).isEqualTo("SHOP_REPORT_INAPPROPRIATE_CONTENT");
        assertThat(reasons.get(1).description()).isEqualTo("매장명/사진이 부적절해요");

        assertThat(reasons.get(2).code()).isEqualTo("SHOP_REPORT_INAPPROPRIATE_HINT");
        assertThat(reasons.get(2).description()).isEqualTo("위치 힌트에 부적절한 단어가 있어요");

        assertThat(reasons.get(3).code()).isEqualTo("SHOP_REPORT_DUPLICATE");
        assertThat(reasons.get(3).description()).isEqualTo("중복 등록된 매장이에요");

        assertThat(reasons.get(4).code()).isEqualTo("SHOP_REPORT_OTHER");
        assertThat(reasons.get(4).description()).isEqualTo("기타");
    }

    @Test
    @DisplayName("SHOP_SUGGESTION 사유 상세 확인")
    void shopSuggestionReasons_Details() {
        // when
        List<ReportReasonResponse> result = ReportReasonResponse.getAllReasons();
        ReportReasonResponse shopSuggestion = result.stream()
                .filter(r -> r.targetType().equals("SHOP_SUGGESTION"))
                .findFirst()
                .orElseThrow();

        // then
        List<ReasonItem> reasons = shopSuggestion.reasons();
        assertThat(reasons.get(0).code()).isEqualTo("SHOP_SUGGESTION_WRONG_ADDRESS");
        assertThat(reasons.get(0).description()).isEqualTo("주소 정보가 잘못됐어요");

        assertThat(reasons.get(1).code()).isEqualTo("SHOP_SUGGESTION_WRONG_LOCATION_HINT");
        assertThat(reasons.get(1).description()).isEqualTo("매장 위치힌트가 달라요");

        assertThat(reasons.get(2).code()).isEqualTo("SHOP_SUGGESTION_CLOSED");
        assertThat(reasons.get(2).description()).isEqualTo("영업 종료/폐업된 매장이에요");

        assertThat(reasons.get(3).code()).isEqualTo("SHOP_SUGGESTION_WRONG_HOURS");
        assertThat(reasons.get(3).description()).isEqualTo("영업시간 정보가 달라요");

        assertThat(reasons.get(4).code()).isEqualTo("SHOP_SUGGESTION_WRONG_PAYMENT");
        assertThat(reasons.get(4).description()).isEqualTo("카드 결제/ATM 등 결제 정보가 달라요");

        assertThat(reasons.get(5).code()).isEqualTo("SHOP_SUGGESTION_OTHER");
        assertThat(reasons.get(5).description()).isEqualTo("기타");
    }
}
