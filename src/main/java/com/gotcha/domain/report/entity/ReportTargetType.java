package com.gotcha.domain.report.entity;

public enum ReportTargetType {

    REVIEW("리뷰"),
    SHOP("가게"),
    USER("유저");

    private final String description;

    ReportTargetType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
