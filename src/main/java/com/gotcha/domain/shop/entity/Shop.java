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
    private String address;

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
    private String region;

    @Column(length = 50)
    private String district;

    @Column(length = 50)
    private String neighborhood;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Builder
    public Shop(String name, String address, Double latitude, Double longitude,
                String mainImageUrl, String locationHint, String openTime,
                String region, String district, String neighborhood, User createdBy) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.mainImageUrl = mainImageUrl;
        this.locationHint = locationHint;
        this.openTime = openTime;
        this.region = region;
        this.district = district;
        this.neighborhood = neighborhood;
        this.createdBy = createdBy;
    }

    public void updateInfo(String name, String address, String locationHint, String openTime) {
        this.name = name;
        this.address = address;
        this.locationHint = locationHint;
        this.openTime = openTime;
    }

    public void updateMainImage(String mainImageUrl) {
        this.mainImageUrl = mainImageUrl;
    }
}
