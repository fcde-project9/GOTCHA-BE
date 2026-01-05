package com.gotcha.domain.shop.entity;

import com.gotcha._global.entity.BaseTimeEntity;
import com.gotcha.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "shop_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShopReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Column(nullable = false, length = 50)
    private String reportTitle;

    @Column(columnDefinition = "TEXT")
    private String reportContent;

    @Column(nullable = false)
    private Boolean isAnonymous;

    @Builder
    public ShopReport(Shop shop, User reporter, String reportTitle,
                      String reportContent, Boolean isAnonymous) {
        this.shop = shop;
        this.reporter = reporter;
        this.reportTitle = reportTitle;
        this.reportContent = reportContent;
        this.isAnonymous = isAnonymous != null ? isAnonymous : false;
    }
}
