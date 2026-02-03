package com.gotcha.domain.user.entity;

public enum UserStatus {
    ACTIVE,      // 정상 활성
    SUSPENDED,   // 일시 정지 (관리자에 의한 기간 제재)
    BANNED,      // 영구 차단
    DELETED      // 탈퇴
}
