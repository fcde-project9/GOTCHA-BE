package com.gotcha.domain.report.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha.domain.report.dto.AdminReportListResponse;
import com.gotcha.domain.report.dto.ReportDetailResponse;
import com.gotcha.domain.report.dto.UpdateReportStatusRequest;
import com.gotcha.domain.report.entity.ReportStatus;
import com.gotcha.domain.report.entity.ReportTargetType;
import com.gotcha.domain.report.service.AdminReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@Validated
public class AdminReportController implements AdminReportControllerApi {

    private final AdminReportService adminReportService;

    @Override
    @GetMapping
    public ApiResponse<AdminReportListResponse> getReports(
            @RequestParam(required = false) ReportTargetType targetType,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        AdminReportListResponse response = adminReportService.getReports(targetType, status, pageable);
        return ApiResponse.success(response);
    }

    @Override
    @GetMapping("/{reportId}")
    public ApiResponse<ReportDetailResponse> getReport(@PathVariable Long reportId) {
        ReportDetailResponse response = adminReportService.getReport(reportId);
        return ApiResponse.success(response);
    }

    @Override
    @PatchMapping("/{reportId}/status")
    public ApiResponse<ReportDetailResponse> updateReportStatus(
            @PathVariable Long reportId,
            @Valid @RequestBody UpdateReportStatusRequest request
    ) {
        ReportDetailResponse response = adminReportService.updateReportStatus(reportId, request);
        return ApiResponse.success(response);
    }
}
