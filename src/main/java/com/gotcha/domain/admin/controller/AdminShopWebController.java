package com.gotcha.domain.admin.controller;

import com.gotcha.domain.admin.security.AdminUserDetails;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.repository.ShopRepository;
import com.gotcha.domain.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/shops")
@RequiredArgsConstructor
public class AdminShopWebController {

    private final ShopRepository shopRepository;
    private final ShopService shopService;

    @GetMapping
    public String listShops(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String searchKeyword = (keyword != null && keyword.trim().isEmpty()) ? null : keyword;
        Page<Shop> shopPage = shopRepository.findAllWithKeywordFilter(searchKeyword, pageable);
        model.addAttribute("shops", shopPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", page);
        model.addAttribute("currentMenu", "shops");
        model.addAttribute("pageTitle", "매장 관리");
        return "admin/shops/list";
    }

    @GetMapping("/{shopId}")
    public String shopDetail(@PathVariable Long shopId, Model model) {
        Shop shop = shopRepository.findByIdWithCreator(shopId)
                .orElseThrow(() -> new IllegalArgumentException("매장을 찾을 수 없습니다: " + shopId));
        model.addAttribute("shop", shop);
        model.addAttribute("currentMenu", "shops");
        model.addAttribute("pageTitle", "매장 상세");
        return "admin/shops/detail";
    }

    @PostMapping("/{shopId}/delete")
    public String deleteShop(
            @PathVariable Long shopId,
            @AuthenticationPrincipal AdminUserDetails adminDetails,
            RedirectAttributes redirectAttributes) {
        shopService.deleteShop(shopId, adminDetails.getUser());
        redirectAttributes.addFlashAttribute("message", "매장이 삭제되었습니다.");
        return "redirect:/admin/shops";
    }
}
