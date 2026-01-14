package com.gotcha.domain.file.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha.domain.file.dto.FileUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "File", description = "파일 업로드 API")
public interface FileUploadControllerApi {

    @Operation(
            summary = "이미지 파일 업로드",
            description = "이미지 파일을 클라우드 스토리지에 업로드 (dev: GCS, prod: AWS S3). 최대 20MB, jpg/jpeg/png/webp/heic/heif 형식 지원"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "업로드 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (파일 없음, 크기 초과, 지원하지 않는 형식)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "업로드 실패"
            )
    })
    ApiResponse<FileUploadResponse> uploadImage(
            @Parameter(description = "업로드할 이미지 파일", required = true)
            @RequestParam("file")
            @NotNull(message = "파일은 필수입니다")
            MultipartFile file,

            @Parameter(description = "저장할 폴더 (예: reviews, shops, profiles)", required = true, example = "reviews")
            @RequestParam("folder")
            @NotBlank(message = "폴더명은 필수입니다")
            String folder
    );
}
