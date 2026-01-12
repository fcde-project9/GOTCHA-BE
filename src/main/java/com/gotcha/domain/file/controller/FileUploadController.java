package com.gotcha.domain.file.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha.domain.file.dto.FileUploadResponse;
import com.gotcha.domain.file.service.FileUploadService;
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

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Validated
public class FileUploadController implements FileUploadControllerApi {

    private final FileUploadService fileUploadService;

    @Override
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FileUploadResponse> uploadImage(
            @RequestParam("file")
            @NotNull(message = "파일은 필수입니다")
            MultipartFile file,

            @RequestParam("folder")
            @NotBlank(message = "폴더명은 필수입니다")
            String folder
    ) {
        FileUploadResponse response = fileUploadService.uploadImage(file, folder);
        return ApiResponse.success(response);
    }
}
