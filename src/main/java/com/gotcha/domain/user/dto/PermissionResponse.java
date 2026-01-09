package com.gotcha.domain.user.dto;

import com.gotcha.domain.user.entity.PermissionType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "권한 동의 상태 응답")
public record PermissionResponse(

        @Schema(description = "권한 타입", example = "LOCATION")
        PermissionType permissionType,

        @Schema(description = "동의 여부", example = "true")
        Boolean isAgreed
) {
    public static PermissionResponse of(PermissionType permissionType, Boolean isAgreed) {
        return new PermissionResponse(permissionType, isAgreed);
    }
}
