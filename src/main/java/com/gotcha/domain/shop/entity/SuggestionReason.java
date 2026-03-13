package com.gotcha.domain.shop.entity;

public enum SuggestionReason {
    WRONG_ADDRESS("잘못된 주소예요"),
    WRONG_PHOTO("매장 사진이 달라요"),
    WRONG_LOCATION_HINT("매장 위치힌트가 달라요"),
    WRONG_BUSINESS_HOURS("영업시간 정보가 달라요"),
    WRONG_PAYMENT_INFO("카드 결제/ATM 등 결제 정보가 달라요"),
    OTHER("기타");

    private final String description;

    SuggestionReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
