package com.gotcha.domain.shop.entity;

import com.gotcha._global.entity.BaseTimeEntity;
import com.gotcha.domain.user.entity.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "shop_suggestions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShopSuggestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suggester_id", nullable = false)
    private User suggester;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "shop_suggestion_reasons", joinColumns = @JoinColumn(name = "suggestion_id"))
    @Column(name = "reason", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private List<SuggestionReason> reasons = new ArrayList<>();

    @Builder
    public ShopSuggestion(Shop shop, User suggester, List<SuggestionReason> reasons) {
        this.shop = shop;
        this.suggester = suggester;
        this.reasons = reasons != null ? reasons : new ArrayList<>();
    }
}
