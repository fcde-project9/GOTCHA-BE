package com.gotcha._global.external.kakao.dto;

public record AddressInfo(
        String addressName,
        String region1DepthName,
        String region2DepthName,
        String region3DepthName,
        String mountainYn,
        String mainAddressNo,
        String subAddressNo
) {

    public static AddressInfo from(KakaoAddressResponse response) {
        KakaoAddressResponse.Document document = response.getFirstDocument();
        if (document == null || document.address() == null) {
            return null;
        }

        KakaoAddressResponse.Address address = document.address();
        return new AddressInfo(
                address.addressName(),
                address.region1depthName(),
                address.region2depthName(),
                address.region3depthName(),
                address.mountainYn(),
                address.mainAddressNo(),
                address.subAddressNo()
        );
    }
}
