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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "shops")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shop extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private String addressName;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private String mainImageUrl;

    private String locationHint;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String openTime;

    @Column(length = 50)
    private String region1DepthName;

    @Column(length = 50)
    private String region2DepthName;

    @Column(length = 100)
    private String region3DepthName;

    @Column(length = 10)
    private String mainAddressNo;

    @Column(length = 10)
    private String subAddressNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Builder
    public Shop(String name, String addressName, Double latitude, Double longitude,
                String mainImageUrl, String locationHint, String openTime,
                String region1DepthName, String region2DepthName, String region3DepthName,
                String mainAddressNo, String subAddressNo, User createdBy) {
        this.name = name;
        this.addressName = addressName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.mainImageUrl = mainImageUrl;
        this.locationHint = locationHint;
        this.openTime = openTime;
        this.region1DepthName = region1DepthName;
        this.region2DepthName = region2DepthName;
        this.region3DepthName = region3DepthName;
        this.mainAddressNo = mainAddressNo;
        this.subAddressNo = subAddressNo;
        this.createdBy = createdBy;
    }

    public void updateInfo(String name, String addressName, Double latitude, Double longitude,
                           String locationHint, String openTime,
                           String region1DepthName, String region2DepthName, String region3DepthName,
                           String mainAddressNo, String subAddressNo) {
        this.name = name;
        this.addressName = addressName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationHint = locationHint;
        this.openTime = openTime;
        this.region1DepthName = region1DepthName;
        this.region2DepthName = region2DepthName;
        this.region3DepthName = region3DepthName;
        this.mainAddressNo = mainAddressNo;
        this.subAddressNo = subAddressNo;
    }

    public void updateMainImage(String mainImageUrl) {
        this.mainImageUrl = mainImageUrl;
    }
}
