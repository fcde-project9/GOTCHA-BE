package com.gotcha.domain.report.entity;

public enum ReportReason {

    // 리뷰 신고
    REVIEW_SPAM(ReportTargetType.REVIEW, "도배/광고성 글이에요"),
    REVIEW_COPYRIGHT(ReportTargetType.REVIEW, "저작권을 침해해요"),
    REVIEW_DEFAMATION(ReportTargetType.REVIEW, "명예를 훼손하는 내용이에요"),
    REVIEW_ABUSE(ReportTargetType.REVIEW, "욕설이나 비방이 심해요"),
    REVIEW_OBSCENE(ReportTargetType.REVIEW, "외설적인 내용이 포함돼있어요"),
    REVIEW_PRIVACY(ReportTargetType.REVIEW, "개인정보가 노출되어 있어요"),
    REVIEW_OTHER(ReportTargetType.REVIEW, "기타"),

    // 가게 신고
    SHOP_WRONG_ADDRESS(ReportTargetType.SHOP, "잘못된 주소예요"),
    SHOP_CLOSED(ReportTargetType.SHOP, "영업 종료/폐업된 업체예요"),
    SHOP_INAPPROPRIATE(ReportTargetType.SHOP, "부적절한 업체(불법/유해 업소)예요"),
    SHOP_DUPLICATE(ReportTargetType.SHOP, "중복 제보된 업체예요"),
    SHOP_OTHER(ReportTargetType.SHOP, "기타"),

    // 사용자 신고
    USER_INAPPROPRIATE_NICKNAME(ReportTargetType.USER, "부적절한 닉네임이에요"),
    USER_INAPPROPRIATE_PROFILE(ReportTargetType.USER, "부적절한 프로필 사진이에요"),
    USER_PRIVACY(ReportTargetType.USER, "개인정보가 노출되어 있어요"),
    USER_OTHER(ReportTargetType.USER, "기타");

    private final ReportTargetType targetType;
    private final String description;

    ReportReason(ReportTargetType targetType, String description) {
        this.targetType = targetType;
        this.description = description;
    }

    public ReportTargetType getTargetType() {
        return targetType;
    }

    public String getDescription() {
        return description;
    }
}
