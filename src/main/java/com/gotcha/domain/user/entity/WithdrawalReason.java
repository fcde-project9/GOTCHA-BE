package com.gotcha.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WithdrawalReason {
    LOW_USAGE("사용을 잘 안하게 돼요"),
    INSUFFICIENT_INFO("가챠샵 정보가 부족해요"),
    INACCURATE_INFO("가챠샵 정보가 기재된 내용과 달라요"),
    PRIVACY_CONCERN("개인정보 보호를 위해 삭제할래요"),
    HAS_OTHER_ACCOUNT("다른 계정이 있어요"),
    OTHER("기타");

    private final String description;
}
