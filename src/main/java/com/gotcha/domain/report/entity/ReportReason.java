package com.gotcha.domain.report.entity;

public enum ReportReason {

    // 리뷰 신고
    REVIEW_SPAM("도배/광고성 글이에요"),
    REVIEW_COPYRIGHT("저작권을 침해해요"),
    REVIEW_DEFAMATION("명예를 훼손하는 내용이에요"),
    REVIEW_ABUSE("욕설이나 비방이 심해요"),
    REVIEW_OBSCENE("외설적인 내용이 포함돼있어요"),
    REVIEW_PRIVACY("개인정보가 노출되어 있어요"),
    REVIEW_OTHER("기타"),

    // 가게 신고
    SHOP_WRONG_ADDRESS("잘못된 주소예요"),
    SHOP_CLOSED("영업 종료/폐업된 업체예요"),
    SHOP_INAPPROPRIATE("부적절한 업체(불법/유해 업소)예요"),
    SHOP_DUPLICATE("중복 제보된 업체예요"),
    SHOP_OTHER("기타"),

    // 사용자 신고
    USER_INAPPROPRIATE_NICKNAME("부적절한 닉네임이에요"),
    USER_INAPPROPRIATE_PROFILE("부적절한 프로필 사진이에요"),
    USER_PRIVACY("개인정보가 노출되어 있어요"),
    USER_OTHER("기타");

    private final String description;

    ReportReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public ReportTargetType getTargetType() {
        String prefix = this.name().split("_")[0];
        return ReportTargetType.valueOf(prefix);
    }
}
