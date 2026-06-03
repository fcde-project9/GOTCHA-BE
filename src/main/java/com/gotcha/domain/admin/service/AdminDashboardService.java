package com.gotcha.domain.admin.service;

import com.gotcha.domain.admin.dto.DashboardStats;
import com.gotcha.domain.post.repository.PostRepository;
import com.gotcha.domain.report.entity.ReportStatus;
import com.gotcha.domain.report.repository.ReportRepository;
import com.gotcha.domain.review.repository.ReviewRepository;
import com.gotcha.domain.shop.repository.ShopRepository;
import com.gotcha.domain.user.entity.UserStatus;
import com.gotcha.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final ReviewRepository reviewRepository;
    private final PostRepository postRepository;
    private final ReportRepository reportRepository;

    public DashboardStats getStats() {
        long totalUsers = userRepository.findAllWithStatusFilter(null, PageRequest.of(0, 1)).getTotalElements();
        long activeUsers = userRepository.findAllWithStatusFilter(UserStatus.ACTIVE, PageRequest.of(0, 1)).getTotalElements();
        long suspendedUsers = userRepository.findAllWithStatusFilter(UserStatus.SUSPENDED, PageRequest.of(0, 1)).getTotalElements();
        long bannedUsers = userRepository.findAllWithStatusFilter(UserStatus.BANNED, PageRequest.of(0, 1)).getTotalElements();
        long totalShops = shopRepository.count();
        long totalReviews = reviewRepository.count();
        long totalPosts = postRepository.count();
        long pendingReports = reportRepository.countWithFilters(null, ReportStatus.PENDING);

        return new DashboardStats(
                totalUsers, totalShops, totalReviews, totalPosts,
                pendingReports, activeUsers, suspendedUsers, bannedUsers
        );
    }
}
