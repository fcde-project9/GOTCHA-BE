package com.gotcha._global.external.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record KakaoAddressResponse(
        List<Document> documents
) {

    public record Document(
            Address address,
            @JsonProperty("road_address") RoadAddress roadAddress
    ) {
    }

    public record Address(
            @JsonProperty("address_name") String addressName,
            @JsonProperty("region_1depth_name") String region1depthName,
            @JsonProperty("region_2depth_name") String region2depthName,
            @JsonProperty("region_3depth_name") String region3depthName,
            @JsonProperty("mountain_yn") String mountainYn,
            @JsonProperty("main_address_no") String mainAddressNo,
            @JsonProperty("sub_address_no") String subAddressNo
    ) {
    }

    public record RoadAddress(
            @JsonProperty("address_name") String addressName,
            @JsonProperty("region_1depth_name") String region1depthName,
            @JsonProperty("region_2depth_name") String region2depthName,
            @JsonProperty("region_3depth_name") String region3depthName,
            @JsonProperty("road_name") String roadName,
            @JsonProperty("building_name") String buildingName
    ) {
    }

    public boolean hasResult() {
        return documents != null && !documents.isEmpty();
    }

    public Document getFirstDocument() {
        if (!hasResult()) {
            return null;
        }
        return documents.get(0);
    }
}
