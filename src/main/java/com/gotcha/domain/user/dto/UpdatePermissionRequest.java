package com.gotcha.domain.user.dto;

import com.gotcha.domain.user.entity.PermissionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "권한 동의 상태 업데이트 요청")
public record UpdatePermissionRequest(

        @Schema(description = "권한 타입", example = "LOCATION", allowableValues = {"LOCATION", "CAMERA", "ALBUM"})
        @NotNull(message = "권한 타입은 필수입니다")
        PermissionType permissionType,

        @Schema(description = "동의 여부", example = "true")
        @NotNull(message = "동의 여부는 필수입니다")
        Boolean isAgreed
) {
}
