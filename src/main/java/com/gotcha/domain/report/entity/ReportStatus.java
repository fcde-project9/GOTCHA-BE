package com.gotcha.domain.report.entity;

public enum ReportStatus {

    PENDING("처리 대기"),
    ACCEPTED("승인"),
    REJECTED("반려"),
    CANCELLED("취소");

    private final String description;

    ReportStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
