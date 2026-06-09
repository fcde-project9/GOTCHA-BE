package com.gotcha.domain.push.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DevicePlatform {
    IOS,
    ANDROID;

    @JsonCreator
    public static DevicePlatform fromString(String value) {
        return DevicePlatform.valueOf(value.toUpperCase());
    }
}
