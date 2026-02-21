package com.gotcha.domain.report.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha.domain.report.dto.CreateReportRequest;
import com.gotcha.domain.report.dto.ReportResponse;
import com.gotcha.domain.report.service.ReportService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class ReportController implements ReportControllerApi {

    private final ReportService reportService;

    @Override
    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReportResponse> createReport(@Valid @RequestBody CreateReportRequest request) {
        ReportResponse response = reportService.createReport(request);
        return ApiResponse.success(response);
    }

    @Override
    @GetMapping("/users/me/reports")
    public ApiResponse<List<ReportResponse>> getMyReports() {
        List<ReportResponse> reports = reportService.getMyReports();
        return ApiResponse.success(reports);
    }

    @Override
    @DeleteMapping("/reports/{reportId}")
    public ApiResponse<Void> cancelReport(@PathVariable Long reportId) {
        reportService.cancelReport(reportId);
        return ApiResponse.success(null);
    }
}
