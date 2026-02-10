package com.gotcha.domain.report.entity;

public enum ReportReason {

    ABUSE("욕설/비방"),
    OBSCENE("음란물"),
    SPAM("광고/스팸"),
    PRIVACY("개인정보 노출"),
    OTHER("기타");

    private final String description;

    ReportReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
