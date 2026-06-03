package com.gotcha.domain.admin.controller;

import com.gotcha.domain.report.dto.UpdateReportStatusRequest;
import com.gotcha.domain.report.entity.ReportStatus;
import com.gotcha.domain.report.entity.ReportTargetType;
import com.gotcha.domain.report.service.AdminReportService;
import com.gotcha.domain.user.entity.UserStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class AdminReportWebController {

    private static final List<Integer> SUSPENSION_HOURS = List.of(1, 12, 24, 72, 120, 168, 336, 720);

    private final AdminReportService adminReportService;

    @GetMapping
    public String listReports(
            @RequestParam(required = false) ReportTargetType targetType,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        model.addAttribute("result", adminReportService.getReports(targetType, status, pageable));
        model.addAttribute("targetTypes", ReportTargetType.values());
        model.addAttribute("statuses", ReportStatus.values());
        model.addAttribute("currentTargetType", targetType);
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentPage", safePage);
        model.addAttribute("currentMenu", "reports");
        model.addAttribute("pageTitle", "신고 관리");
        return "admin/reports/list";
    }

    @GetMapping("/{reportId}")
    public String reportDetail(@PathVariable Long reportId, Model model) {
        model.addAttribute("report", adminReportService.getReport(reportId));
        model.addAttribute("suspensionHours", SUSPENSION_HOURS);
        model.addAttribute("currentMenu", "reports");
        model.addAttribute("pageTitle", "신고 상세");
        return "admin/reports/detail";
    }

    @PostMapping("/{reportId}/status")
    public String updateStatus(
            @PathVariable Long reportId,
            @RequestParam ReportStatus status,
            @RequestParam(required = false) UserStatus userStatus,
            @RequestParam(required = false) Integer suspensionHours,
            RedirectAttributes redirectAttributes) {
        adminReportService.updateReportStatus(reportId,
                new UpdateReportStatusRequest(status, userStatus, suspensionHours));
        redirectAttributes.addFlashAttribute("message", "신고 상태가 변경되었습니다.");
        return "redirect:/admin/reports/" + reportId;
    }
}
