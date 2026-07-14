package me.supernb.activity.domain.model.checkin;

import java.time.Instant;

/// 我的补给发放记录一行(GET /checkin/rewards,已含展示标签——查询服务组装,
/// month 用 "yyyy-MM" 字符串,只包含 status=success 的历史行)。
public record CheckinRewardSummary(String month, String tier, String label, Instant grantedAt) {
}
