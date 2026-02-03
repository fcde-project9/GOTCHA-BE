package com.gotcha.domain.user.entity;

import com.gotcha._global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"social_type", "social_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_type", length = 20)
    private SocialType socialType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserType userType = UserType.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "social_id")
    private String socialId;

    @Column(nullable = false, length = 50, unique = true)
    private String nickname;

    private String email;

    private String profileImageUrl;

    private LocalDateTime lastLoginAt;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean isDeleted = false;

    /**
     * 소셜 연동 해제용 토큰.
     * Google: OAuth access_token / Apple: refresh_token
     * 로그인 시 저장, 탈퇴 시 revoke API 호출에 사용
     */
    @Column(name = "social_revoke_token", columnDefinition = "TEXT")
    private String socialRevokeToken;

    @Builder
    public User(SocialType socialType, String socialId, String nickname,
                String email, String profileImageUrl, UserType userType) {
        this.socialType = socialType;
        this.socialId = socialId;
        this.nickname = nickname;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.userType = userType != null ? userType : UserType.NORMAL;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateProfileImage(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void updateEmail(String email) {
        this.email = email;
    }

    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void updateSocialRevokeToken(String socialRevokeToken) {
        this.socialRevokeToken = socialRevokeToken;
    }

    /**
     * 회원 탈퇴 처리
     * - 소셜 연동 정보 제거 (재가입 허용)
     * - 개인정보 마스킹 (닉네임, 이메일, 프로필 이미지)
     * - OAuth 토큰 제거
     * - 상태 변경 및 soft delete 플래그 설정
     *
     * 닉네임은 랜덤 UUID 8자리로 마스킹하여 내부 ID 노출 방지
     */
    public void delete() {
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
        this.socialType = null;
        this.socialId = null;
        this.nickname = "탈퇴한 사용자_" + randomSuffix;
        this.email = null;
        this.profileImageUrl = null;
        this.socialRevokeToken = null;
        this.status = UserStatus.DELETED;
        this.isDeleted = true;
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    public void ban() {
        this.status = UserStatus.BANNED;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public boolean isAdmin() {
        return this.userType == UserType.ADMIN;
    }
}
