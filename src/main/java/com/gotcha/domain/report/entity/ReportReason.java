package com.gotcha.domain.report.entity;

/**
 * 신고 사유
 * - 각 사유는 특정 ReportTargetType에 매핑됨
 * - targetType: 이 사유가 적용되는 신고 대상 유형
 * - description: 사용자에게 보여지는 설명 문구
 */
public enum ReportReason {

    // ========== 리뷰 신고 (REVIEW) ==========
    /** 도배/광고성 글 */
    REVIEW_SPAM(ReportTargetType.REVIEW, "도배/광고성 글이에요"),
    /** 저작권 침해 */
    REVIEW_COPYRIGHT(ReportTargetType.REVIEW, "저작권을 침해해요"),
    /** 명예 훼손 */
    REVIEW_DEFAMATION(ReportTargetType.REVIEW, "명예를 훼손하는 내용이에요"),
    /** 욕설/비방 */
    REVIEW_ABUSE(ReportTargetType.REVIEW, "욕설이나 비방이 심해요"),
    /** 폭력/위협적 내용 */
    REVIEW_VIOLENCE(ReportTargetType.REVIEW, "폭력적이거나 위협적인 내용이에요"),
    /** 외설적 내용 */
    REVIEW_OBSCENE(ReportTargetType.REVIEW, "외설적인 내용이 포함돼있어요"),
    /** 개인정보 노출 */
    REVIEW_PRIVACY(ReportTargetType.REVIEW, "개인정보가 노출되어 있어요"),
    /** 혐오 표현 */
    REVIEW_HATE_SPEECH(ReportTargetType.REVIEW, "혐오 표현이 포함돼있어요"),
    /** 허위/거짓 정보 */
    REVIEW_FALSE_INFO(ReportTargetType.REVIEW, "허위/거짓 정보예요"),
    /** 기타 */
    REVIEW_OTHER(ReportTargetType.REVIEW, "기타"),

    // ========== 매장 문제 신고 (SHOP_REPORT) ==========
    /** 부적절한 업체 (불법/유해 업소) */
    SHOP_REPORT_INAPPROPRIATE(ReportTargetType.SHOP_REPORT, "부적절한 업체(불법/유해 업소)예요"),
    /** 부적절한 매장명/사진 */
    SHOP_REPORT_INAPPROPRIATE_CONTENT(ReportTargetType.SHOP_REPORT, "매장명/사진이 부적절해요"),
    /** 부적절한 위치 힌트 */
    SHOP_REPORT_INAPPROPRIATE_HINT(ReportTargetType.SHOP_REPORT, "위치 힌트에 부적절한 단어가 있어요"),
    /** 중복 등록된 매장 */
    SHOP_REPORT_DUPLICATE(ReportTargetType.SHOP_REPORT, "중복 등록된 매장이에요"),
    /** 기타 */
    SHOP_REPORT_OTHER(ReportTargetType.SHOP_REPORT, "기타"),

    // ========== 매장 정보 수정 제안 (SHOP_SUGGESTION) ==========
    /** 주소 정보 오류 */
    SHOP_SUGGESTION_WRONG_ADDRESS(ReportTargetType.SHOP_SUGGESTION, "주소 정보가 잘못됐어요"),
    /** 위치 힌트 오류 */
    SHOP_SUGGESTION_WRONG_LOCATION_HINT(ReportTargetType.SHOP_SUGGESTION, "매장 위치힌트가 달라요"),
    /** 영업 종료/폐업 */
    SHOP_SUGGESTION_CLOSED(ReportTargetType.SHOP_SUGGESTION, "영업 종료/폐업된 매장이에요"),
    /** 영업시간 정보 오류 */
    SHOP_SUGGESTION_WRONG_HOURS(ReportTargetType.SHOP_SUGGESTION, "영업시간 정보가 달라요"),
    /** 결제 정보 오류 */
    SHOP_SUGGESTION_WRONG_PAYMENT(ReportTargetType.SHOP_SUGGESTION, "카드 결제/ATM 등 결제 정보가 달라요"),
    /** 기타 */
    SHOP_SUGGESTION_OTHER(ReportTargetType.SHOP_SUGGESTION, "기타"),

    // ========== 사용자 신고 (USER) ==========
    /** 부적절한 닉네임 */
    USER_INAPPROPRIATE_NICKNAME(ReportTargetType.USER, "부적절한 닉네임이에요"),
    /** 부적절한 프로필 사진 */
    USER_INAPPROPRIATE_PROFILE(ReportTargetType.USER, "부적절한 프로필 사진이에요"),
    /** 개인정보 노출 */
    USER_PRIVACY(ReportTargetType.USER, "개인정보가 노출되어 있어요"),
    /** 사칭 */
    USER_IMPERSONATION(ReportTargetType.USER, "다른 사람을 사칭하고 있어요"),
    /** 혐오 표현 */
    USER_HATE_SPEECH(ReportTargetType.USER, "혐오 표현이 포함돼있어요"),
    /** 기타 */
    USER_OTHER(ReportTargetType.USER, "기타"),

    // ========== 게시글 신고 (POST) ==========
    /** 도배/광고성 글 */
    POST_SPAM(ReportTargetType.POST, "도배/광고성 글이에요"),
    /** 저작권 침해 */
    POST_COPYRIGHT(ReportTargetType.POST, "저작권을 침해해요"),
    /** 명예 훼손 */
    POST_DEFAMATION(ReportTargetType.POST, "명예를 훼손하는 내용이에요"),
    /** 욕설/비방 */
    POST_ABUSE(ReportTargetType.POST, "욕설이나 비방이 심해요"),
    /** 폭력/위협적 내용 */
    POST_VIOLENCE(ReportTargetType.POST, "폭력적이거나 위협적인 내용이에요"),
    /** 외설적 내용 */
    POST_OBSCENE(ReportTargetType.POST, "외설적인 내용이 포함돼있어요"),
    /** 개인정보 노출 */
    POST_PRIVACY(ReportTargetType.POST, "개인정보가 노출되어 있어요"),
    /** 혐오 표현 */
    POST_HATE_SPEECH(ReportTargetType.POST, "혐오 표현이 포함돼있어요"),
    /** 허위/거짓 정보 */
    POST_FALSE_INFO(ReportTargetType.POST, "허위/거짓 정보예요"),
    /** 기타 */
    POST_OTHER(ReportTargetType.POST, "기타");

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
