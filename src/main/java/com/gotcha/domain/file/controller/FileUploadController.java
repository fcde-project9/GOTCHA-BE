package com.gotcha.domain.file.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha.domain.file.dto.FileUploadResponse;
import com.gotcha.domain.file.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "File", description = "파일 업로드 API")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Validated
public class FileUploadController {

    private final FileUploadService fileUploadService;

    /**
     *   folder example
     *   - 리뷰 작성 페이지 → folder: "reviews"
     *   - 가게 등록 페이지 → folder: "shops"
     *   - 프로필 수정 페이지 → folder: "profiles"
     */
    @Operation(
            summary = "이미지 파일 업로드",
            description = "이미지 파일을 Google Cloud Storage에 업로드. 최대 20MB, jpg/jpeg/png/webp/heic/heif 형식 지원"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "업로드 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (파일 없음, 크기 초과, 지원하지 않는 형식)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "업로드 실패")
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FileUploadResponse> uploadImage(
            @Parameter(description = "업로드할 이미지 파일", required = true)
            @RequestParam("file")
            @NotNull(message = "파일은 필수입니다")
            MultipartFile file,

            @Parameter(description = "저장할 폴더 (예: reviews, shops, profiles)", required = true, example = "reviews")
            @RequestParam("folder")
            @NotBlank(message = "폴더명은 필수입니다")
            String folder
    ) {
        FileUploadResponse response = fileUploadService.uploadImage(file, folder);
        return ApiResponse.success(response);
    }
}
