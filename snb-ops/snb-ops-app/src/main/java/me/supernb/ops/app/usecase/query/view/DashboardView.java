package me.supernb.ops.app.usecase.query.view;

import java.util.List;

/// 看板待办:30 天内扣款(按日升序)/退款该催了/封号未结案计数。
public record DashboardView(List<SubscriptionView> upcomingBilling, List<SubscriptionView> refundFollowUps,
                            long bannedOpenCount) {
}
