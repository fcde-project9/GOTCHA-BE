package com.gotcha.domain.admin.controller;

import com.gotcha.domain.user.dto.UpdateUserStatusRequest;
import com.gotcha.domain.user.entity.UserStatus;
import com.gotcha.domain.user.service.AdminUserService;
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
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserWebController {

    private static final List<Integer> SUSPENSION_HOURS = List.of(1, 12, 24, 72, 120, 168, 336, 720);

    private final AdminUserService adminUserService;

    @GetMapping
    public String listUsers(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        model.addAttribute("result", adminUserService.getUsers(status, pageable));
        model.addAttribute("statuses", UserStatus.values());
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentPage", safePage);
        model.addAttribute("currentMenu", "users");
        model.addAttribute("pageTitle", "사용자 관리");
        return "admin/users/list";
    }

    @GetMapping("/{userId}")
    public String userDetail(@PathVariable Long userId, Model model) {
        model.addAttribute("user", adminUserService.getUser(userId));
        model.addAttribute("suspensionHours", SUSPENSION_HOURS);
        model.addAttribute("currentMenu", "users");
        model.addAttribute("pageTitle", "사용자 상세");
        return "admin/users/detail";
    }

    @PostMapping("/{userId}/status")
    public String updateStatus(
            @PathVariable Long userId,
            @RequestParam UserStatus status,
            @RequestParam(required = false) Integer suspensionHours,
            RedirectAttributes redirectAttributes) {
        adminUserService.updateUserStatus(userId, new UpdateUserStatusRequest(status, suspensionHours));
        redirectAttributes.addFlashAttribute("message", "사용자 상태가 변경되었습니다.");
        return "redirect:/admin/users/" + userId;
    }
}
