package com.gotcha.domain.user.entity;

import com.gotcha._global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "withdrawal_surveys")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WithdrawalSurvey extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Convert(converter = WithdrawalReasonsConverter.class)
    @Column(name = "reasons", columnDefinition = "text", nullable = false)
    private List<WithdrawalReason> reasons = new ArrayList<>();

    @Column(length = 500)
    private String detail;

    @Builder
    public WithdrawalSurvey(User user, List<WithdrawalReason> reasons, String detail) {
        this.user = user;
        this.reasons = reasons != null ? new ArrayList<>(reasons) : new ArrayList<>();
        this.detail = detail;
    }
}
