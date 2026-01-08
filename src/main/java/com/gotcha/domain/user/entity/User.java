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

    @Column(name = "social_id")
    private String socialId;

    @Column(nullable = false, length = 50, unique = true)
    private String nickname;

    private String email;

    private String profileImageUrl;

    private LocalDateTime lastLoginAt;

    @Column(nullable = false)
    private Boolean isAnonymous;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean isDeleted = false;

    @Builder
    public User(SocialType socialType, String socialId, String nickname,
                String email, String profileImageUrl, Boolean isAnonymous) {
        this.socialType = socialType;
        this.socialId = socialId;
        this.nickname = nickname;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.isAnonymous = isAnonymous != null ? isAnonymous : false;
        this.isDeleted = false;
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

    /**
     * 회원 탈퇴 처리
     * - 개인정보 마스킹 (닉네임, 이메일, 프로필 이미지)
     * - soft delete 플래그 설정
     */
    public void delete() {
        this.nickname = "탈퇴한 사용자_" + this.id;
        this.email = null;
        this.profileImageUrl = null;
        this.isDeleted = true;
    }
}
