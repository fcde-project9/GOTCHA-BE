package com.gotcha.domain.admin.dto;

public record DashboardStatsResponse(
        long totalUsers,
        long totalShops,
        long totalReviews,
        long totalPosts,
        long pendingReports,
        long activeUsers,
        long suspendedUsers,
        long bannedUsers
) {
}
