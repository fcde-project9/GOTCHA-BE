package com.gotcha.domain.report.entity;

/**
 * 신고 대상 유형
 * - 어떤 종류의 대상을 신고하는지 구분
 */
public enum ReportTargetType {

    /** 리뷰 신고 */
    REVIEW("리뷰"),

    /** 매장 문제 신고 */
    SHOP_REPORT("매장 문제 신고"),

    /** 매장 정보 수정 제안 */
    SHOP_SUGGESTION("매장 정보 수정 제안"),

    /** 사용자 신고 */
    USER("유저");

    private final String description;

    ReportTargetType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
