package com.gotcha.domain.user.entity;

/**
 * 사용자 권한 타입
 * 앱에서 사용자에게 요청하는 디바이스 권한의 종류를 정의
 */
public enum PermissionType {
    /**
     * 위치 정보 접근 권한 - 현재 위치 기반 주변 가챠샵 검색에 사용
     */
    LOCATION,

    /**
     * 카메라 접근 권한 - 가챠샵 제보 시 실시간 사진 촬영에 사용
     */
    CAMERA,

    /**
     * 앨범(사진) 접근 권한 - 가챠샵 제보 시 기존 사진 선택에 사용
     */
    ALBUM,

    /**
     * 푸시 알림 권한 - 새 리뷰, 댓글 등 알림 수신에 사용
     */
    NOTIFICATION
}
